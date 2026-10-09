package com.barriogo.app.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.barriogo.app.BarrioGoApp
import com.barriogo.app.ui.login.LoginViewModel
import com.barriogo.app.ui.login.LoginScreen as NewLoginScreen

/** L2 · Ingreso con correo y contraseña de la tienda (pantalla con logo oficial). */
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onGoRegister: () -> Unit,
    onToggleTheme: () -> Unit = {},
) {
    val app = LocalContext.current.applicationContext as BarrioGoApp
    val vm: LoginViewModel = viewModel(
        factory = remember {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LoginViewModel(authenticate = { email, pass ->
                        app.container.authenticateStore(email, pass)
                    }) as T
                }
            }
        }
    )

    NewLoginScreen(
        onLoginSuccess = onLoggedIn,
        onForgotPassword = { /* Recuperar clave */ },
        onRegister = onGoRegister,
        onToggleTheme = onToggleTheme,
        viewModel = vm,
    )
}

