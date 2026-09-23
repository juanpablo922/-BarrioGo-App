package com.barriogo.app.ui.fiados

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.barriogo.app.domain.Money
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.AmountField
import com.barriogo.app.ui.components.BarrioTopBar
import com.barriogo.app.ui.components.Card
import com.barriogo.app.ui.components.PrimaryButton

/** Registrar abono: reduce la deuda del cliente elegido en Fiados. */
@Composable
fun RegisterPaymentScreen(clientId: Long, onBack: () -> Unit, onSaved: () -> Unit) {
    val vm = barrioViewModel { FiadosViewModel(it) }
    val debts by vm.debts.collectAsState()
    val debt = debts.firstOrNull { it.client.id == clientId }
    var amount by remember { mutableLongStateOf(0L) }

    Column(Modifier.fillMaxSize()) {
        BarrioTopBar("Registrar abono", onBack = onBack)
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Card {
                Text(debt?.client?.name ?: "Cliente", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Debe ${Money.cop(debt?.balance ?: 0)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(20.dp))
            Text("¿Cuánto abona?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            AmountField(value = amount, onValueChange = { amount = it })
            Spacer(Modifier.weight(1f))
            PrimaryButton("Guardar abono", enabled = amount > 0) {
                vm.registerPayment(clientId, amount, onSaved)
            }
        }
    }
}
