package com.barriogo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.ui.theme.BarrioColors

enum class BottomDest(val route: String, val label: String) {
    INICIO("home", "Inicio"),
    MOVIMIENTOS("movements", "Movimientos"),
    REGISTRAR("register_movement", "Registrar"),
    CAJA("cash", "Caja"),
}

/**
 * "Menú glass" del Figma: cápsula flotante de 364 × 72 (24 de margen a los lados) con fondo
 * translúcido, borde claro sutil y sombra. La pestaña activa va en un rectángulo redondeado
 * más claro con ícono y texto amarillos.
 */
@Composable
fun BarrioBottomBar(
    current: BottomDest,
    onSelect: (BottomDest) -> Unit,
    onOpenRegisterMenu: () -> Unit
) {
    val dark = BarrioColors.isDark
    val capsula = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .testTag("barrio_bottom_bar"),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .height(72.dp)
                // Sombra amplia y oscura para que el menú se vea flotando sobre la pantalla
                .shadow(24.dp, capsula, ambientColor = Color.Black.copy(alpha = 0.6f), spotColor = Color.Black.copy(alpha = 0.75f))
                .clip(capsula)
                .background(
                    if (dark) Brush.verticalGradient(listOf(Color(0xFF34483E), Color(0xFF233329)))
                    else SolidColor(Color(0xFFFFFDF9).copy(alpha = 0.95f))
                )
                .border(1.dp, if (dark) Color.White.copy(alpha = 0.22f) else Color(0x336B5628), capsula)
                .padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(Icons.Outlined.Home, Icons.Filled.Home, "Inicio", current == BottomDest.INICIO, "nav_inicio") {
                onSelect(BottomDest.INICIO)
            }
            BottomNavItem(Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Filled.ReceiptLong, "Movimientos", current == BottomDest.MOVIMIENTOS, "nav_movimientos") {
                onSelect(BottomDest.MOVIMIENTOS)
            }
            // "Registrar" abre el menú de opciones (voz, escanear, a mano)
            BottomNavItem(Icons.Filled.Add, Icons.Filled.Add, "Registrar", current == BottomDest.REGISTRAR, "nav_registrar", onClick = onOpenRegisterMenu)
            BottomNavItem(Icons.Outlined.PointOfSale, Icons.Filled.PointOfSale, "Caja", current == BottomDest.CAJA, "nav_caja") {
                onSelect(BottomDest.CAJA)
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    iconSelected: ImageVector,
    label: String,
    isSelected: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    val dark = BarrioColors.isDark
    // Todo el menú va en amarillo: la pestaña activa más intensa y en negrita
    val activo = if (dark) Color(0xFFF2C849) else Color(0xFF8A6A12)
    val inactivo = if (dark) Color(0xFFCCAC6D) else Color(0xFF8A6A12).copy(alpha = 0.8f)
    val color = if (isSelected) activo else inactivo
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight()
            // Resaltado y toque en forma de cápsula (redondo), no de cuadro
            .clip(RoundedCornerShape(percent = 50))
            .background(if (isSelected) (if (dark) Color(0xFF41564B) else Color(0x1A6B5628)) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
            .testTag(tag)
    ) {
        Icon(
            if (isSelected) iconSelected else icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = color,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

/**
 * Overlay "¿Cómo quieres registrar?" del Figma: tarjeta flotante de vidrio sobre el menú,
 * con tres círculos grandes (Por voz, Escanear, A mano). Tocar afuera la cierra.
 */
@Composable
fun RegisterChooserSheet(
    onDismiss: () -> Unit,
    onVoice: () -> Unit,
    onScan: () -> Unit,
    onManual: () -> Unit
) {
    val dark = BarrioColors.isDark
    val forma = RoundedCornerShape(24.dp)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (dark) 0.15f else 0.08f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .shadow(24.dp, forma, ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = 0.6f))
                .clip(forma)
                .background(
                    if (dark) Brush.verticalGradient(listOf(Color(0xEB4B6056), Color(0xEB3A4D43)))
                    else SolidColor(Color(0xF5FFFDF9))
                )
                .border(1.dp, if (dark) Color.White.copy(alpha = 0.22f) else Color(0x336B5628), forma)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp)
                .testTag("register_chooser_sheet")
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "¿Cómo quieres registrar?",
                    color = BarrioColors.Title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (dark) Color.White.copy(alpha = 0.12f) else Color(0x1A0F2E22))
                        .clickable(onClick = onDismiss)
                        .testTag("register_chooser_close"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.HighlightOff, "Cerrar", tint = BarrioColors.Title, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val menta = Color(0xFF6FCF97)
                RegisterOptionCircle("Por voz", Icons.Filled.Mic, menta, "choose_voice") {
                    onDismiss()
                    onVoice()
                }
                RegisterOptionCircle("Escanear", Icons.Filled.Receipt, Color(0xFFF2C849), "choose_scan") {
                    onDismiss()
                    onScan()
                }
                RegisterOptionCircle("A mano", Icons.Filled.Add, menta, "choose_manual") {
                    onDismiss()
                    onManual()
                }
            }
        }
    }
}

@Composable
private fun RegisterOptionCircle(
    title: String,
    icon: ImageVector,
    fondo: Color,
    tag: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(fondo),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = Color(0xFF164030), modifier = Modifier.size(26.dp))
        }
        Text(title, color = BarrioColors.Title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
