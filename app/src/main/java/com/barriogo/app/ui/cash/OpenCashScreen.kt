package com.barriogo.app.ui.cash

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.domain.EstadoCaja
import com.barriogo.app.domain.MAX_OPENING_CHANGES
import com.barriogo.app.domain.Money
import com.barriogo.app.domain.ReglasCaja
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.AvisoDialogo
import com.barriogo.app.ui.components.BotonSecundarioFigma
import com.barriogo.app.ui.components.CabeceraFigma
import com.barriogo.app.ui.components.DialogoFigma
import com.barriogo.app.ui.components.LoadingOverlay
import com.barriogo.app.ui.components.NotaFigma
import com.barriogo.app.ui.components.TecladoMonto
import com.barriogo.app.ui.components.montoConDigito
import com.barriogo.app.ui.components.montoSinDigito
import com.barriogo.app.ui.theme.BarrioColors
import com.barriogo.app.ui.theme.FigmaColors

/**
 * K0 · Abrir caja del día y K1 · Apertura de caja (guardada, corregir, sin más cambios).
 * - Sin caja hoy: se escribe el dinero con el que se empieza (mayor a $ 0) con el teclado del Figma.
 * - Con caja abierta: se ve la apertura y se puede corregir hasta 3 veces; cada cambio pide confirmación.
 * - Caja de un día anterior abierta: primero hay que cerrarla.
 */
