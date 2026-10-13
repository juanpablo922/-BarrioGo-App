package com.barriogo.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.barriogo.app.R
import com.barriogo.app.ui.theme.BarrioTheme

/**
 * Stepper del registro (Figma "Stepper · Registro"): paso actual amarillo de 32 dp, completados en
 * verde menta con check y pendientes con borde dorado. [current] empieza en 1.
 */
@Composable
fun RegisterStepper(current: Int, modifier: Modifier = Modifier, total: Int = 3) {
    val sizes = BarrioTheme.sizes
    val colors = BarrioTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(sizes.stepCurrent)
            .semantics(mergeDescendants = true) { contentDescription = "Paso $current de $total" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (step in 1..total) {
            when {
                step < current -> Box(
                    Modifier.size(sizes.stepOther).background(colors.success, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(painterResource(R.drawable.ic_check_small), null, tint = colors.onSuccess, modifier = Modifier.size(14.dp)) }
                step == current -> Box(
                    Modifier.size(sizes.stepCurrent).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("$step", style = BarrioTheme.textStyles.stepNumber, color = MaterialTheme.colorScheme.onPrimary) }
                else -> Box(
                    Modifier.size(sizes.stepOther).border(sizes.borderStepper, MaterialTheme.colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "$step",
                        style = BarrioTheme.textStyles.stepNumber.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            if (step < total) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(sizes.stepConnector)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (step < current) colors.success else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                )
            }
        }
    }
}

/** Tarjeta de la grilla de categorías (R2): ícono + nombre, seleccionable como opción de radio. */
@Composable
fun CategoryCard(label: String, icon: Painter, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SelectableCard(selected, onClick, modifier.height(BarrioTheme.sizes.categoryCardHeight)) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = BarrioTheme.colors.heading, modifier = Modifier.padding(bottom = BarrioTheme.spacing.xs))
            Text(label, style = MaterialTheme.typography.labelLarge, color = BarrioTheme.colors.heading, textAlign = TextAlign.Center)
        }
    }
}

/** Tarjeta de rol (R6): avatar de color con ícono, título y descripción. */
@Composable
fun RoleCard(
    title: String,
    description: String,
    icon: Painter,
    avatarColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectableCard(selected, onClick, modifier.height(BarrioTheme.sizes.roleCardHeight)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = BarrioTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(BarrioTheme.sizes.avatarSmall).background(avatarColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.background) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = BarrioTheme.colors.heading)
                Text(description, style = MaterialTheme.typography.bodySmall, color = BarrioTheme.colors.cardDescription)
            }
        }
    }
}

@Composable
private fun SelectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val sizes = BarrioTheme.sizes
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = BarrioTheme.colors.fieldContainer,
        border = if (selected) BorderStroke(sizes.borderEmphasis, MaterialTheme.colorScheme.primary)
        else BorderStroke(sizes.borderThin, MaterialTheme.colorScheme.outline),
    ) { Box(contentAlignment = Alignment.Center) { content() } }
}

/**
 * Avatar circular con la inicial del perfil (L3, L4, L5). [isAdmin] agrega la insignia de corona
 * arriba a la derecha.
 */
@Composable
fun ProfileAvatar(
    initial: String,
    container: Color,
    content: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    isAdmin: Boolean = false,
    initialStyle: androidx.compose.ui.text.TextStyle = BarrioTheme.textStyles.avatarInitial,
) {
    Box(modifier.size(size)) {
        Box(
            Modifier
                .size(size)
                .shadow(4.dp, CircleShape)
                .background(container, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(initial, style = initialStyle, color = content) }
        if (isAdmin) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .background(MaterialTheme.colorScheme.background, CircleShape)
                    .border(BarrioTheme.sizes.borderEmphasis, MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(6.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_crown_badge),
                    contentDescription = "Administrador",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

/** Aviso con borde de color (L2 "Saliste de la tienda"). */
@Composable
fun InfoBanner(title: String, message: String, modifier: Modifier = Modifier) {
    val colors = BarrioTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.fieldContainer, MaterialTheme.shapes.medium)
            .border(BarrioTheme.sizes.borderThin, colors.success, MaterialTheme.shapes.medium)
            .padding(horizontal = BarrioTheme.spacing.md, vertical = BarrioTheme.spacing.sm)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(BarrioTheme.spacing.xxs),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = colors.success)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Requisito de contraseña (R3/RP3): círculo dorado vacío que pasa a verde con check al cumplirse. */
@Composable
fun RequirementRow(text: String, met: Boolean, modifier: Modifier = Modifier) {
    val colors = BarrioTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = "$text: ${if (met) "cumplido" else "pendiente"}" },
        horizontalArrangement = Arrangement.spacedBy(BarrioTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (met) {
            Box(Modifier.size(12.dp).background(colors.success, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_check_small), null, tint = colors.onSuccess, modifier = Modifier.size(10.dp))
            }
        } else {
            Icon(painterResource(R.drawable.ic_requirement), null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(12.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = if (met) colors.success else MaterialTheme.colorScheme.onSurface)
    }
}

/** Ilustración de éxito (R4): círculo amarillo con check, borde verde y brillo. */
@Composable
fun SuccessHero(modifier: Modifier = Modifier) {
    val sizes = BarrioTheme.sizes
    Box(modifier.size(sizes.successHero + 32.dp), contentAlignment = Alignment.Center) {
        // Puntos decorativos del Figma alrededor del círculo.
        Box(Modifier.align(Alignment.TopCenter).offset(x = (-18).dp, y = 16.dp).size(8.dp).background(BarrioTheme.colors.fieldContainer.copy(alpha = 0.75f), CircleShape))
        Box(Modifier.align(Alignment.CenterStart).size(6.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), CircleShape))
        Box(
            Modifier
                .size(sizes.successHero)
                .shadow(20.dp, CircleShape, ambientColor = BarrioTheme.colors.primaryGlow, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .border(4.dp, BarrioTheme.colors.fieldContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_success_check), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        }
        Box(Modifier.align(Alignment.CenterEnd).offset(x = (-12).dp, y = 10.dp).size(12.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape))
    }
}

/** Sensor de huella (L5, R7): botón circular de 104 dp con anillo decorativo. */
@Composable
fun FingerprintSensor(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val sizes = BarrioTheme.sizes
    Box(modifier.size(sizes.fingerprintSensor + sizes.fingerprintRingInset * 2), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .matchParentSize()
                .border(sizes.borderThin, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
        )
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            color = BarrioTheme.colors.fieldContainer,
            border = BorderStroke(sizes.borderEmphasis, MaterialTheme.colorScheme.outline),
            modifier = Modifier.size(sizes.fingerprintSensor),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(R.drawable.ic_fingerprint_large),
                    contentDescription = "Usar huella",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(53.dp, 59.dp),
                )
            }
        }
    }
}
