package com.barriogo.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import com.barriogo.app.R
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.ui.theme.BarrioColors
import com.barriogo.app.ui.theme.Danger

/**
 * "Botón Principal" del Figma: amarillo #F2C849, 56 de alto, esquinas de 8, Inter SemiBold 16.
 * Deshabilitado se ve dorado apagado (como "Leyendo…").
 */
@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    val activo = enabled && !loading
    val dark = BarrioColors.isDark
    val fondo = when {
        activo || loading -> Color(0xFFF2C849)
        dark -> Color(0xFFCCAC6D)
        else -> Color(0xFFCBBF9A)
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .alpha(if (activo || loading) 1f else if (dark) 0.45f else 0.55f)
            .clip(RoundedCornerShape(8.dp))
            .background(fondo)
            .clickable(enabled = activo, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BarrioColors.OnYellow, strokeWidth = 2.dp)
        } else {
            Text(text, color = BarrioColors.OnYellow, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
    }
}

/**
 * "Submit Button" del Figma (L2 Ingreso, RP1…): amarillo, 56 de alto, esquinas de 12,
 * texto SemiBold 16 con flecha a la derecha y un resplandor amarillo suave debajo.
 */
@Composable
fun SubmitArrowButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    val forma = RoundedCornerShape(12.dp)
    val activo = enabled && !loading
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .alpha(if (enabled || loading) 1f else 0.45f)
            .shadow(7.dp, forma, ambientColor = Color(0x33F2C849), spotColor = Color(0x33F2C849))
            .clip(forma)
            .background(Color(0xFFF2C849))
            .clickable(enabled = activo, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = BarrioColors.OnYellow, strokeWidth = 2.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text, color = BarrioColors.OnYellow, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
                Icon(painterResource(R.drawable.fig_flecha_derecha), null, tint = BarrioColors.OnYellow)
            }
        }
    }
}

/** "Botón Omitir" del Figma: solo texto dorado SemiBold 16 en una franja táctil de 48. */
@Composable
fun TextLinkButton(text: String, modifier: Modifier = Modifier, color: Color = BarrioColors.Gold, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

/** Botón secundario: borde, fondo transparente. Para "volver a hablar", "cancelar", etc. */
@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun DangerButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = Color.White),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}
