package com.barriogo.app.ui.fiados

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.AmountField
import com.barriogo.app.ui.components.BarrioTopBar
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.theme.SurfaceDark

/** Nuevo fiado: elegir o crear cliente, qué se llevó y por cuánto. */
@Composable
fun NewFiadoScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val vm = barrioViewModel { FiadosViewModel(it) }
    val clients by vm.clients.collectAsState()
    var name by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var itemsText by remember { mutableStateOf("") }
    var amount by remember { mutableLongStateOf(0L) }

    val matches = remember(clients, name) {
        if (selectedId != null || name.isBlank()) emptyList() else clients.filter { it.name.contains(name, ignoreCase = true) }.take(4)
    }

    Column(Modifier.fillMaxSize()) {
        BarrioTopBar("Nuevo fiado", onBack = onBack)
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("¿Para quién es?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; selectedId = null },
                placeholder = { Text("Nombre del cliente") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (matches.isNotEmpty()) {
                LazyColumn(Modifier.heightIn(max = 160.dp).padding(top = 4.dp)) {
                    items(matches) { c ->
                        Text(
                            c.name, style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceDark)
                                .clickable { name = c.name; selectedId = c.id }.padding(12.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("¿Qué se llevó? (opcional)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = itemsText, onValueChange = { itemsText = it },
                placeholder = { Text("Ej. Arroz, leche") }, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Text("¿Cuánto?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            AmountField(value = amount, onValueChange = { amount = it })
            Spacer(Modifier.weight(1f))
            PrimaryButton("Guardar fiado", enabled = name.isNotBlank() && amount > 0) {
                if (selectedId != null) {
                    vm.registerFiado(selectedId!!, itemsText, amount, onSaved)
                } else {
                    vm.addClient(name, "") { id -> vm.registerFiado(id, itemsText, amount, onSaved) }
                }
            }
        }
    }
}
