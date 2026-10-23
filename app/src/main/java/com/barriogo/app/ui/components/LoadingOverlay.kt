package com.barriogo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.barriogo.app.ui.theme.BarrioColors

/**
 * Cargando a pantalla completa mientras se guarda en Firebase (crear tienda, crear o editar perfiles).
 * Va en un Dialog para quedar encima de todo (también de las hojas inferiores) y bloquea toques y
 * el botón atrás para que no parezca que la app se trabó ni se envíe dos veces. Usa el mismo vidrio que el menú "¿Cómo quieres registrar?".
 */
@Composable
fun LoadingOverlay(mensaje: String, detalle: String = "Esto toma solo unos segundos. No cierres la app.") {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) { LoadingContent(mensaje, detalle) }
}

@Composable
private fun LoadingContent(mensaje: String, detalle: String) {
    val dark = BarrioColors.isDark
    val forma = RoundedCornerShape(24.dp)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (dark) 0.25f else 0.1f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            .testTag("loading_overlay"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 40.dp)
                .widthIn(max = 320.dp)
                .shadow(24.dp, forma, ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = 0.6f))
                .clip(forma)
                .background(
                    if (dark) Brush.verticalGradient(listOf(Color(0xF24B6056), Color(0xF23A4D43)))
                    else SolidColor(Color(0xFAFFFDF9))
                )
                .border(1.dp, if (dark) Color.White.copy(alpha = 0.22f) else Color(0x336B5628), forma)
                .padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Color(0xFFF2C849),
                trackColor = (if (dark) Color(0xFFCCAC6D) else Color(0xFF8A7346)).copy(alpha = 0.25f),
                strokeWidth = 4.dp,
            )
            Text(mensaje, color = BarrioColors.Title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(detalle, color = BarrioColors.Body, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}
