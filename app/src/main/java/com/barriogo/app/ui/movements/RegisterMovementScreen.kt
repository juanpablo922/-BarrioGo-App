package com.barriogo.app.ui.movements

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.barriogo.app.domain.MovementType
import com.barriogo.app.domain.PayMethod
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.AmountField
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.components.SecondaryButton
import com.barriogo.app.ui.theme.Accent
import com.barriogo.app.ui.theme.OnAccent
import com.barriogo.app.ui.theme.SurfaceDark

/** M1 · Registrar movimiento a mano: elegir tipo, monto y una descripción opcional. */
@Composable
fun RegisterMovementScreen(
    onSaved: () -> Unit,
    onGoVoice: (MovementType) -> Unit,
    onGoScan: () -> Unit,
) {
    val vm = barrioViewModel { MovementsViewModel(it) }
    val sessionId by vm.activeSessionId.collectAsState()
    var type by remember { mutableStateOf(MovementType.VENTA) }
    var amount by remember { mutableLongStateOf(0L) }
    var detail by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Registrar movimiento", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text("¿Qué vas a registrar?", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TypeTab("Venta", type == MovementType.VENTA, Modifier.weight(1f)) { type = MovementType.VENTA }
            TypeTab("Gasto", type == MovementType.GASTO, Modifier.weight(1f)) { type = MovementType.GASTO }
            TypeTab("Fiado", type == MovementType.FIADO, Modifier.weight(1f)) { type = MovementType.FIADO }
        }
        Spacer(Modifier.height(20.dp))
        Text("¿Cuánto?", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        AmountField(value = amount, onValueChange = { amount = it })
        Spacer(Modifier.height(16.dp))
        Text("¿De qué? (opcional)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = detail, onValueChange = { detail = it },
            placeholder = { Text("Ej. Bolsa de arroz") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Hablar en vez de escribir", modifier = Modifier.weight(1f)) { onGoVoice(type) }
            if (type == MovementType.GASTO) {
                SecondaryButton("Escanear factura", modifier = Modifier.weight(1f)) { onGoScan() }
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton("Guardar movimiento", enabled = amount > 0) {
            vm.register(
                type = type, amount = amount,
                title = detail.ifBlank { if (type == MovementType.VENTA) "Venta mostrador" else if (type == MovementType.GASTO) "Gasto" else "Fiado" },
                detail = if (type == MovementType.VENTA) "Venta" else if (type == MovementType.GASTO) "Insumos" else "Fiado",
                sessionId = sessionId,
                onDone = onSaved,
            )
        }
        Text(
            "Elige tipo y escribe el monto.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun TypeTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Accent else SurfaceDark)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) OnAccent else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
    }
}