@Composable
fun OpenCashScreen(
    onBack: () -> Unit,
    onGoCash: () -> Unit,
    onGoClose: () -> Unit,
    /** true = entra directo a corregir (lápiz de Inicio o Caja). */
    corregir: Boolean = false,
) {
    val vm = barrioViewModel { CashViewModel(it) }
    val s by vm.state.collectAsState()
    val ocupado by vm.ocupado.collectAsState()
    val error by vm.error.collectAsState()

    var editando by rememberSaveable { mutableStateOf(corregir) }
    var monto by rememberSaveable { mutableLongStateOf(0L) }
    var tocado by rememberSaveable { mutableStateOf(false) }
    var confirmar by rememberSaveable { mutableStateOf(false) }
    var limite by rememberSaveable { mutableStateOf(false) }

    // Los avisos de cierre son notificaciones: en Android 13+ hay que pedir permiso.
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun pedirPermisoAvisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permiso.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // Al entrar a corregir, el campo arranca con la apertura actual.
    LaunchedEffect(editando, s.session?.id) {
        if (editando) s.session?.let { if (monto == 0L) monto = it.openingAmount }
    }
    // Sin caja abierta o sin cambios disponibles no se corrige: se muestra el estado guardado (bloqueado).
    if (editando && s.cargado && (s.estado != EstadoCaja.ABIERTA || s.editsLeft == 0)) editando = false

    Box(Modifier.fillMaxSize().background(BarrioColors.Background).statusBarsPadding().testTag("open_cash_screen")) {
        Column(Modifier.fillMaxSize()) {
            CabeceraFigma("Apertura de caja", onBack = onBack)
            when {
                !s.cargado -> Unit
                s.estado == EstadoCaja.ABIERTA_DIA_ANTERIOR -> CajaPendiente(
                    titulo = "La caja de un día anterior sigue abierta",
                    texto = "Ciérrala con el conteo del efectivo antes de abrir la caja de hoy.",
                    boton = "Cerrar esa caja",
                    onBoton = onGoClose,
                    onCancelar = onBack,
                )
                s.estado == EstadoCaja.CERRADA_HOY -> CajaPendiente(
                    titulo = "La caja de hoy ya se cerró",
                    texto = "Mañana podrás abrir una nueva caja. Si el conteo quedó mal, corrige el cierre desde Caja.",
                    boton = "Ir a Caja",
                    onBoton = onGoCash,
                    onCancelar = onBack,
                )
                s.estado == EstadoCaja.SIN_ABRIR || editando -> {
                    val actual = s.session?.openingAmount
                    val problema = ReglasCaja.problemaApertura(monto, actual = if (editando) actual else null)
                    val enCero = tocado && monto == 0L
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight)
                            .padding(horizontal = 24.dp).padding(top = 10.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            if (editando) "Corregir la apertura" else "Abre la caja del día",
                            color = FigmaColors.Crema, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        )
                        Text(
                            if (editando) "Cambio ${(s.session?.editsUsed ?: 0) + 1} de $MAX_OPENING_CHANGES."
                            else "Es el dinero que tienes antes de la primera venta.",
                            color = FigmaColors.Tenue, fontSize = 14.sp,
                        )
                        CampoApertura(monto = monto, error = if (tocado) problema else null)
                        if (editando) ContadorCambios(s.editsLeft)
                        NotaFigma("La apertura es la base del día: todo lo que entre y salga se cuenta a partir de aquí.")
                      }
                      // Las acciones van al fondo, empujadas por el espacio libre (como el "Espaciador" del Figma).
                      Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BotonSecundarioFigma("Cancelar", Modifier.weight(1f).testTag("cancel_opening")) {
                                if (editando && !corregir) {
                                    editando = false
                                    monto = 0L
                                    tocado = false
                                } else onBack()
                            }
                            BotonApertura("Guardar apertura", habilitado = problema == null, modifier = Modifier.weight(1f)) {
                                tocado = true
                                if (problema == null) {
                                    if (editando) confirmar = true
                                    else vm.abrir(monto) { pedirPermisoAvisos() }
                                }
                            }
                        }
                        Text(
                            when {
                                enCero -> "Si aún no tienes base, escribe el valor real con el que abres."
                                tocado && problema != null -> problema
                                editando -> "Solo puedes cambiar la apertura $MAX_OPENING_CHANGES veces al día."
                                monto > 0 && problema == null -> "Todo listo, ya puedes guardar"
                                else -> "Escribe cuánto dinero tienes para empezar."
                            },
                            color = if (tocado && problema != null && !enCero) FigmaColors.Naranja else FigmaColors.Dorado,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("opening_status"),
                        )
                      }
                    }
                    }
                    TecladoMonto(
                        listoHabilitado = problema == null,
                        onDigito = { d -> monto = montoConDigito(monto, d); tocado = true },
                        onBorrar = { monto = montoSinDigito(monto); tocado = true },
                        onListo = {
                            tocado = true
                            if (problema == null) {
                                if (editando) confirmar = true else vm.abrir(monto) { pedirPermisoAvisos() }
                            }
                        },
                        modifier = Modifier.navigationBarsPadding(),
                    )
                }
                else -> AperturaGuardada(
                    monto = s.session?.openingAmount ?: 0L,
                    restantes = s.editsLeft,
                    onIrACaja = onGoCash,
                    onCorregir = {
                        monto = s.session?.openingAmount ?: 0L
                        tocado = false
                        editando = true
                    },
                    onLimite = { limite = true },
                )
            }
        }

        // K1 · "Confirmar": revisa antes de guardar; el tercer cambio avisa que es el último (naranja).
        if (confirmar) {
            val numero = (s.session?.editsUsed ?: 0) + 1
            val ultimo = numero == MAX_OPENING_CHANGES
            val quedan = MAX_OPENING_CHANGES - numero
            DialogoFigma(
                icono = if (ultimo) Icons.Outlined.WarningAmber else Icons.Outlined.Info,
                titulo = if (ultimo) "Este es tu último cambio" else "Revisa antes de guardar",
                mensaje = if (ultimo) "La apertura quedará en ${Money.cop(monto)} y no podrás cambiarla otra vez hoy."
                else "La apertura pasará a ${Money.cop(monto)}. Te ${if (quedan == 1) "quedará 1 cambio" else "quedarán $quedan cambios"} hoy.",
                principal = if (ultimo) "Sí, es el último" else "Sí, guardar",
                onPrincipal = {
                    confirmar = false
                    vm.corregirApertura(monto) {
                        editando = false
                        tocado = false
                        if (corregir) onBack()
                    }
                },
                secundario = "Revisar de nuevo",
                onSecundario = { confirmar = false },
                acentoNaranja = ultimo,
            )
        }
        // K1 · "Límite": tocar "Corregir apertura" sin cambios disponibles.
        if (limite) {
            DialogoFigma(
                icono = Icons.Filled.Lock,
                titulo = "Llegaste al límite de hoy",
                mensaje = "Ya usaste los $MAX_OPENING_CHANGES cambios de la apertura. Podrás volver a cambiarla mañana.",
                principal = "Entendido",
                onPrincipal = { limite = false },
                acentoNaranja = true,
            )
        }
        error?.let { AvisoDialogo(titulo = "No se pudo guardar", mensaje = it, principal = "Entendido", onPrincipal = vm::cerrarError) }
        ocupado?.let { LoadingOverlay(it) }
    }
}

