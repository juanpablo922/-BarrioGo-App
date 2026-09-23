package com.barriogo.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.components.SecondaryButton
import com.barriogo.app.ui.theme.Accent
import com.barriogo.app.ui.theme.SurfaceDark

/** R1-R3 · Registro del negocio en tres pasos: datos del propietario, categoría y contraseña. */
@Composable
fun RegisterScreen(onFinished: () -> Unit) {
    val vm = barrioViewModel { AuthViewModel(it) }
    val s by vm.register.collectAsState()

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        StepDots(total = 3, current = s.step)
        Spacer(Modifier.height(20.dp))
        when (s.step) {
            0 -> {
                Text("Cuéntanos de tu tienda", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                Field("Tu nombre", s.ownerName) { v -> vm.updateRegister { it.copy(ownerName = v) } }
                Field("Nombre de la tienda", s.storeName) { v -> vm.updateRegister { it.copy(storeName = v) } }
                Field("Celular", s.phone, KeyboardType.Phone) { v -> vm.updateRegister { it.copy(phone = v) } }
                Field("Correo", s.email, KeyboardType.Email) { v -> vm.updateRegister { it.copy(email = v) } }
            }
            1 -> {
                Text("¿Qué tipo de negocio es?", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                LazyColumn {
                    items(vm.categories) { cat ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(if (s.category == cat) SurfaceDark else androidx.compose.ui.graphics.Color.Transparent)
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = s.category == cat, onClick = { vm.updateRegister { it.copy(category = cat) } })
                            Text(cat, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            else -> {
                Text("Crea una contraseña", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "La usarás para volver a entrar si sales de la tienda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Field("Contraseña", s.password, KeyboardType.Password, password = true) { v -> vm.updateRegister { it.copy(password = v) } }
                Field("Confirmar contraseña", s.confirmPassword, KeyboardType.Password, password = true) { v ->
                    vm.updateRegister { it.copy(confirmPassword = v) }
                }
            }
        }
        if (s.error != null) {
            Text(s.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.weight(1f))
        Row {
            if (s.step > 0) {
                SecondaryButton("Atrás", modifier = Modifier.weight(1f)) { vm.prevStep() }
                Spacer(Modifier.width(12.dp))
            }
            PrimaryButton(if (s.step < 2) "Continuar" else "Crear mi tienda", modifier = Modifier.weight(1f)) {
                if (vm.nextStep()) {
                    vm.saveBusinessData()
                    onFinished()
                }
            }
        }
    }
}

@Composable
private fun StepDots(total: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            androidx.compose.foundation.layout.Box(
                Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (i <= current) Accent else SurfaceDark),
            )
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    )
}


