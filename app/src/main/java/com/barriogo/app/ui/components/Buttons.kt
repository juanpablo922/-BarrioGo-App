package com.barriogo.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.barriogo.app.ui.theme.BarrioTheme
import com.barriogo.app.ui.theme.Danger

/**
 * Botón principal (Figma "Botón Principal"): amarillo, 56 dp, radio 12 y brillo amarillo debajo.
 * Deshabilitado se ve dorado apagado, igual que el prototipo, y no recibe toques.
 */
@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    trailingIcon: Painter? = null,
    onClick: () -> Unit,
) {
    val colors = BarrioTheme.colors
    val shape = MaterialTheme.shapes.medium
    val active = enabled && !loading
    Button(
        onClick = onClick,
        enabled = active,
        modifier = modifier
            .fillMaxWidth()
            .height(BarrioTheme.sizes.buttonHeight)
            .then(
                if (active) Modifier.shadow(6.dp, shape, ambientColor = colors.primaryGlow, spotColor = colors.primaryGlow)
                else Modifier,
            ),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = if (loading) MaterialTheme.colorScheme.primary else colors.disabledContainer,
            disabledContentColor = if (loading) MaterialTheme.colorScheme.onPrimary else colors.disabledContent,
        ),
        elevation = null,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
            if (trailingIcon != null) {
                Spacer(Modifier.width(BarrioTheme.spacing.xs))
                Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/** Botón secundario con borde dorado ("Usar mi clave"). Para acciones alternativas a la principal. */
@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(BarrioTheme.sizes.buttonHeight),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        border = BorderStroke(BarrioTheme.sizes.borderThin, MaterialTheme.colorScheme.outline),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/**
 * Enlace de texto ("Olvidé mi clave", "Ahora no, usar solo mi clave"). Usa TextButton de Material
 * para tener un área táctil de 48 dp aunque el texto sea pequeño.
 */
@Composable
fun TextLinkButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    underline: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = BarrioTheme.sizes.textButtonHeight)
            .semantics { role = Role.Button },
        colors = ButtonDefaults.textButtonColors(contentColor = color),
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            textDecoration = if (underline) TextDecoration.Underline else null,
        )
    }
}

/** Enlace pequeño en línea (14 sp regular) para usar junto a etiquetas, p. ej. "Olvidé mi contraseña". */
@Composable
fun InlineLink(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 36.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = BarrioTheme.spacing.xs),
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        shape = MaterialTheme.shapes.extraSmall,
    ) { Text(text, style = MaterialTheme.typography.bodyMedium) }
}

@Composable
fun DangerButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(BarrioTheme.sizes.buttonHeight),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = Color.White),
    ) { Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 4.dp)) }
}
