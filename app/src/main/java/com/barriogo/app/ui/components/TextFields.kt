package com.barriogo.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.barriogo.app.R
import com.barriogo.app.ui.theme.BarrioTheme

/**
 * Las tres variantes de campo que usa el Figma:
 * - [Subtle]: ingreso y recuperación (etiqueta gris 14, relleno verde al 50 %, espacio reservado para el error).
 * - [Solid]:  registro R1 y perfil R6 (etiqueta fuerte 16 SemiBold, relleno verde sólido).
 * - [Outline]: contraseñas nuevas R3/RP3 (etiqueta gris 14, sin relleno).
 */
enum class FieldVariant { Subtle, Solid, Outline }

/**
 * Campo de texto del sistema de diseño: etiqueta encima, campo de Material 3 de 56 dp con borde
 * dorado (amarillo al enfocar, ámbar con error) y mensaje de validación debajo.
 */
@Composable
fun BarrioTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    variant: FieldVariant = FieldVariant.Subtle,
    leadingIcon: Painter? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    labelAction: (@Composable RowScope.() -> Unit)? = null,
    errorText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
) {
    val colors = BarrioTheme.colors
    val spacing = BarrioTheme.spacing
    val labelStyle = if (variant == FieldVariant.Solid) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium
    val labelColor = when {
        variant == FieldVariant.Solid -> MaterialTheme.colorScheme.onSurface
        labelAction != null -> MaterialTheme.colorScheme.onSurface
        else -> colors.textMuted
    }
    val container = when (variant) {
        FieldVariant.Subtle -> colors.fieldContainerSubtle
        FieldVariant.Solid -> colors.fieldContainer
        FieldVariant.Outline -> Color.Transparent
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Row(
            Modifier.fillMaxWidth().padding(start = if (variant == FieldVariant.Subtle) spacing.xxs else 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = labelStyle, color = labelColor)
            labelAction?.invoke(this)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = BarrioTheme.sizes.fieldHeight)
                .semantics {
                    contentDescription = label
                    if (errorText != null) error(errorText)
                },
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.textMuted) },
            leadingIcon = leadingIcon?.let { painter -> { Icon(painter, contentDescription = null) } },
            trailingIcon = trailingIcon,
            isError = errorText != null,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = keyboardActions,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = container,
                unfocusedContainerColor = container,
                errorContainerColor = container,
                disabledContainerColor = container,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                cursorColor = MaterialTheme.colorScheme.primary,
                errorCursorColor = MaterialTheme.colorScheme.error,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                errorTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLeadingIconColor = MaterialTheme.colorScheme.onSurface,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurface,
                errorLeadingIconColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
        // El Figma reserva el alto del mensaje en los campos de ingreso para que el formulario no salte.
        if (errorText != null || variant == FieldVariant.Subtle) {
            Text(
                errorText ?: " ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** Campo de contraseña con el botón de ojo para mostrar u ocultar lo escrito. */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "••••••••",
    variant: FieldVariant = FieldVariant.Subtle,
    showLockIcon: Boolean = variant == FieldVariant.Subtle,
    labelAction: (@Composable RowScope.() -> Unit)? = null,
    errorText: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    val toggleTint = if (variant == FieldVariant.Outline) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
    BarrioTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        placeholder = placeholder,
        variant = variant,
        leadingIcon = if (showLockIcon) painterResource(R.drawable.ic_lock) else null,
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    painterResource(if (visible) R.drawable.ic_eye else R.drawable.ic_eye_off),
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                    tint = toggleTint,
                )
            }
        },
        labelAction = labelAction,
        errorText = errorText,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        keyboardActions = keyboardActions,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
    )
}
