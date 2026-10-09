package com.barriogo.app.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.barriogo.app.domain.MovementType
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.theme.BarrioColors

/**
 * V1 · Registrar por voz (Screen 17 del prototipo Figma).
 */
@Composable
fun VoiceCaptureScreen(
    initialMode: MovementType,
    onBack: () -> Unit,
    onConfirmed: () -> Unit,
    onWriteInstead: () -> Unit
) {
    val ctx = LocalContext.current
    val vm = barrioViewModel { VoiceViewModel(it) }
    var mode by remember { mutableStateOf(initialMode) }
    var isListening by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) {
            isListening = true
            vm.setMode(mode)
            vm.startListening(ctx)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.2f else 1.05f,
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
            .testTag("voice_capture_screen")
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(Modifier.height(14.dp))

                // Header
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("voice_back_arrow")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = BarrioColors.Yellow)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Registrar por voz",
                        color = BarrioColors.Yellow,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("voice_title")
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    "Habla y BarrioGo registra el movimiento por ti.",
                    color = BarrioColors.TextHint,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(20.dp))

                // Toggle: Venta vs Fiado
                Text("¿Qué vas a registrar?", color = BarrioColors.TextHint, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VoiceModeTab(
                        title = "Venta",
                        icon = Icons.Outlined.PointOfSale,
                        isSelected = mode == MovementType.VENTA,
                        modifier = Modifier.weight(1f),
                        onClick = { mode = MovementType.VENTA }
                    )
                    VoiceModeTab(
                        title = "Fiado",
                        icon = Icons.Outlined.Person,
                        isSelected = mode == MovementType.FIADO,
                        modifier = Modifier.weight(1f),
                        onClick = { mode = MovementType.FIADO }
                    )
                }

                Spacer(Modifier.height(28.dp))

                // Microphone Big Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(BarrioColors.CardBg)
                        .border(1.dp, BarrioColors.CardSubtle, RoundedCornerShape(24.dp))
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(if (isListening) BarrioColors.Yellow.copy(alpha = 0.25f) else BarrioColors.Surface)
                                .border(2.dp, BarrioColors.Yellow, CircleShape)
                                .clickable {
                                    val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        isListening = !isListening
                                        if (isListening) {
                                            vm.setMode(mode)
                                            vm.startListening(ctx)
                                        } else {
                                            vm.stopListening()
                                        }
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Mic,
                                contentDescription = "Micrófono",
                                tint = BarrioColors.Yellow,
                                modifier = Modifier.size(46.dp)
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        Text(
                            if (isListening) "Escuchando..." else "Mantén presionado para hablar",
                            color = BarrioColors.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            "Toca el micrófono y dilo con tus palabras",
                            color = BarrioColors.TextHint,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Examples Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BarrioColors.Surface.copy(alpha = 0.5f))
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Por ejemplo:", color = BarrioColors.Yellow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "\"Vendí 2 gaseosas por 8 mil\"\n\"Fiado para Marta, arroz y leche, 24 mil\"",
                            color = BarrioColors.TextHint,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            isListening = true
                            vm.setMode(mode)
                            vm.startListening(ctx)
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("hold_to_talk_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BarrioColors.Yellow,
                        contentColor = BarrioColors.OnYellow
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text("Mantener para hablar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    "Por ahora la voz sirve para ventas y fiados.",
                    color = BarrioColors.TextHint,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    "¿El micrófono no funciona? Registrar a mano",
                    color = BarrioColors.Yellow,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onWriteInstead)
                )
            }
        }
    }
}

@Composable
private fun VoiceModeTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BarrioColors.CardBg)
            .border(
                1.5.dp,
                if (isSelected) BarrioColors.Yellow else BarrioColors.CardSubtle,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) BarrioColors.Yellow else BarrioColors.TextHint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                title,
                color = if (isSelected) BarrioColors.Yellow else BarrioColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
