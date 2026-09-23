package com.barriogo.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.theme.Accent

/** L2 · Ingreso con correo y contraseña de la tienda. */
@Composable
fun LoginScreen(onLoggedIn: () -> Unit, onGoRegister: () -> Unit) {
    val vm = barrioViewModel { AuthViewModel(it) }
    val state by vm.login.collectAsState()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Storefront, contentDescription = null, tint = Accent, modifier = Modifier.padding(bottom = 16.dp))
        Text("Ingresa a tu tienda", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Usa el correo y la contraseña con los que registraste tu negocio.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = { v -> vm.updateLogin { it.copy(email = v) } },
            label = { Text("Correo") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.password,
            onValueChange = { v -> vm.updateLogin { it.copy(password = v) } },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.error != null) {
            Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Ingresar") { if (vm.submitLogin()) onLoggedIn() }
        Spacer(Modifier.height(12.dp))
        androidx.compose.material3.TextButton(onClick = onGoRegister, modifier = Modifier.fillMaxWidth()) {
            Text("¿Aún no tienes tienda registrada? Regístrala", color = Accent, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
