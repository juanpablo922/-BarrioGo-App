package com.barriogo.app.ui.cash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.AmountField
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.theme.Accent

/** K1 · Apertura de caja: cuánto dinero hay al empezar el día. */
@Composable
fun OpenCashScreen(onOpened: () -> Unit) {
    val vm = barrioViewModel { CashViewModel(it) }
    var amount by remember { mutableLongStateOf(0L) }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.Payments, contentDescription = null, tint = Accent)
        Text("Abrir caja", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 12.dp))
        Text(
            "¿Cuánto dinero hay en la caja para empezar el día?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
        )
        AmountField(value = amount, onValueChange = { amount = it })
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Abrir caja", enabled = amount >= 0) { vm.openCash(amount, onOpened) }
    }
}
