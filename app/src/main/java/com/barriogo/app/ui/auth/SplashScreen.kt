package com.barriogo.app.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.ui.components.BarrioLogo
import com.barriogo.app.ui.theme.BarrioColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * L1 · Splash BarrioGo: muestra la portada oficial mientras [decide] revisa la sesión
 * (Firebase) y navega a donde corresponda.
 */
@Composable
fun SplashScreen(decide: suspend () -> Unit) {
    val alphaAnim = remember { Animatable(0f) }
    val progreso = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { progreso.animateTo(1f, tween(2050, easing = LinearEasing)) }
        alphaAnim.animateTo(1f, animationSpec = tween(650))
        delay(1400)
        decide()
    }

    val brillo = Color(0xFFF2C849)
    val pista = if (BarrioColors.isDark) Color(0xFFCCAC6D) else Color(0xFF8A7346)
    Column(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            // "Background Pattern Effect": óvalo amarillo al 5% en el centro
            .drawBehind {
                val rx = size.width * 29.133f * 10f * 0.058926f / 412f
                val ry = size.height * 64.842f * 10f * 0.058926f / 917f
                drawOval(brillo.copy(alpha = 0.05f), Offset(center.x - rx, center.y - ry), Size(rx * 2, ry * 2))
            }
            .testTag("splash_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // pt 336 de 917: el bloque central y el del loader se reparten el resto por igual
        Spacer(Modifier.weight(336f / 290.5f))
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp).alpha(alphaAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        ) {
            BarrioLogo(Modifier.testTag("splash_logo"))
            Text(
                "Tu tienda al día. Sin dejar de atender.",
                color = if (BarrioColors.isDark) Color(0xFFF2F2F2) else Color(0xFF0F2E22),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("splash_subtitle"),
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(bottom = 40.dp), contentAlignment = Alignment.BottomCenter) {
            // Loader · Splash 3s: anillo de 40 con trazo de 4.4 y el progreso amarillo encima
            Canvas(Modifier.size(40.dp)) {
                val grosor = 4.4.dp.toPx()
                val arco = Size(size.width - grosor, size.height - grosor)
                val origen = Offset(grosor / 2, grosor / 2)
                drawArc(pista.copy(alpha = 0.25f), 0f, 360f, false, origen, arco, style = Stroke(grosor))
                drawArc(brillo, -90f, 360f * progreso.value.coerceAtLeast(0.04f), false, origen, arco, style = Stroke(grosor))
            }
        }
    }
}
