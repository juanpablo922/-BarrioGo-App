package com.barriogo.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.barriogo.app.AppContainer
import com.barriogo.app.BarrioGoApp
import com.barriogo.app.data.Hasher
import com.barriogo.app.data.ProfileEntity
import com.barriogo.app.ui.components.NumericKeypad
import com.barriogo.app.ui.components.PinDots
import com.barriogo.app.ui.theme.Accent
import com.barriogo.app.ui.theme.SurfaceDark
import kotlinx.coroutines.delay

private const val MAX_ATTEMPTS = 3
private const val LOCK_MS = 30_000L

/**
 * L4/L6 · PIN de un perfil concreto. Si falla 3 veces, bloquea 30 s (regla de la guía original del
 * prototipo). Ofrece huella si el perfil la activó.
 */
@Composable
fun PinLoginScreen(profileId: Long, onSuccess: () -> Unit, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as BarrioGoApp
    val container = app.container
    var profile by remember { mutableStateOf<ProfileEntity?>(null) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var lockRemaining by remember { mutableLongStateOf(0L) }

    LaunchedEffect(profileId) { profile = container.repo.profile(profileId) }

    LaunchedEffect(profileId) {
        while (true) {
            val until = container.store.lockUntil(profileId)
            val rem = until - System.currentTimeMillis()
            lockRemaining = if (rem > 0) rem else 0
            delay(500)
        }
    }

    fun tryPin(value: String) {
        val p = profile ?: return
        if (Hasher.matches(p.pinSalt, value, p.pinHash)) {
            container.store.setFailedAttempts(profileId, 0)
            container.activeProfileId.value = profileId
            onSuccess()
        } else {
            val attempts = container.store.failedAttempts(profileId) + 1
            container.store.setFailedAttempts(profileId, attempts)
            if (attempts >= MAX_ATTEMPTS) {
                container.store.setLockUntil(profileId, System.currentTimeMillis() + LOCK_MS)
                container.store.setFailedAttempts(profileId, 0)
                error = "Demasiados intentos. Espera 30 segundos."
            } else {
                error = "PIN incorrecto (intento $attempts de $MAX_ATTEMPTS)"
            }
            pin = ""
        }
    }

    val locked = lockRemaining > 0

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(SurfaceDark), contentAlignment = Alignment.Center) {
                Text(profile?.initial ?: "?", style = MaterialTheme.typography.headlineMedium)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(profile?.name ?: "", style = MaterialTheme.typography.titleLarge)
        Text(
            if (locked) "Bloqueado por ${lockRemaining / 1000 + 1} s" else "Ingresa tu PIN",
            style = MaterialTheme.typography.bodyMedium,
            color = if (locked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
        )
        PinDots(length = 4, filled = pin.length)
        if (error != null && !locked) {
            Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        }
        Spacer(Modifier.height(32.dp))
        if (!locked) {
            NumericKeypad(
                onDigit = { d ->
                    if (pin.length < 4) {
                        pin += d
                        if (pin.length == 4) tryPin(pin)
                    }
                },
                onBackspace = { pin = pin.dropLast(1) },
            )
        }
        if (profile?.biometric == true && !locked) {
            Spacer(Modifier.height(16.dp))
            IconButton(onClick = { /* BiometricPrompt: ver BiometricHelper en el módulo auth */ }) {
                Icon(Icons.Filled.Fingerprint, contentDescription = "Usar huella", tint = Accent, modifier = Modifier.size(36.dp))
            }
        }
    }
}
