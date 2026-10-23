package com.barriogo.app.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.R
import com.barriogo.app.domain.Validaciones
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.BarrioLogo
import com.barriogo.app.ui.components.FigmaInput
import com.barriogo.app.ui.components.FigmaInputColors
import com.barriogo.app.ui.components.SubmitArrowButton
import com.barriogo.app.ui.theme.BarrioColors

/** RP1–RP3 · Olvidé mi contraseña: correo → código de 6 números → nueva contraseña. */
@Composable
fun RecuperarScreen(onVolver: () -> Unit, onListo: () -> Unit) {
    val vm = barrioViewModel { RecuperarViewModel(it.authRepo) }
    val s by vm.estado.collectAsState()
    val enFlujo = s.paso == PasoRecuperar.CODIGO || s.paso == PasoRecuperar.NUEVA
    BackHandler(enabled = enFlujo) { vm.volver() }

    Box(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .navigationBarsPadding()
            .imePadding()
            .testTag("recuperar_screen"),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 56.dp, bottom = 40.dp),
        ) {
            // Encabezado: logo + título centrado
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                BarrioLogo(iconWidth = 104.dp, iconHeight = 100.dp, fontSize = 35.sp, gap = 7.dp)
                Spacer(Modifier.height(28.dp))
                Text(
                    when (s.paso) {
                        PasoRecuperar.CORREO -> "Recuperar contraseña"
                        PasoRecuperar.CODIGO -> "Revisa tu correo"
                        PasoRecuperar.NUEVA -> "Crea una nueva contraseña"
                        PasoRecuperar.LISTO -> "¡Listo!"
                    },
                    color = FigmaInputColors.texto,
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                when (s.paso) {
                    PasoRecuperar.CORREO -> "Te enviaremos un código de 6 dígitos para que puedas crear una nueva clave."
                    PasoRecuperar.CODIGO -> "Te enviamos un código de 6 dígitos a ${s.correo.trim()}. Vence en 10 minutos. " +
                        "Si no lo ves, revisa la carpeta de spam."
                    PasoRecuperar.NUEVA -> "La usarás para ingresar a tu tienda."
                    PasoRecuperar.LISTO -> "Ya puedes ingresar con tu nueva contraseña."
                },
                color = FigmaInputColors.texto,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(40.dp))

            when (s.paso) {
                PasoRecuperar.CORREO -> {
                    Etiqueta("Correo electrónico", tenue = s.correo.isEmpty())
                    FigmaInput(
                        value = s.correo,
                        onValueChange = vm::onCorreo,
                        placeholder = "tu@correo.com",
                        leading = { Icon(painterResource(R.drawable.fig_correo), null, tint = FigmaInputColors.texto) },
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                        onDone = { vm.enviarCodigo() },
                        error = s.error,
                        modifier = Modifier.testTag("correo_input"),
                    )
                    Spacer(Modifier.height(16.dp))
                    SubmitArrowButton("Enviar código", loading = s.cargando, modifier = Modifier.testTag("enviar_codigo")) { vm.enviarCodigo() }
                }
                PasoRecuperar.CODIGO -> {
                    CampoCodigo(s.codigo, conError = s.error != null, onCambio = vm::onCodigo)
                    Spacer(Modifier.height(8.dp))
                    MensajeError(s.error)
                    Spacer(Modifier.height(16.dp))
                    SubmitArrowButton("Verificar", enabled = s.codigo.length == 6, loading = s.cargando) { vm.verificar() }
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val puedeReenviar = s.reenviarEn == 0 && !s.cargando
                        Enlace(
                            if (s.reenviarEn > 0) "Reenviar código en %d:%02d".format(s.reenviarEn / 60, s.reenviarEn % 60) else "Reenviar código",
                            activo = puedeReenviar,
                        ) { vm.enviarCodigo() }
                        Enlace("Cambiar correo", modifier = Modifier.testTag("cambiar_correo")) { vm.volver() }
                    }
                }
                PasoRecuperar.NUEVA -> {
                    var ver by remember { mutableStateOf(false) }
                    var verConfirmacion by remember { mutableStateOf(false) }
                    Etiqueta("Nueva contraseña", tenue = s.contrasena.isEmpty())
                    CampoClave(s.contrasena, ver, { ver = !ver }, vm::onContrasena, error = null)
                    Requisitos(s.contrasena, s.requisitosPendientes)
                    Spacer(Modifier.height(16.dp))
                    Etiqueta("Confirmar contraseña", tenue = s.confirmacion.isEmpty())
                    CampoClave(
                        s.confirmacion, verConfirmacion, { verConfirmacion = !verConfirmacion }, vm::onConfirmacion,
                        error = s.error ?: if (s.confirmacion.isNotEmpty() && s.confirmacion != s.contrasena) "Las contraseñas no coinciden" else null,
                    )
                    Spacer(Modifier.height(16.dp))
                    SubmitArrowButton("Guardar contraseña", loading = s.cargando) { vm.guardar() }
                }
                PasoRecuperar.LISTO -> {
                    SubmitArrowButton("Ingresar") { onListo() }
                }
            }
        }

        // Flecha de volver, amarilla, arriba a la izquierda
        Box(
            Modifier
                .padding(start = 12.dp, top = 44.dp)
                .size(48.dp)
                .clip(CircleShape)
                .clickable { if (enFlujo) vm.volver() else onVolver() }
                .testTag("recuperar_volver"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color(0xFFF2C849), modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun Etiqueta(texto: String, tenue: Boolean) {
    Text(
        texto,
        color = if (tenue) FigmaInputColors.tenue else FigmaInputColors.texto,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun Enlace(texto: String, modifier: Modifier = Modifier, activo: Boolean = true, onClick: () -> Unit) {
    Text(
        texto,
        color = if (activo) Color(0xFFF2C849) else FigmaInputColors.tenue,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.clickable(enabled = activo, onClick = onClick).padding(vertical = 8.dp),
    )
}

/** Campo de contraseña del Figma: candado a la izquierda y ojo para mostrarla. */
@Composable
private fun CampoClave(valor: String, visible: Boolean, onVer: () -> Unit, onCambio: (String) -> Unit, error: String?) {
    val texto = FigmaInputColors.texto
    FigmaInput(
        value = valor,
        onValueChange = onCambio,
        placeholder = "••••••••",
        leading = { Icon(painterResource(R.drawable.fig_candado), null, tint = texto, modifier = Modifier.padding(start = 2.dp)) },
        trailing = {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onVer),
                contentAlignment = Alignment.Center,
            ) {
                val descripcion = if (visible) "Ocultar contraseña" else "Mostrar contraseña"
                if (visible) Icon(Icons.Outlined.Visibility, descripcion, tint = texto, modifier = Modifier.size(22.dp))
                else Icon(painterResource(R.drawable.fig_ojo_tachado), descripcion, tint = texto)
            }
        },
        keyboardType = KeyboardType.Password,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        error = error,
    )
}

/** Seis casillas ("Casilla OTP" del Figma: 48 × 56) dibujadas sobre un solo campo de texto. */
@Composable
private fun CampoCodigo(codigo: String, conError: Boolean, onCambio: (String) -> Unit) {
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { foco.requestFocus() }
    val alerta = FigmaInputColors.alerta
    val activo = Color(0xFFF2C849)
    val normal = FigmaInputColors.borde
    val fondo = FigmaInputColors.fondo
    val texto = FigmaInputColors.texto
    BasicTextField(
        value = codigo,
        onValueChange = onCambio,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth().focusRequester(foco).testTag("codigo_input"),
        decorationBox = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                repeat(6) { i ->
                    val borde = when {
                        conError -> alerta
                        i == codigo.length -> activo
                        else -> normal
                    }
                    Box(
                        Modifier.size(width = 48.dp, height = 56.dp)
                            .background(fondo, RoundedCornerShape(12.dp))
                            .border(if (i == codigo.length || conError) 2.dp else 1.dp, borde, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(codigo.getOrNull(i)?.toString() ?: "", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = texto)
                    }
                }
            }
        },
    )
}

@Composable
private fun Requisitos(contrasena: String, pendientes: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Validaciones.REQUISITOS_CONTRASENA.forEach { r ->
            val cumple = contrasena.isNotEmpty() && r !in pendientes
            Text(
                (if (cumple) "✓ " else "• ") + r,
                color = if (cumple) Color(0xFFF2C849) else FigmaInputColors.tenue,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun MensajeError(error: String?) {
    if (error != null) Text(error, color = FigmaInputColors.alerta, fontSize = 14.sp, modifier = Modifier.testTag("recuperar_error"))
}
