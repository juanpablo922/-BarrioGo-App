package com.barriogo.app.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barriogo.app.AppContainer
import com.barriogo.app.data.MovementEntity
import com.barriogo.app.data.ProfileEntity
import com.barriogo.app.data.auth.Resultado
import com.barriogo.app.domain.Dates
import com.barriogo.app.domain.ExpenseKind
import com.barriogo.app.domain.GastoEditable
import com.barriogo.app.domain.PeriodoGastos
import com.barriogo.app.domain.ReglasGasto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GastosUiState(
    val cargado: Boolean = false,
    val periodo: PeriodoGastos = PeriodoGastos.HOY,
    /** Gastos del periodo, del más nuevo al más viejo. Los anulados se ven tachados. */
    val lista: List<MovementEntity> = emptyList(),
    /** Id (Room) de la caja abierta: solo sus gastos se editan o anulan. */
    val cajaAbiertaId: Long? = null,
) {
    val activos: List<MovementEntity> get() = lista.filter { !it.voided }
    val total: Long get() = activos.sumOf { it.amount }
    fun editable(g: MovementEntity) = !g.voided && g.sessionId != null && g.sessionId == cajaAbiertaId
}

/** G1 · Gastos y G2 · Registrar / Editar gasto. */
class GastosViewModel(private val container: AppContainer) : ViewModel() {
    private val gastos = container.gastos
    private val periodo = MutableStateFlow(PeriodoGastos.HOY)

    val state: StateFlow<GastosUiState> = combine(gastos.gastos, container.caja.abierta, periodo) { todos, caja, p ->
        val desde = ReglasGasto.desde(p, LocalDate.now())
        GastosUiState(
            cargado = true,
            periodo = p,
            lista = todos.filter { !Dates.localDate(it.createdAt).isBefore(desde) }.sortedByDescending { it.createdAt },
            cajaAbiertaId = caja?.id,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GastosUiState())

    private val _ocupado = MutableStateFlow<String?>(null)
    val ocupado: StateFlow<String?> = _ocupado

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    fun cerrarError() { _error.value = null }

    /** Gasto que se está editando en G2 (null mientras carga o si es nuevo). */
    private val _original = MutableStateFlow<MovementEntity?>(null)
    val original: StateFlow<MovementEntity?> = _original

    fun elegirPeriodo(p: PeriodoGastos) { periodo.value = p }

    fun cargar(id: Long) {
        viewModelScope.launch { _original.value = gastos.gasto(id) }
    }

    fun editable(g: MovementEntity): GastoEditable = GastoEditable(ReglasGasto.tipoDe(g.category, g.detail), g.amount, g.title)

    fun registrar(tipo: ExpenseKind?, monto: Long, detalle: String, onOk: () -> Unit) =
        lanzar("Guardando el gasto…", onOk) { p -> gastos.registrar(tipo, monto, detalle, p) }

    fun editar(id: Long, tipo: ExpenseKind?, monto: Long, detalle: String, onOk: () -> Unit) =
        lanzar("Guardando los cambios…", onOk) { p -> gastos.editar(id, tipo, monto, detalle, p) }

    fun anular(id: Long, onOk: () -> Unit) = lanzar("Anulando el gasto…", onOk) { p -> gastos.anular(id, p) }

    private fun lanzar(mensaje: String, onOk: () -> Unit, accion: suspend (ProfileEntity) -> Resultado<*>) {
        if (_ocupado.value != null) return
        viewModelScope.launch {
            val p = container.perfilActivo() ?: return@launch run { _error.value = "Vuelve a entrar con tu clave para continuar." }
            _ocupado.value = mensaje
            val inicio = System.currentTimeMillis()
            val r = try {
                accion(p)
            } finally {
                delay((500 - (System.currentTimeMillis() - inicio)).coerceAtLeast(0))
                _ocupado.value = null
            }
            when (r) {
                is Resultado.Ok -> onOk()
                is Resultado.Error -> _error.value = r.mensaje
            }
        }
    }
}
