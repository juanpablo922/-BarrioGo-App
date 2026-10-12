package com.barriogo.app.ui.voice

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AccountBalanceWallet
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
import com.barriogo.app.domain.Money
import com.barriogo.app.domain.MovementType
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.theme.BarrioColors

/**
 * V1 · Registrar por voz: Soporta Ingreso/Venta, Gasto y Fiado.
 * Cuenta con doble motor: reconocimiento integrado en la app con fallback automático
 * a la ventana nativa de Google Speech (ACTION_RECOGNIZE_SPEECH).
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
    val state by vm.state.collectAsState()
    var mode by remember { mutableStateOf(initialMode) }

    // Fallback nativo de Google Voice por si el servicio de fondo del dispositivo o emulador falla
    val speechIntentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull().orEmpty()
            if (spokenText.isNotBlank()) {
                vm.onTranscript(spokenText)
            }
        }
    }

    fun launchSystemSpeechPrompt() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-CO")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di tu movimiento (ej. Venta de 15 mil en gaseosa, Gasto de 8 mil en bolsas, Fiado de 20 mil a Carlos)")
        }
        try {
            speechIntentLauncher.launch(intent)
        } catch (_: Exception) {
            // Si el intent tampoco existe, se informa
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) {
            vm.setMode(mode)
            try {
                vm.startListening(ctx)
            } catch (_: Exception) {
                launchSystemSpeechPrompt()
            }
        } else {
            launchSystemSpeechPrompt()
        }
    }

    fun startListeningWithFallback() {
        vm.setMode(mode)
        val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            try {
                vm.startListening(ctx)
            } catch (_: Exception) {
                launchSystemSpeechPrompt()
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Si VoiceViewModel arroja error de reconocimiento, activar el diálogo nativo
    LaunchedEffect(state.error) {
        if (state.error != null && state.phase == VoicePhase.IDLE) {
            launchSystemSpeechPrompt()
        }
    }

    val isListening = state.phase == VoicePhase.LISTENING
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
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
                .verticalScroll(rememberScrollState())
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
                    "Habla y BarrioGo registra ventas, gastos o fiados al instante.",
                    color = BarrioColors.TextHint,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(16.dp))

                // Toggle: Venta / Ingreso vs Gasto vs Fiado
                Text("¿Qué deseas registrar?", color = BarrioColors.TextHint, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VoiceModeTab(
                        title = "Ingreso",
                        icon = Icons.Outlined.PointOfSale,
                        isSelected = mode == MovementType.VENTA,
                        modifier = Modifier.weight(1f),
                        onClick = { mode = MovementType.VENTA; vm.setMode(mode) }
                    )
                    VoiceModeTab(
                        title = "Gasto",
                        icon = Icons.Outlined.AccountBalanceWallet,
                        isSelected = mode == MovementType.GASTO,
                        modifier = Modifier.weight(1f),
                        onClick = { mode = MovementType.GASTO; vm.setMode(mode) }
                    )
                    VoiceModeTab(
                        title = "Fiado",
                        icon = Icons.Outlined.Person,
                        isSelected = mode == MovementType.FIADO,
                        modifier = Modifier.weight(1f),
                        onClick = { mode = MovementType.FIADO; vm.setMode(mode) }
                    )
                }

                Spacer(Modifier.height(22.dp))

                // Si ya reconoció y tiene una propuesta lista para confirmar
                if (state.phase == VoicePhase.PROPOSAL && state.draft != null) {
                    val d = state.draft!!
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(BarrioColors.CardBg)
                            .border(1.5.dp, BarrioColors.Yellow, RoundedCornerShape(20.dp))
                            .padding(20.dp)
                            .testTag("voice_proposal_card")
                    ) {
                        Column {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (d.type) {
                                        MovementType.VENTA -> BarrioColors.PositiveGreen.copy(alpha = 0.2f)
                                        MovementType.GASTO -> BarrioColors.NegativeRed.copy(alpha = 0.2f)
                                        else -> BarrioColors.Yellow.copy(alpha = 0.2f)
                                    }
                                ) {
                                    Text(
                                        when (d.type) {
                                            MovementType.VENTA -> "VENTA / INGRESO"
                                            MovementType.GASTO -> "GASTO"
                                            else -> "FIADO"
                                        },
                                        color = when (d.type) {
                                            MovementType.VENTA -> BarrioColors.PositiveGreen
                                            MovementType.GASTO -> BarrioColors.NegativeRed
                                            else -> BarrioColors.Yellow
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    "Escuchado 👂",
                                    color = BarrioColors.TextHint,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                Money.cop(d.amount),
                                color = BarrioColors.Yellow,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(6.dp))

                            val conceptText = when (d.type) {
                                MovementType.VENTA -> if (d.items.isNotEmpty()) d.itemText else "Venta general"
                                MovementType.GASTO -> if (d.items.isNotEmpty()) d.itemText else "Gasto de tienda"
                                else -> "Cliente: ${d.clientName ?: d.heardClient ?: "Pendiente"} · ${if (d.items.isNotEmpty()) d.itemText else "Mercancía"}"
                            }

                            Text(
                                conceptText,
                                color = BarrioColors.TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (state.transcript.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "\"${state.transcript}\"",
                                    color = BarrioColors.TextHint,
                                    fontSize = 13.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }

                            Spacer(Modifier.height(20.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { vm.retry() },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BarrioColors.CardSubtle)
                                ) {
                                    Icon(Icons.Filled.Refresh, null, tint = BarrioColors.TextHint, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Repetir", color = BarrioColors.TextHint, fontSize = 14.sp)
                                }

                                Button(
                                    onClick = { vm.confirm(onConfirmed) },
                                    modifier = Modifier.weight(1.5f).height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BarrioColors.Yellow,
                                        contentColor = BarrioColors.OnYellow
                                    )
                                ) {
                                    Text("Confirmar", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    // Tarjeta del Micrófono Central
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(BarrioColors.CardBg)
                            .border(1.dp, BarrioColors.CardSubtle, RoundedCornerShape(24.dp))
                            .padding(26.dp),
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
                                        if (isListening) {
                                            vm.stopListening()
                                        } else {
                                            startListeningWithFallback()
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

                            Spacer(Modifier.height(18.dp))

                            Text(
                                if (isListening) "Escuchando... Habla ahora"
                                else if (state.phase == VoicePhase.PROCESSING) "Procesando lo que dijiste..."
                                else "Toca para hablar",
                                color = BarrioColors.TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                if (isListening) "Di por ejemplo: \"Venta de 15 mil en gaseosa\""
                                else "Pulsa el micrófono o el botón amarillo de abajo",
                                color = BarrioColors.TextHint,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                if (state.phase == VoicePhase.NOT_UNDERSTOOD) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        state.reason ?: "No alcancé a entender el valor o concepto. Intenta nuevamente.",
                        color = BarrioColors.Error,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(18.dp))

                // Atajos de prueba rápida por voz (chips con un solo toque)
                Text("O prueba con estos ejemplos listos:", color = BarrioColors.TextHint, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { vm.onTranscript("Venta de 15000 en gaseosas") },
                        label = { Text("Venta $15k", fontSize = 12.sp, color = BarrioColors.TextPrimary) },
                        modifier = Modifier.weight(1f)
                    )
                    AssistChip(
                        onClick = { vm.onTranscript("Gasto de 8000 en bolsas de empaque") },
                        label = { Text("Gasto $8k", fontSize = 12.sp, color = BarrioColors.TextPrimary) },
                        modifier = Modifier.weight(1f)
                    )
                    AssistChip(
                        onClick = { vm.onTranscript("Fiado para Carlos arroz y leche 20000") },
                        label = { Text("Fiado $20k", fontSize = 12.sp, color = BarrioColors.TextPrimary) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Barra inferior con botón de hablar y enlace manual
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        if (isListening) {
                            vm.stopListening()
                        } else {
                            startListeningWithFallback()
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
                    Icon(Icons.Filled.Mic, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isListening) "Terminar de escuchar" else "Activar micrófono",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "¿Prefieres escribir? Registrar a mano",
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
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) BarrioColors.Yellow else BarrioColors.TextHint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                title,
                color = if (isSelected) BarrioColors.Yellow else BarrioColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
