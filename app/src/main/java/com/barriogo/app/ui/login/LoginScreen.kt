package com.barriogo.app.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.R
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.BarrioLogo
import com.barriogo.app.ui.components.FigmaInput
import com.barriogo.app.ui.components.FigmaInputColors
import com.barriogo.app.ui.theme.BarrioColors

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

/** L2 · Ingreso con correo (Figma "Pantalla · L2 Ingreso", Login=Normal). */
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
    val texto = FigmaInputColors.texto

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .navigationBarsPadding()
            .imePadding()
            .testTag("login_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 56.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section
            Column(
                Modifier.fillMaxWidth().padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                BarrioLogo(
                    Modifier.testTag("barriogo_logo"),
                    iconWidth = 104.dp, iconHeight = 100.dp, fontSize = 35.sp, gap = 7.dp,
                )
                Text(
                    "Ingresa a tu tienda",
                    color = texto,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp).testTag("login_title")
                )
            }

            if (state.noConnection) {
                NoConnectionNotice(Modifier.testTag("login_no_connection"))
                Spacer(Modifier.height(16.dp))
            }

            // Form Section
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Campo · L2 Correo
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Correo electrónico",
                        color = if (state.email.isEmpty()) FigmaInputColors.tenue else texto,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp)
                    )
                    FigmaInput(
                        value = state.email,
                        onValueChange = onEmailChange,
                        placeholder = "tu@correo.com",
                        leading = { Icon(painterResource(R.drawable.fig_correo), null, tint = texto) },
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        onNext = { focus.moveFocus(FocusDirection.Down) },
                        error = state.emailError,
                        modifier = Modifier.testTag("email_input")
                    )
                }

                // Campo · L2 Contraseña
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Contraseña", color = texto, fontSize = 14.sp, lineHeight = 20.sp)
                        Text(
                            "Olvidé mi contraseña",
                            color = Color(0xFFF2C849),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier
                                .clickable(onClick = onForgotPassword)
                                .padding(start = 8.dp, top = 7.5.dp, bottom = 8.5.dp)
                                .testTag("forgot_password_link")
                        )
                    }
                    FigmaInput(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        placeholder = "••••••••",
                        leading = { Icon(painterResource(R.drawable.fig_candado), null, tint = texto, modifier = Modifier.padding(start = 2.dp)) },
                        trailing = {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onTogglePassword)
                                    .testTag("password_visibility_toggle"),
                                contentAlignment = Alignment.Center,
                            ) {
                                val descripcion = if (state.passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                if (state.passwordVisible) {
                                    Icon(Icons.Outlined.Visibility, descripcion, tint = texto, modifier = Modifier.size(22.dp))
                                } else {
                                    Icon(painterResource(R.drawable.fig_ojo_tachado), descripcion, tint = texto)
                                }
                            }
                        },
                        visualTransformation =
                            if (state.passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        onDone = { focus.clearFocus(); onLogin() },
                        error = state.passwordError,
                        highlightBorder = state.credentialsError,
                        modifier = Modifier.testTag("password_input")
                    )
                }

                // Submit Button (pt 16)
                Box(
                    Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(7.dp, RoundedCornerShape(12.dp), ambientColor = Color(0x33F2C849), spotColor = Color(0x33F2C849))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF2C849))
                        .clickable(enabled = !state.isLoading) { focus.clearFocus(); onLogin() }
                        .testTag("login_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = BarrioColors.OnYellow,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                if (state.noConnection) "Reintentar" else "Ingresar",
                                color = BarrioColors.OnYellow,
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(painterResource(R.drawable.fig_flecha_derecha), null, tint = BarrioColors.OnYellow)
                        }
                    }
                }
            }

            // Footer Section (pt 40)
            Row(
                Modifier.padding(top = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("¿No tienes cuenta?", color = texto, fontSize = 16.sp, lineHeight = 24.sp)
                Text(
                    "Registra tu tienda",
                    color = if (BarrioColors.isDark) Color(0xFFF2C849) else BarrioColors.Gold,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(onClick = onRegister)
                        .testTag("register_link")
                )
            }
        }

        // Botón Apariencia: círculo de 44 a 24 del borde derecho y 48 del borde superior
        val context = androidx.compose.ui.platform.LocalContext.current
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 24.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(if (BarrioColors.isDark) Color(0xFF22593F) else Color(0xFFFFFDF9))
                .clickable {
                    val app = context.applicationContext as? com.barriogo.app.BarrioGoApp
                    app?.let {
                        val curr = it.container.store.darkTheme.value
                        it.container.store.setDark(!curr)
                    }
                    onToggleTheme()
                }
                .testTag("theme_toggle_button"),
            contentAlignment = Alignment.Center,
        ) {
            val descripcion = if (BarrioColors.isDark) "Cambiar a modo claro" else "Cambiar a modo oscuro"
            if (BarrioColors.isDark) {
                Icon(painterResource(R.drawable.fig_sol), descripcion, tint = Color.Unspecified)
            } else {
                Icon(Icons.Filled.DarkMode, descripcion, tint = BarrioColors.Gold, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Ámbar de las alertas del Figma (#F2B544); en tema claro se usa el rojo de error por contraste. */
@Composable
private fun loginWarningColor(): Color =
    if (BarrioColors.isDark) Color(0xFFF2B544) else BarrioColors.Error

/** Figma L2 · Aviso · Sin conexión. */
@Composable
private fun NoConnectionNotice(modifier: Modifier = Modifier) {
    val warning = loginWarningColor()
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (BarrioColors.isDark) Color(0xFF22593F) else BarrioColors.Surface)
            .border(1.dp, warning, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "Sin conexión a internet",
            color = warning,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Necesitas conexión solo para este primer ingreso. Revisa tu red e inténtalo de nuevo.",
            color = if (BarrioColors.isDark) Color(0xFFD9E0DB) else BarrioColors.TextHint,
            fontSize = 14.sp
        )
    }
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
