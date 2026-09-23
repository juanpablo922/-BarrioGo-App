package com.barriogo.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.barriogo.app.domain.Role
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.PrimaryButton

/** P1 · Crear un nuevo perfil (vendedor o administrador) para la tienda. */
@Composable
fun CreateProfileScreen(onDone: (Long) -> Unit) {
    val vm = barrioViewModel { AuthViewModel(it) }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(Role.SELLER) }
    var askingPin by remember { mutableStateOf(false) }

    if (askingPin) {
        CreatePinScreen(subtitle = "Este PIN abrirá el perfil de $name.") { pin ->
            vm.createProfile(name, role, pin) { id -> onDone(id) }
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Nuevo perfil", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = name, onValueChange = { name = it }, label = { Text("Nombre") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Text("Rol", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = role == Role.ADMIN, onClick = { role = Role.ADMIN })
            Text("Administrador", modifier = Modifier.padding(end = 16.dp))
            RadioButton(selected = role == Role.SELLER, onClick = { role = Role.SELLER })
            Text("Vendedor")
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton("Continuar", enabled = name.isNotBlank()) { askingPin = true }
    }
}
