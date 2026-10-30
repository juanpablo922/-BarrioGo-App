package com.barriogo.app.data.fiados

import com.barriogo.app.data.MovementEntity
import com.barriogo.app.data.ProfileEntity
import com.barriogo.app.data.SessionStore
import com.barriogo.app.data.auth.Resultado
import com.barriogo.app.data.caja.CajaRepository
import com.barriogo.app.data.movimientos.MovimientosRemotos
import com.barriogo.app.domain.FiadoPendiente
import com.barriogo.app.domain.Fiados
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.PayMethod
import com.barriogo.app.domain.kind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn

/**
 * Fiados en Firestore: clientes (tiendas/{t}/clientes) y sus fiados y abonos
 * (tiendas/{t}/movimientos con tipo "fiado" o "abono"). Se leen y escriben solo allí, en tiempo
 * real y con la caché de Firestore cuando no hay internet. Nada vive en Room.
 *
 * - Crear: cliente (nace con cupo de $ 50.000), fiado (sin pasar el cupo) y abono (sin pasar la deuda).
 * - Leer: [clientes] y [movimientos]; los saldos se calculan en la app (Fiados.cuentas) y el
 *   servidor los confirma en el cliente y en cada fiado (saldoPorPagar, estadoPago).
 * - Actualizar: datos del cliente, cupo (solo administrador), valor y "qué se llevó" del fiado,
 *   valor y método del abono.
 * - Eliminar: no se borra nada. Pagar deja el fiado en abonado o pagado; uno registrado por error
 *   se anula y queda tachado.
 *
 * Caja: todo pide una caja abierta. Un fiado de otro día se edita o anula con la caja abierta de
 * hoy (no mueve la caja); un abono, solo mientras su caja siga abierta (firestore.rules).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FiadosRepository(
    private val store: SessionStore,
    private val cajas: CajaRepository,
    alcance: CoroutineScope,
    private val remotos: MovimientosRemotos = MovimientosRemotos(),
    private val clientesRemotos: ClientesRemotos = ClientesRemotos(),
) {
    /** Clientes de la tienda activa, por nombre. */
    val clientes: Flow<List<Cliente>> = store.cuenta
        .flatMapLatest { tienda -> if (tienda.isBlank()) flowOf(emptyList()) else clientesRemotos.clientes(tienda) }
        .shareIn(alcance, SharingStarted.WhileSubscribed(5_000), replay = 1)

    /** Fiados y abonos de la tienda activa, del más nuevo al más viejo (también los anulados). */
    val movimientos: Flow<List<MovementEntity>> = store.cuenta
        .flatMapLatest { tienda -> if (tienda.isBlank()) flowOf(emptyList()) else remotos.fiados(tienda) }
        .shareIn(alcance, SharingStarted.WhileSubscribed(5_000), replay = 1)

    suspend fun cliente(id: Long): Cliente? = clientes.first().firstOrNull { it.id == id }

    /** Estado de cuenta del cliente con lo que hay ahora en la caché. */
    suspend fun cuenta(id: Long): FiadoPendiente? {
        val c = cliente(id) ?: return null
        return Fiados.cuentas(listOf(c), movimientos.first()).firstOrNull()
    }

    // ── Clientes ───────────────────────────────────────────────────────────────────────────────

    /** "+ Agregar cliente". Si ya hay uno con ese celular, se usa ese en vez de duplicarlo. */
    suspend fun crearCliente(nombre: String, celular: String, direccion: String): Resultado<Cliente> {
        Fiados.problemasCliente(nombre, celular, direccion).values.firstOrNull()?.let { return Resultado.Error(it, "invalid-argument") }
        clientes.first().firstOrNull { it.phone == celular }?.let { return Resultado.Ok(it) }
        val tienda = tienda() ?: return SIN_NUBE
        val remoteId = clientesRemotos.nuevoId(tienda)
        val nuevo = Cliente(
            id = Cliente.idLocal(remoteId), remoteId = remoteId,
            name = nombre.trim(), phone = celular, address = direccion.trim(),
        )
        clientesRemotos.crear(tienda, nuevo)
        return Resultado.Ok(nuevo)
    }

    suspend fun editarCliente(id: Long, nombre: String, celular: String, direccion: String): Resultado<Cliente> {
        val c = cliente(id) ?: return NO_CLIENTE
        Fiados.problemasCliente(nombre, celular, direccion).values.firstOrNull()?.let { return Resultado.Error(it, "invalid-argument") }
        val editado = c.copy(name = nombre.trim(), phone = celular, address = direccion.trim())
        if (editado == c) return Resultado.Error("Todavía no has cambiado nada", "invalid-argument")
        val tienda = tienda() ?: return SIN_NUBE
        clientesRemotos.editar(tienda, editado)
        return Resultado.Ok(editado)
    }

    /** Subir el cupo del cliente: solo un administrador. */
    suspend fun cambiarCupo(id: Long, cupo: Long, perfil: ProfileEntity): Resultado<Cliente> {
        if (!perfil.isAdmin) return Resultado.Error("Solo un administrador puede cambiar el cupo de fiado.", "permission-denied")
        val c = cliente(id) ?: return NO_CLIENTE
        Fiados.problemaNuevoCupo(c.creditLimit, cupo)?.let { return Resultado.Error(it.ifEmpty { "Todavía no has cambiado nada" }, "invalid-argument") }
        val nube = enLaNube(perfil) ?: return SIN_NUBE
        clientesRemotos.cambiarCupo(nube.first, c.remoteId, cupo, nube.second)
        return Resultado.Ok(c.copy(creditLimit = cupo))
    }

    // ── Fiados y abonos ────────────────────────────────────────────────────────────────────────

    /** Nuevo fiado: "¿Qué se llevó?" obligatorio y sin pasar el cupo del cliente. */
    suspend fun registrarFiado(clienteId: Long, queSeLlevo: String, monto: Long, perfil: ProfileEntity, origenVoz: Boolean = false): Resultado<MovementEntity> {
        if (monto <= 0) return Resultado.Error("Escribe un monto mayor a cero", "invalid-argument")
        if (queSeLlevo.isBlank()) return Resultado.Error("Escribe qué se llevó", "invalid-argument")
        val cuenta = cuenta(clienteId) ?: return NO_CLIENTE
        Fiados.problemaCupo(cuenta.cliente.name, cuenta.saldo, monto, cuenta.cliente.creditLimit)?.let { return Resultado.Error(it, CUPO) }
        return crear(
            MovementEntity(
                type = MovementType.FIADO.name, amount = monto, title = "Fiado", detail = queSeLlevo.trim(),
                payMethod = PayMethod.EFECTIVO.name, clientId = clienteId, profileId = perfil.id, profileName = perfil.name,
                createdAt = System.currentTimeMillis(), extra = if (origenVoz) "voz" else "",
            ),
            cuenta.cliente, perfil,
        )
    }

    /**
     * Abono (Registrar pago o "Sí, me pagó"): el efectivo entra a la caja; la transferencia solo baja
     * la deuda. Con [fiadoRemoteId] el abono va a ese solo fiado y no puede pasar de lo que ese fiado debe.
     */
    suspend fun registrarAbono(clienteId: Long, monto: Long, metodo: PayMethod, perfil: ProfileEntity, fiadoRemoteId: String? = null): Resultado<MovementEntity> {
        val cuenta = cuenta(clienteId) ?: return NO_CLIENTE
        val sinMonto = "Escribe un monto mayor a cero"
        if (fiadoRemoteId == null) {
            Fiados.problemaAbono(monto, cuenta.saldo)?.let { return Resultado.Error(it.ifEmpty { sinMonto }, "invalid-argument") }
        } else {
            val todos = movimientos.first()
            val fiado = todos.firstOrNull { it.remoteId == fiadoRemoteId && it.kind == MovementType.FIADO && it.clientId == clienteId && !it.voided }
                ?: return Resultado.Error("No encontramos ese fiado.", "not-found")
            val porPagar = Fiados.pagosPorFiado(clienteId, todos)[fiado.id]?.porPagar ?: 0L
            Fiados.problemaAbonoAFiado(monto, porPagar)?.let { return Resultado.Error(it.ifEmpty { sinMonto }, "invalid-argument") }
        }
        return crear(
            MovementEntity(
                type = MovementType.ABONO.name, amount = monto, title = "Abono", detail = "Abono a fiado",
                payMethod = metodo.name, clientId = clienteId, profileId = perfil.id, profileName = perfil.name,
                createdAt = System.currentTimeMillis(), fiadoId = fiadoRemoteId,
            ),
            cuenta.cliente, perfil,
        )
    }

    /** Editar fiado: valor y "¿Qué se llevó?". Sirve aunque el fiado sea de otro día. */
    suspend fun editarFiado(remoteId: String, queSeLlevo: String, monto: Long, perfil: ProfileEntity): Resultado<MovementEntity> {
        val f = cambiable(remoteId, MovementType.FIADO).let { if (it is Resultado.Error) return it else (it as Resultado.Ok).valor }
        if (queSeLlevo.isBlank()) return Resultado.Error("Escribe qué se llevó", "invalid-argument")
        val c = cliente(f.clientId ?: 0) ?: return NO_CLIENTE
        val balance = Fiados.balance(c.id, movimientos.first())
        Fiados.problemaEditarFiado(c.name, f.amount, monto, balance, c.creditLimit)?.let { problema ->
            val pasaCupo = monto > f.amount && balance - f.amount + monto > c.creditLimit
            return Resultado.Error(problema, if (pasaCupo) CUPO else "invalid-argument")
        }
        if (monto == f.amount && queSeLlevo.trim() == f.detail) return Resultado.Error("Todavía no has cambiado nada", "invalid-argument")
        val nube = enLaNube(perfil) ?: return SIN_NUBE
        val editado = f.copy(amount = monto, detail = queSeLlevo.trim())
        remotos.editar(nube.first, editado, nube.second)
        return Resultado.Ok(editado)
    }

    /** Editar abono: valor y método de pago. Solo mientras su caja siga abierta. */
    suspend fun editarAbono(remoteId: String, monto: Long, metodo: PayMethod, perfil: ProfileEntity): Resultado<MovementEntity> {
        val a = cambiable(remoteId, MovementType.ABONO).let { if (it is Resultado.Error) return it else (it as Resultado.Ok).valor }
        Fiados.problemaEditarAbono(a.amount, monto, Fiados.balance(a.clientId ?: 0, movimientos.first()))?.let { return Resultado.Error(it, "invalid-argument") }
        if (monto == a.amount && metodo.name == a.payMethod) return Resultado.Error("Todavía no has cambiado nada", "invalid-argument")
        val nube = enLaNube(perfil) ?: return SIN_NUBE
        val editado = a.copy(amount = monto, payMethod = metodo.name)
        remotos.editar(nube.first, editado, nube.second)
        return Resultado.Ok(editado)
    }

    /** Anular un fiado o abono registrado por error: no se borra, queda tachado y deja de contar. */
    suspend fun anular(remoteId: String, perfil: ProfileEntity): Resultado<Unit> {
        val m = cambiable(remoteId, null).let { if (it is Resultado.Error) return it else (it as Resultado.Ok).valor }
        if (m.kind == MovementType.FIADO) {
            Fiados.problemaAnularFiado(m.amount, Fiados.balance(m.clientId ?: 0, movimientos.first()))
                ?.let { return Resultado.Error(it, "failed-precondition") }
        }
        val nube = enLaNube(perfil) ?: return SIN_NUBE
        remotos.anular(nube.first, remoteId, nube.second)
        return Resultado.Ok(Unit)
    }

    private suspend fun crear(m: MovementEntity, cliente: Cliente, perfil: ProfileEntity): Resultado<MovementEntity> {
        val caja = cajas.cajaAbiertaUnaVez() ?: return SIN_CAJA
        val nube = enLaNube(perfil) ?: return SIN_NUBE
        val remoteId = remotos.nuevoId(nube.first)
        val nuevo = m.copy(id = MovimientosRemotos.idLocal(remoteId), remoteId = remoteId, cuenta = nube.first, cajaId = caja.id)
        remotos.crear(nube.first, remoteId, nuevo, caja.id, nube.second, cliente)
        return Resultado.Ok(nuevo)
    }

    /**
     * El movimiento existe, no está anulado y su caja permite cambiarlo: un fiado, con cualquier
     * caja abierta; un abono, solo si es de la caja abierta (firestore.rules → cajaPermiteCambio).
     */
    private suspend fun cambiable(remoteId: String, tipo: MovementType?): Resultado<MovementEntity> {
        val m = movimientos.first().firstOrNull { it.remoteId == remoteId && (tipo == null || it.kind == tipo) }
            ?: return Resultado.Error("No encontramos ese movimiento.", "not-found")
        if (m.voided) return Resultado.Error("Ya está anulado.", "failed-precondition")
        val caja = cajas.cajaAbiertaUnaVez() ?: return SIN_CAJA
        if (m.kind == MovementType.ABONO && m.cajaId != caja.id) return Resultado.Error(ABONO_CAJA_CERRADA, "failed-precondition")
        return Resultado.Ok(m)
    }

    private fun tienda(): String? = store.tiendaId?.takeIf { it.isNotBlank() }

    private fun enLaNube(perfil: ProfileEntity): Pair<String, String>? {
        val tienda = tienda() ?: return null
        val remoto = perfil.remoteId ?: return null
        return tienda to remoto
    }

    companion object {
        /** Código de error cuando el fiado pasaría el cupo: las pantallas muestran el aviso de límite. */
        const val CUPO = "cupo"
        const val ABONO_CAJA_CERRADA = "Este abono es de una caja cerrada: ya no se puede cambiar."
        private val NO_CLIENTE = Resultado.Error("No encontramos ese cliente.", "not-found")
        private val SIN_CAJA = Resultado.Error("Primero abre la caja del día.", "failed-precondition")
        private val SIN_NUBE = Resultado.Error(
            "Tu perfil todavía no está listo en la nube. Revisa tu internet y vuelve a entrar con tu clave.", "failed-precondition",
        )
    }
}
