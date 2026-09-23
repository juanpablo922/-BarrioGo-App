package com.barriogo.app.ui.expenses

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.barriogo.app.domain.ExpenseKind
import com.barriogo.app.domain.MovementType
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.AmountField
import com.barriogo.app.ui.components.BarrioTopBar
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.movements.MovementsViewModel
import com.barriogo.app.ui.theme.Accent
import com.barriogo.app.ui.theme.OnAccent
import com.barriogo.app.ui.theme.SurfaceDark

/** G2 · Registrar un gasto: proveedor, servicios u otros. */
@Composable
fun RegisterExpenseScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val vm = barrioViewModel { MovementsViewModel(it) }
    val sessionId by vm.activeSessionId.collectAsState()
    var kind by remember { mutableStateOf(ExpenseKind.PROVEEDOR) }
    var desc by remember { mutableStateOf("") }
    var amount by remember { mutableLongStateOf(0L) }

    Column(Modifier.fillMaxSize()) {
        BarrioTopBar("Registrar gasto", onBack = onBack)
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("Tipo de gasto", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExpenseKind.values().forEach { k ->
                    val sel = k == kind
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (sel) Accent else SurfaceDark)
                            .clickable { kind = k }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(k.label, color = if (sel) OnAccent else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge) }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("¿A quién le pagaste o qué compraste?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = desc, onValueChange = { desc = it },
                placeholder = { Text("Ej. Distribuidora La Central") }, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Text("¿Cuánto?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            AmountField(value = amount, onValueChange = { amount = it })
            Spacer(Modifier.weight(1f))
            PrimaryButton("Guardar gasto", enabled = amount > 0) {
                vm.register(
                    type = MovementType.GASTO, amount = amount, title = desc.ifBlank { kind.label },
                    detail = kind.detail, sessionId = sessionId, onDone = onSaved,
                )
            }
        }
    }
}
