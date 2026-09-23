package com.barriogo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.barriogo.app.ui.theme.Accent
import com.barriogo.app.ui.theme.OnAccent

enum class BottomDest(val route: String, val label: String) {
    INICIO("home", "Inicio"),
    MOVIMIENTOS("movements", "Movimientos"),
    REGISTRAR("register_movement", "Registrar"),
    CAJA("cash", "Caja"),
}

/** Barra inferior de 4 pestañas del prototipo. El botón "Registrar" va resaltado en amarillo. */
@Composable
fun BarrioBottomBar(current: BottomDest, onSelect: (BottomDest) -> Unit) {
    NavigationBar(containerColor = com.barriogo.app.ui.theme.SurfaceDark, tonalElevation = 0.dp) {
        BottomDest.values().forEach { dest ->
            val selected = dest == current
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(dest) },
                icon = {
                    if (dest == BottomDest.REGISTRAR) {
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(Accent),
                            contentAlignment = androidx.compose.ui.Alignment.Center,
                        ) { Icon(Icons.Filled.Add, contentDescription = dest.label, tint = OnAccent) }
                    } else {
                        Icon(iconFor(dest), contentDescription = dest.label)
                    }
                },
                label = { Text(dest.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Accent, selectedTextColor = Accent,
                    unselectedIconColor = com.barriogo.app.ui.theme.TextMuted,
                    unselectedTextColor = com.barriogo.app.ui.theme.TextMuted,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}

private fun iconFor(dest: BottomDest) = when (dest) {
    BottomDest.INICIO -> Icons.Filled.Home
    BottomDest.MOVIMIENTOS -> Icons.Filled.List
    BottomDest.CAJA -> Icons.Filled.Payments
    BottomDest.REGISTRAR -> Icons.Filled.Add
}
