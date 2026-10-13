package com.barriogo.app.ui.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.barriogo.app.R
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.AuthScaffold
import com.barriogo.app.ui.components.BarrioLogo
import com.barriogo.app.ui.components.BarrioTextField
import com.barriogo.app.ui.components.InfoBanner
import com.barriogo.app.ui.components.InlineLink
import com.barriogo.app.ui.components.PasswordTextField
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.components.ScreenTitle
import com.barriogo.app.ui.components.TextLinkButton
import com.barriogo.app.ui.theme.BarrioGoTheme
import com.barriogo.app.ui.theme.BarrioTheme

/** Aviso opcional sobre el formulario de L2. */
enum class LoginNotice(val title: String, val message: String) {
    /** L2 tras cerrar sesión. */
    SIGNED_OUT("Saliste de la tienda", "Todo quedó guardado. Cuando quieras volver, entra con tu correo y tu contraseña."),
    /** Después de RP3. */
    PASSWORD_RESET("Contraseña actualizada", "Ya puedes entrar con tu correo y tu nueva contraseña."),
}

/** L2 · Ingreso con correo y contraseña de la tienda. */
@Composable
fun LoginScreen(
    notice: LoginNotice?,
    onLoggedIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
) {
    val vm = barrioViewModel { LoginViewModel(it) }
    val state by vm.state.collectAsState()
    LoginContent(
        state = state,
        notice = notice,
        onEmailChange = vm::onEmailChange,
        onPasswordChange = vm::onPasswordChange,
        onSubmit = { vm.submit(onLoggedIn) },
        onForgotPassword = onForgotPassword,
        onRegister = onRegister,
    )
}

@Composable
private fun LoginContent(
    state: LoginUiState,
    notice: LoginNotice?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
) {
    val focus = LocalFocusManager.current
    val spacing = BarrioTheme.spacing
    AuthScaffold(horizontalAlignment = Alignment.CenterHorizontally) {
        // El Figma mide 56 dp desde el borde, incluida la barra de estado (≈24 dp).
        Spacer(Modifier.height(spacing.xxl))
        BarrioLogo()
        Spacer(Modifier.height(spacing.lg + spacing.xs))
        ScreenTitle("Ingresa a tu tienda", textAlign = TextAlign.Center)
        Spacer(Modifier.height(spacing.huge))

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            if (notice != null) InfoBanner(title = notice.title, message = notice.message)
            BarrioTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = "Correo electrónico",
                placeholder = "tu@correo.com",
                leadingIcon = painterResource(R.drawable.ic_mail),
                errorText = state.emailError,
                keyboardType = KeyboardType.Email,
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
            )
            PasswordTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = "Contraseña",
                labelAction = { InlineLink("Olvidé mi contraseña", onClick = onForgotPassword) },
                errorText = state.passwordError,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); onSubmit() }),
            )
            Spacer(Modifier.height(spacing.xxs))
            PrimaryButton(
                text = "Ingresar",
                trailingIcon = painterResource(R.drawable.ic_arrow_forward),
                onClick = { focus.clearFocus(); onSubmit() },
            )
        }

        Spacer(Modifier.height(spacing.xxl))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("¿No tienes cuenta?", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            TextLinkButton("Registra tu tienda", onClick = onRegister)
        }
        Spacer(Modifier.height(spacing.xl))
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun LoginPreview() {
    BarrioGoTheme(darkTheme = true) {
        LoginContent(LoginUiState(), notice = LoginNotice.SIGNED_OUT, {}, {}, {}, {}, {})
    }
}
