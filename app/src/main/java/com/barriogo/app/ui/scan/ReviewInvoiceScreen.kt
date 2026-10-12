package com.barriogo.app.ui.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.barriogo.app.ai.InvoiceDraft
import com.barriogo.app.ai.InvoiceIssue
import com.barriogo.app.domain.Dates
import com.barriogo.app.domain.Money
import com.barriogo.app.ui.components.BarrioTopBar
import com.barriogo.app.ui.components.Card
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.components.SecondaryButton
import com.barriogo.app.ui.components.StatusChip
import com.barriogo.app.ui.theme.Accent
import com.barriogo.app.ui.theme.BarrioColors
import com.barriogo.app.ui.theme.MoneyDisplay
import com.barriogo.app.ui.theme.NegativeCoral
import com.barriogo.app.ui.theme.SurfaceDarkAlt

/**
 * C3 · Revisar factura: lo que el OCR leyó, con edición interactiva y diseño adaptado a móviles.
 */
@Composable
fun ReviewInvoiceScreen(
    draft: InvoiceDraft,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onConfirm: (InvoiceDraft) -> Unit
) {
    var currentDraft by remember(draft) { mutableStateOf(draft) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editSupplier by remember { mutableStateOf("") }
    var editAmountText by remember { mutableStateOf("") }

    val blocking = currentDraft.total <= 0
    val mismatch = currentDraft.items.isNotEmpty() && currentDraft.total > 0 && currentDraft.itemsSum != currentDraft.total

    // Diálogo interactivo para corregir a mano si el OCR leyó mal
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = BarrioColors.Surface,
            title = {
                Text(
                    "Corregir datos de la factura",
                    color = BarrioColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Ingresa el valor real de la factura y el proveedor:",
                        color = BarrioColors.TextHint,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = editSupplier,
                        onValueChange = { editSupplier = it },
                        label = { Text("Proveedor / Negocio") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BarrioColors.TextPrimary,
                            unfocusedTextColor = BarrioColors.TextPrimary,
                            focusedBorderColor = BarrioColors.Yellow,
                            unfocusedBorderColor = BarrioColors.CardSubtle,
                        )
                    )
                    OutlinedTextField(
                        value = editAmountText,
                        onValueChange = { editAmountText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Total en pesos ($)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BarrioColors.TextPrimary,
                            unfocusedTextColor = BarrioColors.TextPrimary,
                            focusedBorderColor = BarrioColors.Yellow,
                            unfocusedBorderColor = BarrioColors.CardSubtle,
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsedAmt = editAmountText.toLongOrNull() ?: currentDraft.total
                        val updated = currentDraft.copy(
                            supplier = if (editSupplier.isBlank()) currentDraft.supplier else editSupplier,
                            total = parsedAmt
                        )
                        currentDraft = updated
                        showEditDialog = false
                    }
                ) {
                    Text("Guardar", color = BarrioColors.Yellow, style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar", color = BarrioColors.TextHint)
                }
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .systemBarsPadding()
    ) {
        BarrioTopBar("Revisar factura", onBack = onBack)

        // Lista deslizable con weight(1f) para que nunca tape los botones
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            item {
                Text(
                    "Esto leyó el escáner de la factura",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BarrioColors.TextHint
                )
            }
            item {
                Card {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusChip("GASTO · PROVEEDOR", Accent)
                        ClickableText(
                            text = AnnotatedString("Corregir a mano ✎"),
                            onClick = {
                                editSupplier = currentDraft.supplier ?: ""
                                editAmountText = if (currentDraft.total > 0) currentDraft.total.toString() else ""
                                showEditDialog = true
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = BarrioColors.Yellow,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        currentDraft.supplier ?: "Proveedor no identificado",
                        style = MaterialTheme.typography.titleLarge,
                        color = BarrioColors.TextPrimary
                    )
                    Text(
                        listOfNotNull(
                            currentDraft.dateMillis?.let { Dates.full(it) },
                            currentDraft.number?.let { "Factura $it" },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = BarrioColors.TextHint,
                    )
                    Text(
                        Money.cop(currentDraft.total),
                        style = MoneyDisplay,
                        color = BarrioColors.Yellow,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            if (currentDraft.items.isNotEmpty()) {
                item {
                    Text(
                        "Detalle detectado",
                        style = MaterialTheme.typography.titleMedium,
                        color = BarrioColors.TextPrimary
                    )
                }
                items(currentDraft.items) { it2 ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "${it2.name}${if (it2.qty > 1) " x${it2.qty}" else ""}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = BarrioColors.TextPrimary
                        )
                        Text(
                            Money.cop(it2.amount),
                            style = MaterialTheme.typography.bodyLarge,
                            color = BarrioColors.TextHint
                        )
                    }
                }
                item {
                    Card(color = SurfaceDarkAlt) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "Suma del detalle",
                                color = if (mismatch) NegativeCoral else BarrioColors.TextHint
                            )
                            Text(
                                Money.cop(currentDraft.itemsSum),
                                color = if (mismatch) NegativeCoral else BarrioColors.TextPrimary
                            )
                        }
                        if (mismatch) {
                            Text(
                                if (currentDraft.missing > 0) "Faltan ${Money.cop(currentDraft.missing)} en el detalle frente al total."
                                else "El detalle suma ${Money.cop(-currentDraft.missing)} más que el total.",
                                color = NegativeCoral,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
            if (currentDraft.issues.contains(InvoiceIssue.SUPPLIER_MISSING) || currentDraft.issues.contains(InvoiceIssue.DATE_MISSING)) {
                item {
                    Text(
                        "Revisa el proveedor y la fecha: puedes tocar 'Corregir a mano' si necesitas ajustarlos.",
                        color = BarrioColors.TextHint,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        // Barra inferior fija con botones que se ajustan a cualquier pantalla
        Column(
            Modifier
                .fillMaxWidth()
                .background(BarrioColors.Background)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton("Volver a escanear", modifier = Modifier.weight(1f)) { onRetry() }
                PrimaryButton(
                    "Confirmar gasto",
                    modifier = Modifier.weight(1f),
                    enabled = !blocking && currentDraft.total > 0
                ) { onConfirm(currentDraft) }
            }
            Text(
                if (blocking) "Monto en $0. Toca 'Corregir a mano' para ingresar el valor."
                else "Se registrará como gasto de proveedor en el libro y se descontará de la caja.",
                style = MaterialTheme.typography.bodySmall,
                color = BarrioColors.TextHint,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}
