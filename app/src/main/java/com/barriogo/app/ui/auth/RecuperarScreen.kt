package com.barriogo.app.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.domain.Validaciones
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.BarrioTopBar
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.theme.BarrioColors

/** RP1–RP3 · Olvidé mi contraseña: correo → código de 6 números → nueva contraseña. */
@Composable
fun RecuperarScreen(onVolver: () -> Unit, onListo: () -> Unit) {
    val vm = barrioViewModel { RecuperarViewModel(it.authRepo) }
    val s by vm.estado.collectAsState()
    val enFlujo = s.paso == PasoRecuperar.CODIGO || s.paso == PasoRecuperar.NUEVA
    BackHandler(enabled = enFlujo) { vm.volver() }

    Column(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .systemBarsPadding()
            .imePadding()
            .testTag("recuperar_screen"),
    ) {
        BarrioTopBar(title = "Recuperar contraseña", onBack = { if (enFlujo) vm.volver() else onVolver() })
        Column(
            Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (s.paso) {
                PasoRecuperar.CORREO -> {
                    Titulo("¿Olvidaste tu contraseña?", "Escribe el correo de tu cuenta y te enviaremos un código de 6 números.")
                    CampoTexto("Correo electrónico", s.correo, KeyboardType.Email, onCambio = vm::onCorreo)
                    MensajeError(s.error)
                    PrimaryButton("Enviar código", loading = s.cargando) { vm.enviarCodigo() }
                }
                PasoRecuperar.CODIGO -> {
                    Titulo(
                        "Revisa tu correo",
                        "Te enviamos un código de 6 números a ${s.correo.trim()}. Vence en 10 minutos. " +
                            "Si no lo ves, revisa la carpeta de spam.",
                    )
                    TextButton(onClick = vm::volver, modifier = Modifier.testTag("cambiar_correo")) {
                        Text("Cambiar correo", color = BarrioColors.Yellow)
                    }
                    CampoCodigo(s.codigo, conError = s.error != null, onCambio = vm::onCodigo)
                    MensajeError(s.error)
                    TextButton(onClick = vm::enviarCodigo, enabled = s.reenviarEn == 0 && !s.cargando) {
                        Text(
                            if (s.reenviarEn > 0) "Reenviar código en %d:%02d".format(s.reenviarEn / 60, s.reenviarEn % 60) else "Reenviar código",
                            color = if (s.reenviarEn > 0) BarrioColors.TextHint else BarrioColors.Yellow,
                        )
                    }
                    PrimaryButton("Verificar", enabled = s.codigo.length == 6, loading = s.cargando) { vm.verificar() }
                }
                PasoRecuperar.NUEVA -> {
                    Titulo("Crea una nueva contraseña", "La usarás para ingresar a tu tienda.")
                    CampoTexto("Nueva contraseña", s.contrasena, KeyboardType.Password, esClave = true, onCambio = vm::onContrasena)
                    Requisitos(s.contrasena, s.requisitosPendientes)
                    CampoTexto("Confirmar contraseña", s.confirmacion, KeyboardType.Password, esClave = true, onCambio = vm::onConfirmacion)
                    MensajeError(s.error)
                    PrimaryButton("Guardar contraseña", loading = s.cargando) { vm.guardar() }
                }
                PasoRecuperar.LISTO -> {
                    Titulo("¡Listo!", "Ya puedes ingresar con tu nueva contraseña.")
                    PrimaryButton("Ingresar") { onListo() }
                }
            }
        }
    }
}

/** Seis casillas, como RP2 del Figma, dibujadas sobre un solo campo de texto. */
@Composable
private fun CampoCodigo(codigo: String, conError: Boolean, onCambio: (String) -> Unit) {
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { foco.requestFocus() }
    val error = BarrioColors.Error
    val activo = BarrioColors.Yellow
    val normal = BarrioColors.YellowStroke
    val fondo = BarrioColors.Surface
    val texto = BarrioColors.TextPrimary
    BasicTextField(
        value = codigo,
        onValueChange = onCambio,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.focusRequester(foco).testTag("codigo_input"),
        decorationBox = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                repeat(6) { i ->
                    val borde = when {
                        conError -> error
                        i == codigo.length -> activo
                        else -> normal
                    }
                    Box(
                        Modifier.size(width = 46.dp, height = 56.dp)
                            .background(fondo, RoundedCornerShape(12.dp))
                            .border(2.dp, borde, RoundedCornerShape(12.dp)),
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
                color = if (cumple) BarrioColors.Yellow else BarrioColors.TextHint,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun Titulo(titulo: String, texto: String) {
    Text(titulo, color = BarrioColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    Text(texto, color = BarrioColors.TextHint, fontSize = 15.sp)
}

@Composable
private fun MensajeError(error: String?) {
    if (error != null) Text(error, color = BarrioColors.Error, fontSize = 14.sp, modifier = Modifier.testTag("recuperar_error"))
}

@Composable
private fun CampoTexto(etiqueta: String, valor: String, teclado: KeyboardType, esClave: Boolean = false, onCambio: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        visualTransformation = if (esClave) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = BarrioColors.TextPrimary,
            unfocusedTextColor = BarrioColors.TextPrimary,
            focusedBorderColor = BarrioColors.Yellow,
            unfocusedBorderColor = BarrioColors.YellowStroke,
            focusedLabelColor = BarrioColors.Yellow,
            unfocusedLabelColor = BarrioColors.TextHint,
            cursorColor = BarrioColors.Yellow,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
