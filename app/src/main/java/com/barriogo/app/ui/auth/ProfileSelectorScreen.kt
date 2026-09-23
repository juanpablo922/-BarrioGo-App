package com.barriogo.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.barriogo.app.data.ProfileEntity
import com.barriogo.app.domain.Role
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.theme.Accent
import com.barriogo.app.ui.theme.SurfaceDark

/** L3 · "¿Quién está atendiendo?": elegir con qué perfil se entra a la tienda. */
@Composable
fun ProfileSelectorScreen(onPick: (Long) -> Unit, onCreateProfile: () -> Unit) {
    val vm = barrioViewModel { AuthViewModel(it) }
    val profiles by vm.profiles.collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("¿Quién está atendiendo?", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Elige tu perfil para continuar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(profiles) { p -> ProfileRow(p) { onPick(p.id) } }
            item {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceDark)
                        .clickable(onClick = onCreateProfile).padding(16.dp),
                ) {
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = Accent)
                        Text(
                            "Crear nuevo perfil",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(p: ProfileEntity, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceDark)
            .clickable(onClick = onClick).padding(14.dp),
    ) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(Accent),
                contentAlignment = Alignment.Center,
            ) { Text(p.initial, color = MaterialTheme.colorScheme.background, style = MaterialTheme.typography.titleMedium) }
            Column(Modifier.padding(start = 12.dp)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (p.role == Role.ADMIN.name) "Administrador" else "Vendedor",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
