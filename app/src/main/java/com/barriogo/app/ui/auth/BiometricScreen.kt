package com.barriogo.app.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.ui.theme.BarrioColors
import androidx.compose.ui.platform.LocalContext

enum class BiometricState { IDLE, SCANNING, SUCCESS }

/**
 * Screen 8 · Entra con tu huella (Biometría interactiva del prototipo).
 */
@Composable
fun BiometricScreen(
    onFinished: (enabled: Boolean) -> Unit
) {
    var state by remember { mutableStateOf(BiometricState.IDLE) }
    val context = LocalContext.current
    var aviso by remember { mutableStateOf<String?>(null) }

    /** Activa la huella solo si Android la confirma con BiometricPrompt (antes era una animación). */
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
                state = BiometricState.IDLE
            },
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == BiometricState.SCANNING) 1.15f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .systemBarsPadding()
            .padding(24.dp)
            .testTag("biometric_screen")
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(28.dp))

                Text(
                    when (state) {
                        BiometricState.IDLE -> "Entra con tu huella"
                        BiometricState.SCANNING -> "Pon tu dedo en el sensor"
                        BiometricState.SUCCESS -> "Huella activada"
                    },
                    color = BarrioColors.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("biometric_title")
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    when (state) {
                        BiometricState.IDLE -> "Es más rápido que escribir tu clave y funciona sin internet. Puedes activarla ahora o más tarde."
                        BiometricState.SCANNING -> "Mantenlo apoyado hasta que el círculo se complete."
                        BiometricState.SUCCESS -> "Desde ahora puedes entrar a tu tienda con solo apoyar el dedo. Tu clave de 4 números sigue funcionando."
                    },
                    color = BarrioColors.TextHint,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Fingerprint Illustration
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .scale(if (state == BiometricState.SCANNING) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(BarrioColors.CardBg)
                    .border(
                        width = 2.dp,
                        color = when (state) {
                            BiometricState.SUCCESS -> BarrioColors.PositiveGreen
                            BiometricState.SCANNING -> BarrioColors.Yellow
                            else -> BarrioColors.YellowStroke
                        },
                        shape = CircleShape
                    )
                    .clickable {
                        if (state == BiometricState.IDLE) {
                            state = BiometricState.SCANNING
                        }
                    }
                    .testTag("fingerprint_sensor_badge"),
                contentAlignment = Alignment.Center
            ) {
                if (state == BiometricState.SUCCESS) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(BarrioColors.PositiveGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = BarrioColors.OnYellow,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                } else {
                    Icon(
                        Icons.Outlined.Fingerprint,
                        contentDescription = "Huella",
                        tint = if (state == BiometricState.SCANNING) BarrioColors.Yellow else BarrioColors.TextHint,
                        modifier = Modifier.size(90.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state == BiometricState.SUCCESS) {
                    Button(
                        onClick = { onFinished(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("biometric_continue_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BarrioColors.Yellow,
                            contentColor = BarrioColors.OnYellow
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Text("Continuar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = { activarHuella() },
                        enabled = state == BiometricState.IDLE,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("activate_biometric_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BarrioColors.Yellow,
                            contentColor = BarrioColors.OnYellow
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Text(
                            if (state == BiometricState.SCANNING) "Leyendo tu huella..." else "Activar mi huella",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        "Ahora no, usar solo mi clave",
                        color = BarrioColors.Yellow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { onFinished(false) }
                            .testTag("skip_biometric_button")
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (aviso != null) {
                    Text(
                        aviso!!,
                        color = BarrioColors.Error,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp).testTag("biometric_aviso"),
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Text(
                    "Tu huella se queda guardada en este celular. No la enviamos a ningún lado.",
                    color = BarrioColors.TextHint.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
