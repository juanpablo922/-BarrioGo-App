package com.barriogo.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.BarrioGoApp
import androidx.compose.runtime.rememberCoroutineScope
import com.barriogo.app.data.perfiles.PerfilesRepository
import kotlinx.coroutines.launch
import com.barriogo.app.data.ProfileEntity
import com.barriogo.app.ui.components.NumericKeypad
import com.barriogo.app.ui.components.PinSquares
import com.barriogo.app.ui.theme.BarrioColors

/**
 * L4/L6 · PIN de un perfil concreto (Screen 10 del prototipo).
 */
@Composable
fun PinLoginScreen(profileId: Long, onSuccess: () -> Unit, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as BarrioGoApp
    val container = app.container
    var profile by remember { mutableStateOf<ProfileEntity?>(null) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showForgotPinDialog by remember { mutableStateOf(false) }

    val alcance = rememberCoroutineScope()
    val context = LocalContext.current
    var verificando by remember { mutableStateOf(false) }

    LaunchedEffect(profileId) {
        // Un perfil que no existe no tiene clave: se vuelve al selector.
        profile = container.repo.profile(profileId) ?: return@LaunchedEffect onBack()
    }

    fun entrar() {
        container.activeProfileId.value = profileId
        profile?.takeIf { it.isAdmin }?.remoteId?.let(container::recordarAdmin)
        onSuccess()
    }

    /** L4 · Compara con el hash guardado (PBKDF2), sin internet. */
    fun tryPin(value: String) {
        val p = profile ?: return
        if (verificando) return
        verificando = true
        alcance.launch {
            if (PerfilesRepository.claveCorrecta(p, value)) {
                entrar()
            } else {
                error = "Clave incorrecta. Intenta de nuevo."
                pin = ""
            }
            verificando = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .systemBarsPadding()
            .testTag("pin_login_screen")
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(16.dp))

                // Top Bar: Back arrow
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("pin_back_arrow")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = BarrioColors.Yellow,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Profile Avatar Circle
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(BarrioColors.Surface)
                        .border(1.5.dp, BarrioColors.YellowStroke, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        profile?.initial ?: "",
                        color = BarrioColors.TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    profile?.name.orEmpty(),
                    color = BarrioColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    "Ingresa tu clave de 4 números",
                    color = BarrioColors.TextHint,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(30.dp))

                PinSquares(
                    length = 4,
                    filled = pin.length,
                    modifier = Modifier.testTag("pin_input_squares")
                )

                if (error != null) {
                    Text(
                        error!!,
                        color = BarrioColors.Error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NumericKeypad(
                    onDigit = { d ->
                        if (pin.length < 4) {
                            pin += d
                            if (pin.length == 4) {
                                tryPin(pin)
                            }
                        }
                    },
                    onBackspace = {
                        if (pin.isNotEmpty()) pin = pin.dropLast(1)
                    },
                    leftActionIcon = if (profile?.biometric == true && Huella.disponible(context)) {
                        {
                        // Huella real: BiometricPrompt confirma antes de entrar.
                        IconButton(onClick = {
                            Huella.pedir(
                                context,
                                titulo = "Entrar como ${profile?.name.orEmpty()}",
                                subtitulo = "Apoya el dedo en el lector",
                                onOk = { entrar() },
                                onError = { error = it },
                            )
                        }) {
                            Icon(
                                Icons.Outlined.Fingerprint,
                                contentDescription = "Usar huella",
                                tint = BarrioColors.Yellow,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        }
                    } else null,
                    modifier = Modifier.testTag("pin_keypad")
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    "Olvidé mi clave",
                    color = BarrioColors.Yellow,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { showForgotPinDialog = true }
                        .padding(bottom = 24.dp)
                        .testTag("forgot_pin_button")
                )
            }
        }

        if (showForgotPinDialog) {
            AlertDialog(
                onDismissRequest = { showForgotPinDialog = false },
                containerColor = BarrioColors.Background,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                title = { Text("¿Olvidaste tu clave?", color = BarrioColors.TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Puedes entrar usando la contraseña general de la tienda o pedirle al Administrador que actualice tu PIN.",
                        color = BarrioColors.TextHint,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showForgotPinDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = BarrioColors.Yellow, contentColor = BarrioColors.OnYellow)
                    ) {
                        Text("Entendido", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
