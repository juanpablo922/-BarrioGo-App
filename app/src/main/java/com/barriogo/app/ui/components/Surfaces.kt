package com.barriogo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.barriogo.app.ui.theme.SurfaceDark

/** Tarjeta base: esquinas redondeadas, fondo verde de superficie (o el color que se indique). */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    color: Color = SurfaceDark,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScopeAlias.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(color)
            .padding(padding),
        content = content,
    )
}

// Alias para poder tipar el content lambda de Card sin exponer ColumnScope directamente en imports circulares.
typealias ColumnScopeAlias = androidx.compose.foundation.layout.ColumnScope

/** Título de sección con un enlace opcional a la derecha ("Ver todos"). */
@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        if (action != null) {
            Text(
                action,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = onAction != null) { onAction?.invoke() }
                    .padding(4.dp),
            )
        }
    }
}

/** Píldora de estado (chip): "VENTA", "FIADO", "Documentado", etc. */
@Composable
fun StatusChip(text: String, color: Color, textColor: Color = Color.White, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = textColor,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier.clip(RoundedCornerShape(20.dp)).background(color).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
