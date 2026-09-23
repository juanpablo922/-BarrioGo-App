package com.barriogo.app.ui.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.barriogo.app.ui.theme.NegativeCoral
import com.barriogo.app.ui.theme.SurfaceDarkAlt

/**
 * C3 · Revisar factura: lo que el OCR leyó, con la validación central del agente:
 * la suma del detalle debe cuadrar con el total antes de dejar confirmar.
 */
@Composable
fun ReviewInvoiceScreen(draft: InvoiceDraft, onBack: () -> Unit, onRetry: () -> Unit, onConfirm: () -> Unit) {
    val blocking = draft.issues.contains(InvoiceIssue.UNREADABLE)
    val mismatch = draft.issues.contains(InvoiceIssue.SUM_MISMATCH)

    Column(Modifier.fillMaxSize()) {
        BarrioTopBar("Revisar factura", onBack = onBack)
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Spacer(Modifier.height(4.dp)) }
            item { Text("Esto leí de la factura", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item {
                Card {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatusChip("GASTO · PROVEEDOR", Accent)
                        Text("Corregir a mano", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(draft.supplier ?: "Proveedor no identificado", style = MaterialTheme.typography.titleLarge)
                    Text(
                        listOfNotNull(
                            draft.dateMillis?.let { Dates.full(it) },
                            draft.number?.let { "Factura $it" },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(Money.cop(draft.total), style = com.barriogo.app.ui.theme.MoneyDisplay, modifier = Modifier.padding(top = 6.dp))
                }
            }
            if (draft.items.isNotEmpty()) {
                item { Text("Detalle detectado", style = MaterialTheme.typography.titleMedium) }
                items(draft.items) { it2 ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${it2.name}${if (it2.qty > 1) " x${it2.qty}" else ""}", style = MaterialTheme.typography.bodyLarge)
                        Text(Money.cop(it2.amount), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                item {
                    Card(color = SurfaceDarkAlt) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Suma del detalle", color = if (mismatch) NegativeCoral else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money.cop(draft.itemsSum), color = if (mismatch) NegativeCoral else MaterialTheme.colorScheme.onSurface)
                        }
                        if (mismatch) {
                            Text(
                                if (draft.missing > 0) "Faltan ${Money.cop(draft.missing)} en el detalle frente al total." else "El detalle suma ${Money.cop(-draft.missing)} más que el total.",
                                color = NegativeCoral, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
            if (draft.issues.contains(InvoiceIssue.SUPPLIER_MISSING) || draft.issues.contains(InvoiceIssue.DATE_MISSING)) {
                item {
                    Text(
                        "Revisa el proveedor y la fecha: puede que el OCR no los haya leído bien.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton("Volver a escanear", modifier = Modifier.weight(1f)) { onRetry() }
                PrimaryButton("Confirmar gasto", modifier = Modifier.weight(1f), enabled = !blocking && !mismatch && draft.total > 0) { onConfirm() }
            }
            Text(
                if (blocking) "No se pudo leer la factura. Vuelve a intentar o escríbelo a mano."
                else if (mismatch) "La suma no cuadra: corrige el detalle antes de registrar el gasto."
                else "Se registrará como gasto de proveedor en el libro y se descontará de tu caja.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
