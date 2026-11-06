package com.barriogo.app.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.barriogo.app.ai.DraftIssue
import com.barriogo.app.domain.Money
import com.barriogo.app.domain.MovementType
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.Card
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.components.SecondaryButton
import com.barriogo.app.ui.components.StatusChip
import com.barriogo.app.ui.theme.Accent
import com.barriogo.app.ui.theme.NegativeCoral
import com.barriogo.app.ui.theme.PositiveGreen

/**
 * V2/V3 · "Esto entendí": muestra la propuesta del agente para que el usuario la confirme o la
 * corrija por voz (V4) antes de guardar. Si no se entendió nada, ofrece repetir o escribir a mano.
 */
@Composable
fun ConfirmDraftScreen(onBack: () -> Unit, onConfirmed: () -> Unit, onWriteInstead: () -> Unit) {
    val vm = barrioViewModel { VoiceViewModel(it) }
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    var editing by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startEditRecognition(context, vm) { editing = false }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(if (s.mode == MovementType.VENTA) "Confirmar venta" else "Confirmar fiado", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))

        if (s.phase == VoicePhase.NOT_UNDERSTOOD) {
            Card {
                Text("No te entendí bien", style = MaterialTheme.typography.titleMedium)
                Text(
                    s.reason ?: "Intenta decirlo de otra forma.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (s.transcript.isNotBlank()) {
                    Text("Escuché: \u00ab${s.transcript}\u00bb", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            SecondaryButton("Volver a hablar") { vm.retry() }
            Spacer(Modifier.height(10.dp))
            PrimaryButton("Escribir a mano") { onWriteInstead() }
            return@Column
        }

        val d = s.draft
        Text("Esto entendí", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        if (d != null) {
            Card {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatusChip(d.type.name, if (d.type == MovementType.VENTA) PositiveGreen else Accent)
                    Text(
                        "Corregir", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable { editing = true },
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (d.type == MovementType.VENTA) {
                    Text("${d.quantity} × ${d.itemText.ifBlank { "Venta" }}", style = MaterialTheme.typography.titleLarge)
                } else {
                    Text(d.clientName ?: d.heardClient ?: "Cliente por definir", style = MaterialTheme.typography.titleLarge)
                    if (d.itemText.isNotBlank()) Text(d.itemText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(Money.cop(d.amount), style = com.barriogo.app.ui.theme.MoneyDisplay, modifier = Modifier.padding(top = 6.dp))
                if (d.issues.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(issueMessage(d.issues.first(), d.heardClient), color = NegativeCoral, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (editing) {
                Spacer(Modifier.height(12.dp))
                SecondaryButton(if (s.phase == VoicePhase.LISTENING) "Escuchando…" else "Toca para corregir por voz") {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (granted) startEditRecognition(context, vm) { editing = false } else permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("¿Está correcto?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton("Volver a hablar", modifier = Modifier.weight(1f)) { vm.retry() }
            PrimaryButton("Confirmar", modifier = Modifier.weight(1f), enabled = d != null && canConfirm(d)) {
                vm.confirm(onConfirmed)
            }
        }
        Text(
            if (d?.type == MovementType.VENTA) "Se registrará venta en movimientos y sumará a tu caja."
            else "Se agregará al historial de fiados de ${d?.clientName ?: d?.heardClient ?: "tu cliente"}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/**
 * Bloquea confirmar solo cuando falta el monto o cuando el fiado no tiene ni un cliente conocido
 * ni un nombre que se pueda crear. Un cliente nuevo oído por voz sí se puede confirmar: se crea
 * al guardar (ver [VoiceViewModel.confirm]).
 */
private fun canConfirm(d: com.barriogo.app.ai.MovementDraft): Boolean {
    if (d.amount <= 0) return false
    if (d.type == MovementType.FIADO && d.clientId == null && d.heardClient.isNullOrBlank()) return false
    return true
}

private fun issueMessage(issue: DraftIssue, heardClient: String?): String = when (issue) {
    DraftIssue.AMOUNT_MISSING -> "No entendí el monto. Corrige por voz o escribe el valor."
    DraftIssue.CLIENT_UNKNOWN ->
        if (heardClient.isNullOrBlank()) "No entendí a quién es el fiado. Corrige por voz para agregarlo."
        else "\"$heardClient\" no está en tu lista de clientes. Se creará al confirmar."
    DraftIssue.ITEM_MISSING -> "No entendí qué se llevó. Puedes corregir por voz para agregarlo."
}

private fun startEditRecognition(context: android.content.Context, vm: VoiceViewModel, onFinished: () -> Unit) {
    com.barriogo.app.ai.VoiceRecognizer(context).start(
        onListening = {},
        onResult = { text -> vm.editWithVoice(text); onFinished() },
        onError = { onFinished() },
    )
}
