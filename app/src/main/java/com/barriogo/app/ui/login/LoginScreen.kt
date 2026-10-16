package com.barriogo.app.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.barriogo.app.R
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.theme.BarrioColors

import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.*
import androidx.compose.ui.zIndex

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onForgotPassword: () -> Unit = {},
    onRegister: () -> Unit,
    onToggleTheme: () -> Unit = {},
    viewModel: LoginViewModel = barrioViewModel { c -> LoginViewModel { e, p -> c.authenticateStore(e, p) } }
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.loginSuccess) {
        if (state.loginSuccess) {
            viewModel.onLoginHandled()
            onLoginSuccess()
        }
    }

    LoginContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePassword = viewModel::togglePasswordVisibility,
        onLogin = viewModel::onLoginClick,
        onForgotPassword = onForgotPassword,
        onRegister = onRegister,
        onToggleTheme = onToggleTheme
    )
}

@Composable
fun LoginContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
    onToggleTheme: () -> Unit
) {
    val focus = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .systemBarsPadding()
            .imePadding()
            .testTag("login_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            val logoRes = if (BarrioColors.isDark) R.drawable.logo_barriogo else R.drawable.logo_barriogo_light
            Image(
                painter = painterResource(logoRes),
                contentDescription = "BarrioGo",
                modifier = Modifier
                    .width(160.dp)
                    .testTag("barriogo_logo")
            )

            Spacer(Modifier.height(32.dp))

            Text(
                "Ingresa a tu tienda",
                color = BarrioColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("login_title")
            )

            Spacer(Modifier.height(40.dp))

            // Correo
            Text(
                "Correo electrónico",
                color = BarrioColors.TextHint,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            BarrioTextField(
                value = state.email,
                onValueChange = onEmailChange,
                placeholder = "tu@correo.com",
                leadingIcon = { Icon(Icons.Outlined.Email, null, tint = BarrioColors.TextPrimary) },
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                onNext = { focus.moveFocus(FocusDirection.Down) },
                error = state.emailError,
                modifier = Modifier.testTag("email_input")
            )

            Spacer(Modifier.height(24.dp))

            // Contraseña
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Contraseña", color = BarrioColors.TextPrimary, fontSize = 14.sp)
                Text(
                    "Olvidé mi contraseña",
                    color = BarrioColors.Yellow,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable(onClick = onForgotPassword)
                        .testTag("forgot_password_link")
                )
            }
            Spacer(Modifier.height(8.dp))
            BarrioTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                placeholder = "••••••••",
                leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = BarrioColors.TextPrimary) },
                trailingIcon = {
                    IconButton(
                        onClick = onTogglePassword,
                        modifier = Modifier.testTag("password_visibility_toggle")
                    ) {
                        Icon(
                            if (state.passwordVisible) Icons.Outlined.Visibility
                            else Icons.Outlined.VisibilityOff,
                            contentDescription = if (state.passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = BarrioColors.TextPrimary
                        )
                    }
                },
                visualTransformation =
                    if (state.passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onDone = { focus.clearFocus(); onLogin() },
                error = state.passwordError,
                modifier = Modifier.testTag("password_input")
            )

            Spacer(Modifier.height(32.dp))

            // Botón Ingresar
            Button(
                onClick = { focus.clearFocus(); onLogin() },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("login_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BarrioColors.Yellow,
                    contentColor = BarrioColors.OnYellow,
                    disabledContainerColor = BarrioColors.Yellow.copy(alpha = 0.6f),
                    disabledContentColor = BarrioColors.OnYellow
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = BarrioColors.OnYellow,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Ingresar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¿No tienes cuenta?", color = BarrioColors.TextPrimary, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Registra tu tienda",
                    color = BarrioColors.Yellow,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(onClick = onRegister)
                        .testTag("register_link")
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        // Botón de tema: posicionado al final del Box para garantizar prioridad visual y táctil
        val context = androidx.compose.ui.platform.LocalContext.current
        IconButton(
            onClick = {
                val app = context.applicationContext as? com.barriogo.app.BarrioGoApp
                app?.let {
                    val curr = it.container.store.darkTheme.value
                    it.container.store.setDark(!curr)
                }
                onToggleTheme()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 20.dp)
                .size(48.dp)
                .background(BarrioColors.Surface, CircleShape)
                .testTag("theme_toggle_button")
        ) {
            Icon(
                imageVector = if (BarrioColors.isDark) Icons.Filled.WbSunny else Icons.Filled.DarkMode,
                contentDescription = if (BarrioColors.isDark) "Cambiar a modo claro" else "Cambiar a modo oscuro",
                tint = BarrioColors.Yellow,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun BarrioTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable () -> Unit,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onNext: () -> Unit = {},
    onDone: () -> Unit = {},
    error: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        placeholder = { Text(placeholder, color = BarrioColors.TextHint, fontSize = 15.sp) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = true,
        isError = error != null,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onNext() }, onDone = { onDone() }),
        shape = RoundedCornerShape(14.dp),
        textStyle = TextStyle(color = BarrioColors.TextPrimary, fontSize = 15.sp),
        supportingText = error?.let { { Text(it, color = BarrioColors.Error, fontSize = 12.sp) } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = BarrioColors.Surface,
            unfocusedContainerColor = BarrioColors.Surface,
            errorContainerColor = BarrioColors.Surface,
            focusedBorderColor = BarrioColors.Yellow,
            unfocusedBorderColor = BarrioColors.YellowStroke,
            errorBorderColor = BarrioColors.Error,
            cursorColor = BarrioColors.Yellow,
            focusedTextColor = BarrioColors.TextPrimary,
            unfocusedTextColor = BarrioColors.TextPrimary
        )
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LoginPreview() {
    LoginContent(
        state = LoginUiState(),
        onEmailChange = {}, onPasswordChange = {}, onTogglePassword = {},
        onLogin = {}, onForgotPassword = {}, onRegister = {}, onToggleTheme = {}
    )
}
