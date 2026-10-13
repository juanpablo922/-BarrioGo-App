package com.barriogo.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import com.barriogo.app.domain.PasswordPolicy
import com.barriogo.app.domain.PinPolicy
import com.barriogo.app.ui.components.FieldVariant
import com.barriogo.app.ui.components.NumericKeypad
import com.barriogo.app.ui.components.PasswordTextField
import com.barriogo.app.ui.components.PinBoxes
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.components.RegisterStepper
import com.barriogo.app.ui.components.RequirementRow
import com.barriogo.app.ui.components.ScreenTitle
import com.barriogo.app.ui.theme.BarrioTheme

/** Stepper + título + subtítulo con los márgenes de R1/R3/RP3. */
@Composable
fun ColumnScope.StepHeader(step: Int, title: String, subtitle: String, centered: Boolean = false) {
    val spacing = BarrioTheme.spacing
    Spacer(Modifier.height(spacing.md))
    RegisterStepper(current = step)
    Spacer(Modifier.height(spacing.xxl))
    val align = if (centered) TextAlign.Center else TextAlign.Start
    ScreenTitle(
        title,
        textAlign = align,
        modifier = Modifier.fillMaxWidth().align(if (centered) Alignment.CenterHorizontally else Alignment.Start),
    )
    Spacer(Modifier.height(spacing.xs))
    Text(
        subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = align,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(spacing.xxxl))
}

/**
 * Formulario de contraseña nueva con requisitos en vivo. Lo usan R3 (registro) y RP3 (recuperación).
 */
@Composable
fun NewPasswordForm(
    password: String,
    confirm: String,
    passwordError: String?,
    confirmError: String?,
    buttonText: String,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val focus = LocalFocusManager.current
    val spacing = BarrioTheme.spacing
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        PasswordTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Nueva contraseña",
            placeholder = "Ingresa tu nueva contraseña",
            variant = FieldVariant.Outline,
            errorText = passwordError,
            imeAction = ImeAction.Next,
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
        )
        PasswordTextField(
            value = confirm,
            onValueChange = onConfirmChange,
            label = "Confirmar contraseña",
            placeholder = "Vuelve a ingresar la contraseña",
            variant = FieldVariant.Outline,
            errorText = confirmError,
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); onSubmit() }),
            modifier = Modifier.padding(bottom = spacing.md),
        )
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            PasswordPolicy.checks(password).forEach { RequirementRow(it.label, it.met) }
        }
        Spacer(Modifier.height(spacing.xl))
        PrimaryButton(buttonText, onClick = { focus.clearFocus(); onSubmit() })
    }
}

/**
 * Creación de la clave de 4 números con teclado propio (R5 y CP3): casillas, teclado, botón de
 * guardar (se activa al completar los 4 números) y ayuda dorada debajo.
 */
@Composable
fun PinCreationContent(
    pin: String,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    buttonText: String,
    onSave: () -> Unit,
    error: String? = null,
) {
    val spacing = BarrioTheme.spacing
    val complete = pin.length == PinPolicy.LENGTH
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        PinBoxes(length = PinPolicy.LENGTH, filled = pin.length, isError = error != null)
        NumericKeypad(onDigit = onDigit, onBackspace = onBackspace, enabled = true)
        PrimaryButton(buttonText, enabled = complete, onClick = onSave, modifier = Modifier.padding(horizontal = spacing.xs))
        Text(
            error ?: if (complete) "Listo, guarda tu clave" else "Ingresa ${PinPolicy.LENGTH} números para continuar",
            style = MaterialTheme.typography.bodyMedium,
            color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )
    }
}
