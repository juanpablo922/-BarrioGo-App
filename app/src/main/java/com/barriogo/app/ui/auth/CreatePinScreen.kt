package com.barriogo.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.NumericKeypad
import com.barriogo.app.ui.components.PinDots

/**
 * R5 · Crear PIN de 4 dígitos (se pide dos veces para confirmarlo).
 * [onCreated] recibe el PIN ya confirmado; quien llama decide qué perfil crear con él.
 */
@Composable
fun CreatePinScreen(subtitle: String, onCreated: (String) -> Unit) {
    val vm = barrioViewModel { AuthViewModel(it) }
    val s by vm.pin.collectAsState()

    LaunchedEffect(Unit) { vm.resetPin() }
    LaunchedEffect(s.current, s.stage) {
        if (s.current.length == 4) {
            val done = vm.advanceCreation()
            if (done != null) onCreated(done)
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            if (s.stage == PinStage.ENTER) "Crea tu PIN" else "Confirma tu PIN",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 28.dp),
        )
        PinDots(length = 4, filled = s.current.length)
        if (s.error != null) {
            Text(s.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        }
        Spacer(Modifier.height(36.dp))
        NumericKeypad(onDigit = { vm.pressDigit(it) }, onBackspace = { vm.backspace() })
    }
}
