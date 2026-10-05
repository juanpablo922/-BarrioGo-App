package com.barriogo.app.ui.auth

import androidx.compose.runtime.Composable
import com.barriogo.app.ui.login.LoginScreen as NewLoginScreen

/** L2 · Ingreso con correo y contraseña de la tienda (pantalla con logo oficial). */
@Composable
fun LoginScreen(onLoggedIn: () -> Unit, onGoRegister: () -> Unit) {
    NewLoginScreen(
        onLoginSuccess = onLoggedIn,
        onForgotPassword = { /* Recuperar clave */ },
        onRegister = onGoRegister,
    )
}

