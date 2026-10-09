package com.barriogo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 * Barra inferior de 4 pestañas idéntica al prototipo Figma.
 */
@Composable
fun BarrioBottomBar(
    current: BottomDest,
    onSelect: (BottomDest) -> Unit,
    onOpenRegisterMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BarrioColors.DarkNav)
            .navigationBarsPadding()
            .height(68.dp)
            .testTag("barrio_bottom_bar")
    ) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Inicio
            BottomNavItem(
                icon = Icons.Outlined.Storefront,
                label = "Inicio",
                isSelected = current == BottomDest.INICIO,
                onClick = { onSelect(BottomDest.INICIO) },
                tag = "nav_inicio"
            )

            // 2. Movimientos
            BottomNavItem(
                icon = Icons.Outlined.ReceiptLong,
                label = "Movimientos",
                isSelected = current == BottomDest.MOVIMIENTOS,
                onClick = { onSelect(BottomDest.MOVIMIENTOS) },
                tag = "nav_movimientos"
            )

            // 3. Registrar (Center + button)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = (-6).dp)
                    .clickable(onClick = onOpenRegisterMenu)
                    .testTag("nav_registrar")
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(BarrioColors.Yellow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Registrar",
                        tint = BarrioColors.OnYellow,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "Registrar",
                    color = BarrioColors.Yellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 4. Caja
            BottomNavItem(
                icon = Icons.Outlined.AccountBalanceWallet,
                label = "Caja",
                isSelected = current == BottomDest.CAJA,
                onClick = { onSelect(BottomDest.CAJA) },
                tag = "nav_caja"
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(tag)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isSelected) BarrioColors.Yellow else BarrioColors.TextHint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = if (isSelected) BarrioColors.Yellow else BarrioColors.TextHint,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Diálogo / Bottom Sheet "¿Cómo quieres registrar?" (Screen 16 del video Figma).
 */
@Composable
fun RegisterChooserSheet(
    onDismiss: () -> Unit,
    onVoice: () -> Unit,
    onScan: () -> Unit,
    onManual: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(BarrioColors.Background)
                .border(1.dp, BarrioColors.YellowStroke, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(enabled = false) {}
                .padding(24.dp)
                .testTag("register_chooser_sheet")
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "¿Cómo quieres registrar?",
                        color = BarrioColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, null, tint = BarrioColors.TextHint)
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Por voz
                    RegisterOptionCircle(
                        title = "Por voz",
                        icon = Icons.Outlined.Mic,
                        onClick = {
                            onDismiss()
                            onVoice()
                        },
                        tag = "choose_voice"
                    )

                    // Escanear
                    RegisterOptionCircle(
                        title = "Escanear",
                        icon = Icons.Outlined.DocumentScanner,
                        onClick = {
                            onDismiss()
                            onScan()
                        },
                        tag = "choose_scan"
                    )

                    // A mano
                    RegisterOptionCircle(
                        title = "A mano",
                        icon = Icons.Filled.Add,
                        onClick = {
                            onDismiss()
                            onManual()
                        },
                        tag = "choose_manual"
                    )
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun RegisterOptionCircle(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(BarrioColors.CardBg)
                .border(1.5.dp, BarrioColors.Yellow, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = BarrioColors.Yellow,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            color = BarrioColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