/** "Entrada apertura": #22593F, borde amarillo de 2, esquinas 14 y el monto en Bold 28. */
@Composable
private fun CampoApertura(monto: Long, error: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Dinero con el que empiezas", color = FigmaColors.Crema, fontSize = 14.sp)
        val forma = RoundedCornerShape(14.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(forma)
                .background(FigmaColors.Tarjeta)
                .border(2.dp, if (error != null) FigmaColors.Naranja else FigmaColors.AmarilloBoton, forma)
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .testTag("opening_amount"),
        ) {
            Text(
                if (monto == 0L) "$ 0" else Money.cop(monto),
                color = when {
                    monto == 0L && error != null -> FigmaColors.Naranja
                    monto == 0L -> FigmaColors.Crema.copy(alpha = 0.45f)
                    else -> FigmaColors.Crema
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (error != null) Text(error, color = FigmaColors.Naranja, fontSize = 13.sp, modifier = Modifier.testTag("opening_error"))
    }
}

/**
 * "Contador" de cambios de la apertura: amarillo con 2 o 3, naranja #F2B544 con 1 y con 0 (candado).
 * [compacto] = la píldora pequeña de H1 / C1 ("Sin más cambios hoy").
 */
@Composable
internal fun ContadorCambios(restantes: Int, compacto: Boolean = false) {
    val naranja = restantes <= 1
    val color = if (naranja) FigmaColors.Naranja else FigmaColors.Amarillo
    Row(
        Modifier
            .clip(RoundedCornerShape(if (compacto) 8.dp else 10.dp))
            .background((if (naranja) Color(0xFFF2B544) else Color(0xFFF2C849)).copy(alpha = if (compacto) 0.16f else 0.14f))
            .padding(horizontal = if (compacto) 9.dp else 12.dp, vertical = if (compacto) 5.dp else 8.dp)
            .testTag("contador_cambios"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compacto) 6.dp else 8.dp),
    ) {
        val icono = if (compacto) 13.dp else 16.dp
        if (restantes == 0) Icon(Icons.Filled.Lock, null, tint = color, modifier = Modifier.size(icono))
        else Box(Modifier.size(icono).clip(CircleShape).background(color))
        Text(
            when {
                restantes == 0 && compacto -> "Sin más cambios hoy"
                restantes == 0 -> "Ya usaste los $MAX_OPENING_CHANGES cambios de hoy"
                restantes == 1 -> "Te queda 1 cambio hoy"
                else -> "Te quedan $restantes cambios hoy"
            },
            color = color, fontSize = if (compacto) 12.sp else 13.sp, fontWeight = FontWeight.SemiBold,
        )
    }
}

/** "Botón principal · Crear": amarillo con esquinas 14; deshabilitado, amarillo al 30 % con texto apagado. */
@Composable
private fun BotonApertura(texto: String, habilitado: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (habilitado) FigmaColors.AmarilloBoton else FigmaColors.AmarilloBoton.copy(alpha = 0.3f))
            .clickable(onClick = onClick)
            .testTag("save_opening"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            texto,
            color = if (habilitado) FigmaColors.SobreAmarillo else FigmaColors.SobreAmarillo.copy(alpha = 0.5f),
            fontSize = 15.sp, fontWeight = FontWeight.Bold,
        )
    }
}

/** K1 · "Caja abierta": tarjeta verde con la apertura, contador y acciones. */
@Composable
private fun AperturaGuardada(monto: Long, restantes: Int, onIrACaja: () -> Unit, onCorregir: () -> Unit, onLimite: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 10.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Caja abierta", color = FigmaColors.Crema, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("Así queda la base de hoy.", color = FigmaColors.Tenue, fontSize = 14.sp)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF6FCF97).copy(alpha = 0.12f))
                .border(1.dp, FigmaColors.Verde, RoundedCornerShape(18.dp))
                .padding(horizontal = 20.dp, vertical = 22.dp)
                .testTag("opening_saved_card"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Outlined.CheckCircle, null, tint = FigmaColors.Verde, modifier = Modifier.size(42.dp))
            Text("Apertura del día: ${Money.cop(monto)}", color = FigmaColors.Crema, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("Ya puedes empezar a vender.", color = FigmaColors.Tenue, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        ContadorCambios(restantes)
        NotaFigma("La apertura es la base del día: todo lo que entre y salga se cuenta a partir de aquí.")
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BotonSecundarioFigma("Ir a Caja", Modifier.weight(1f).testTag("go_cash"), onClick = onIrACaja)
            BotonApertura("Corregir apertura", habilitado = restantes > 0, modifier = Modifier.weight(1f)) {
                if (restantes > 0) onCorregir() else onLimite()
            }
        }
        Text(
            when (restantes) {
                0 -> "Podrás volver a cambiarla mañana."
                1 -> "Después de este cambio no podrás corregir la apertura hoy."
                else -> "Solo puedes cambiar la apertura $MAX_OPENING_CHANGES veces al día."
            },
            color = FigmaColors.Dorado, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Caja de un día anterior abierta, o caja de hoy ya cerrada: no se puede abrir. */
@Composable
private fun CajaPendiente(titulo: String, texto: String, boton: String, onBoton: () -> Unit, onCancelar: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 10.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(titulo, color = FigmaColors.Crema, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(texto, color = FigmaColors.Tenue, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BotonSecundarioFigma("Volver", Modifier.weight(1f), onClick = onCancelar)
            BotonApertura(boton, habilitado = true, modifier = Modifier.weight(1f), onClick = onBoton)
        }
    }
}
