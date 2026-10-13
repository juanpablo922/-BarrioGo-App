package com.barriogo.app.ui.auth.recovery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.barriogo.app.R
import com.barriogo.app.data.RecoveryCodeManager
import com.barriogo.app.ui.auth.NewPasswordForm
import com.barriogo.app.ui.auth.PinCreationContent
import com.barriogo.app.ui.auth.StepHeader
import com.barriogo.app.ui.components.AuthScaffold
import com.barriogo.app.ui.components.BackButton
import com.barriogo.app.ui.components.BarrioLogo
import com.barriogo.app.ui.components.BarrioTextField
import com.barriogo.app.ui.components.FlowTopBar
import com.barriogo.app.ui.components.OtpField
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.components.ScreenTitle
import com.barriogo.app.ui.components.TextLinkButton
import com.barriogo.app.ui.theme.BarrioTheme

/** RP1 / CP1 · Correo de la tienda para recibir el código. */
@Composable
fun RecoverEmailScreen(vm: RecoveryViewModel, onBack: () -> Unit, onCodeSent: () -> Unit) {
    val s by vm.state.collectAsState()
    val focus = LocalFocusManager.current
    val spacing = BarrioTheme.spacing
    val submit = { focus.clearFocus(); vm.sendCode(onCodeSent) }
    Box(Modifier.fillMaxSize()) {
        AuthScaffold(horizontalAlignment = Alignment.CenterHorizontally) {
            // El Figma mide 56 dp desde el borde, incluida la barra de estado (≈24 dp).
            Spacer(Modifier.height(spacing.xxl))
            BarrioLogo()
            Spacer(Modifier.height(spacing.lg + spacing.xs))
            ScreenTitle(vm.target.title, textAlign = TextAlign.Center)
            Spacer(Modifier.height(spacing.lg))
            Text(
                "Te enviaremos un código de ${RecoveryCodeManager.CODE_LENGTH} dígitos para que puedas crear una nueva clave.",
                style = BarrioTheme.textStyles.subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(spacing.huge))
            BarrioTextField(
                value = s.email,
                onValueChange = vm::onEmailChange,
                label = "Correo electrónico",
                placeholder = "tu@correo.com",
                leadingIcon = painterResource(R.drawable.ic_mail),
                errorText = s.emailError,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Send,
                keyboardActions = KeyboardActions(onSend = { submit() }),
            )
            Spacer(Modifier.height(spacing.md + spacing.xxs))
            PrimaryButton(
                "Enviar código",
                loading = s.busy,
                trailingIcon = painterResource(R.drawable.ic_arrow_forward),
                onClick = submit,
            )
            Spacer(Modifier.height(spacing.xxxl))
        }
        BackButton(onBack, Modifier.padding(start = spacing.lg, top = spacing.md))
    }
}

/** RP2 / CP2 · Código de 6 dígitos. */
@Composable
fun RecoverCodeScreen(vm: RecoveryViewModel, onBack: () -> Unit, onVerified: () -> Unit) {
    val s by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val spacing = BarrioTheme.spacing
    LaunchedEffect(s.demoCode) {
        s.demoCode?.let { snackbar.showSnackbar("Modo demostración: tu código es $it", duration = SnackbarDuration.Long) }
    }
    AuthScaffold(topBar = { FlowTopBar(vm.target.title, onBack) }, snackbarHostState = snackbar) {
        StepHeader(
            step = 2,
            title = "Ingresa el código",
            subtitle = "Enviamos un código de ${RecoveryCodeManager.CODE_LENGTH} dígitos a ${s.maskedEmail}.",
        )
        OtpField(
            value = s.code,
            onValueChange = vm::onCodeChange,
            isError = s.codeError != null,
            onDone = { vm.verifyCode(onVerified) },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Text(
            s.codeError ?: " ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = spacing.sm),
        )
        Spacer(Modifier.height(spacing.xxl))
        PrimaryButton(
            "Verificar código",
            enabled = s.code.length == RecoveryCodeManager.CODE_LENGTH,
            onClick = { vm.verifyCode(onVerified) },
        )
        Spacer(Modifier.height(spacing.md))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("¿No te llegó?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            TextLinkButton("Reenviar código", enabled = !s.busy, onClick = vm::resendCode)
        }
        Spacer(Modifier.height(spacing.xxxl))
    }
}

/** RP3 · Nueva contraseña de la tienda. */
@Composable
fun NewPasswordScreen(vm: RecoveryViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    val s by vm.state.collectAsState()
    AuthScaffold(topBar = { FlowTopBar(vm.target.title, onBack) }) {
        StepHeader(
            step = 3,
            title = "Crea tu nueva contraseña",
            subtitle = "Elige una clave nueva para volver a entrar a tu tienda.",
        )
        NewPasswordForm(
            password = s.password,
            confirm = s.confirmPassword,
            passwordError = s.passwordError,
            confirmError = s.confirmError,
            buttonText = "Guardar contraseña",
            onPasswordChange = vm::onPasswordChange,
            onConfirmChange = vm::onConfirmChange,
            onSubmit = { vm.savePassword(onSaved) },
        )
        Spacer(Modifier.height(BarrioTheme.spacing.xxxl))
    }
}

/** CP3 · Nueva clave de 4 números para el perfil. */
@Composable
fun NewPinScreen(vm: RecoveryViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    val s by vm.state.collectAsState()
    AuthScaffold(topBar = { FlowTopBar(vm.target.title, onBack) }) {
        StepHeader(
            step = 3,
            title = "Nueva clave de 4 números",
            subtitle = "Elige 4 números nuevos para entrar a tu perfil.",
        )
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PinCreationContent(
                pin = s.pin,
                onDigit = vm::pressDigit,
                onBackspace = vm::backspace,
                buttonText = "Guardar mi clave",
                onSave = { vm.savePin(onSaved) },
            )
        }
        Spacer(Modifier.height(BarrioTheme.spacing.xxxl))
    }
}
