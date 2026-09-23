package com.barriogo.app.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.ui.theme.Accent
import kotlinx.coroutines.delay

/** Pantalla de arranque: muestra la marca y decide a dónde ir según lo guardado en el celular. */
@Composable
fun SplashScreen(
    isRegistered: Boolean,
    isSignedIn: Boolean,
    hasProfiles: Boolean,
    onGoLogin: () -> Unit,
    onGoRegister: () -> Unit,
    onGoProfileSelector: () -> Unit,
) {
    LaunchedEffect(Unit) {
        delay(900)
        when {
            !isRegistered -> onGoRegister()
            !isSignedIn -> onGoLogin()
            hasProfiles -> onGoProfileSelector()
            else -> onGoLogin()
        }
    }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Storefront, contentDescription = null, tint = Accent, modifier = Modifier.padding(bottom = 12.dp))
        Text("BARRIO", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp))
        Text("GO", color = Accent, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp))
        Text(
            "Tu tienda al día. Sin dejar de atender.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
