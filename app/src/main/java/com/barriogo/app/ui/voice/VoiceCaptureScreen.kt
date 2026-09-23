package com.barriogo.app.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.barriogo.app.domain.MovementType
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.BarrioTopBar
import com.barriogo.app.ui.components.Card
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.components.SecondaryButton
import com.barriogo.app.ui.theme.Accent
import com.barriogo.app.ui.theme.OnAccent
import com.barriogo.app.ui.theme.SurfaceDark

/**
 * V1 · Registrar por voz. Pide el permiso de micrófono si falta, escucha con [VoiceRecognizer]
 * y muestra el resultado de [MovementAgent] en la misma pantalla (propuesta o "no te entendí").
 */
@Composable
fun VoiceCaptureScreen(initialMode: MovementType, onBack: () -> Unit, onConfirmed: () -> Unit, onWriteInstead: () -> Unit) {
    val vm = barrioViewModel { VoiceViewModel(it) }
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) vm.startListening(context)
    }

    LaunchedEffect(initialMode) { vm.setMode(initialMode) }

    if (s.phase == VoicePhase.PROPOSAL || s.phase == VoicePhase.NOT_UNDERSTOOD) {
        ConfirmDraftScreen(onBack = onBack, onConfirmed = onConfirmed, onWriteInstead = onWriteInstead)
        return
    }

    Column(Modifier.fillMaxSize()) {
        BarrioTopBar("Registrar por voz", onBack = onBack)
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Habla y BarrioGo registra el movimiento por ti.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Text("¿Qué vas a registrar?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ModeTab("Venta", s.mode == MovementType.VENTA, Modifier.weight(1f)) { vm.setMode(MovementType.VENTA) }
                ModeTab("Fiado", s.mode == MovementType.FIADO, Modifier.weight(1f)) { vm.setMode(MovementType.FIADO) }
            }
            Spacer(Modifier.height(28.dp))
            MicButton(listening = s.phase == VoicePhase.LISTENING || s.phase == VoicePhase.PROCESSING) {
                if (hasPermission) vm.startListening(context) else permLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                when (s.phase) {
                    VoicePhase.LISTENING -> "Escuchando…"
                    VoicePhase.PROCESSING -> "Entendiendo lo que dijiste…"
                    else -> "Mantén presionado para hablar"
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Toca el micrófono y dilo con tus palabras",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (s.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(s.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.weight(1f))
            Card(color = SurfaceDark) {
                Text("Por ejemplo", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("\u00ab Vendí 2 gaseosas por 8 mil \u00bb", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                Text("\u00ab Fiado para Marta, arroz y leche, 24 mil \u00bb", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(Modifier.height(12.dp))
            SecondaryButton("¿El micrófono no funciona? Escribir a mano") { onWriteInstead() }
        }
    }
}

@Composable
private fun MicButton(listening: Boolean, onClick: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "mic")
    val scale by infinite.animateFloat(
        initialValue = 1f, targetValue = if (listening) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse), label = "scale",
    )
    Box(
        Modifier.size(120.dp).scale(if (listening) scale else 1f).clip(CircleShape).background(Accent)
            .padding(0.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.IconButton(onClick = onClick, modifier = Modifier.size(120.dp)) {
            androidx.compose.material3.Icon(Icons.Filled.Mic, contentDescription = "Hablar", tint = OnAccent, modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun ModeTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(if (selected) Accent else SurfaceDark)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) OnAccent else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
    }
}
