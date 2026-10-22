package com.barriogo.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.R
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.components.TextLinkButton
import com.barriogo.app.ui.theme.BarrioColors

enum class BiometricState { IDLE, SCANNING, ERROR, SUCCESS }

/** Colores del sensor en cada estado del Figma (R7: Oferta, Leyendo, Error, Listo). */
private data class SensorLook(val fondo: Color, val borde: Color, val anillo: Float, val anilloGrosor: Int)

@Composable
private fun sensorLook(state: BiometricState): SensorLook {
    val dark = BarrioColors.isDark
    return when (state) {
        BiometricState.IDLE ->
            if (dark) SensorLook(Color(0xFF22593F), Color(0xFFCCAC6D), 0.4f, 1) else SensorLook(Color(0xFFFFFDF9), Color(0xFF6B5628), 0.9f, 2)
        BiometricState.SCANNING ->
            if (dark) SensorLook(Color(0xFF22593F), Color(0xFFF2C849), 1f, 1) else SensorLook(Color(0xFFFFFDF9), Color(0xFFB8860B), 1f, 2)
        BiometricState.ERROR ->
            if (dark) SensorLook(Color(0xFF22593F), Color(0xFFF2B544), 0.6f, 1) else SensorLook(Color(0xFFFFFDF9), Color(0xFFC2410C), 1f, 2)
        BiometricState.SUCCESS ->
            if (dark) SensorLook(Color(0x406FCF97), Color(0xFF6FCF97), 1f, 1) else SensorLook(Color(0xFFE8F5EC), Color(0xFF1F8A4C), 1f, 2)
    }
}

/**
 * R7 · Registro: Activar huella. La huella solo se activa si Android la confirma con BiometricPrompt.
 */
@Composable
fun BiometricScreen(
    onFinished: (enabled: Boolean) -> Unit
) {
    var state by remember { mutableStateOf(BiometricState.IDLE) }
    val context = LocalContext.current
    var aviso by remember { mutableStateOf<String?>(null) }

    fun activarHuella() {
        aviso = null
        if (!Huella.disponible(context)) {
            aviso = Huella.motivoNoDisponible(context)
            return
        }
        state = BiometricState.SCANNING
        Huella.pedir(
            context,
            titulo = "Activar huella",
            subtitulo = "Apoya el dedo para confirmar",
            onOk = { state = BiometricState.SUCCESS },
            onError = { mensaje ->
                aviso = mensaje
                state = BiometricState.ERROR
            },
        )
    }

    val (titulo, subtitulo) = when (state) {
        BiometricState.IDLE -> "Entra con tu huella" to "Es más rápido que escribir tu clave y funciona sin internet. Puedes activarla ahora o más tarde."
        BiometricState.SCANNING -> "Pon tu dedo en el sensor" to "Mantenlo apoyado hasta que el círculo se complete."
        BiometricState.ERROR -> "No pudimos leerla" to "Puede que tu dedo estuviera húmedo o mal apoyado. Inténtalo otra vez."
        BiometricState.SUCCESS -> "Huella activada" to "Desde ahora puedes entrar a tu tienda con solo apoyar el dedo. Tu clave de 4 números sigue funcionando."
    }
    val look = sensorLook(state)

    Column(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 72.dp, bottom = 48.dp)
            .testTag("biometric_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        // Encabezado
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                titulo,
                color = BarrioColors.Title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().testTag("biometric_title"),
            )
            Text(
                subtitulo,
                color = BarrioColors.Body,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.weight(1f))

        // Biometric Sensor Area
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val tocable = state == BiometricState.IDLE || state == BiometricState.ERROR
            Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                // Outer decorative ring (inset -18)
                Box(
                    Modifier
                        .requiredSize(140.dp)
                        .alpha(look.anillo)
                        .border(look.anilloGrosor.dp, look.borde, CircleShape)
                )
                // Sensor icon con resplandor verde de 15
                Box(
                    Modifier
                        .size(104.dp)
                        .drawBehind {
                            val r = size.minDimension / 2
                            drawCircle(
                                Brush.radialGradient(
                                    0f to Color(0x8022593F), r / (r + 15.dp.toPx()) to Color(0x8022593F), 1f to Color.Transparent,
                                    center = center, radius = r + 15.dp.toPx(),
                                ),
                                radius = r + 15.dp.toPx(),
                            )
                        }
                        .clip(CircleShape)
                        .background(look.fondo)
                        .border(2.dp, look.borde, CircleShape)
                        .clickable(enabled = tocable) { activarHuella() }
                        .testTag("fingerprint_sensor_badge"),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(44.dp)) {
                        Icon(
                            painterResource(R.drawable.fig_huella),
                            contentDescription = "Huella",
                            tint = look.borde,
                            modifier = Modifier.wrapContentSize(Alignment.TopStart, unbounded = true),
                        )
                    }
                }
            }
            if (state != BiometricState.IDLE) {
                Text(
                    when (state) {
                        BiometricState.SCANNING -> "Leyendo tu huella…"
                        BiometricState.ERROR -> "Vuelve a tocar el sensor"
                        else -> "Huella registrada"
                    },
                    color = look.borde,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 52.dp),
                )
            }
        }

        // Action Area (Bottom Pinned)
        Column(
            Modifier.fillMaxWidth().widthIn(max = 384.dp).padding(top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (state) {
                BiometricState.SUCCESS -> PrimaryButton(
                    "Continuar",
                    modifier = Modifier.testTag("biometric_continue_button"),
                ) { onFinished(true) }
                BiometricState.SCANNING -> PrimaryButton(
                    "Leyendo…",
                    enabled = false,
                    modifier = Modifier.testTag("activate_biometric_button"),
                ) {}
                else -> {
                    PrimaryButton(
                        if (state == BiometricState.ERROR) "Intentar de nuevo" else "Activar mi huella",
                        modifier = Modifier.testTag("activate_biometric_button"),
                    ) { activarHuella() }
                    TextLinkButton(
                        "Ahora no, usar solo mi clave",
                        modifier = Modifier.testTag("skip_biometric_button"),
                    ) { onFinished(false) }
                }
            }

            // Aviso del sistema (sin lector, sin huellas guardadas, cancelado…)
            aviso?.let {
                Text(
                    it,
                    color = BarrioColors.Error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().testTag("biometric_aviso"),
                )
            }

            Text(
                "Tu huella se queda guardada en este celular. No la enviamos a ningún lado.",
                color = BarrioColors.Note,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
