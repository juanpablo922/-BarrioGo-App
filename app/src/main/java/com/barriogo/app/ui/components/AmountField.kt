package com.barriogo.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.domain.Money

/**
 * Campo "¿Cuánto?" del prototipo: el usuario escribe solo dígitos y ve el monto ya formateado
 * ("$ 45.000"). Devuelve el valor en pesos a través de [onValueChange].
 */
@Composable
fun AmountField(value: Long, onValueChange: (Long) -> Unit, modifier: Modifier = Modifier, label: String = "$ 0") {
    val display = if (value == 0L) label else Money.cop(value)
    OutlinedTextField(
        value = if (value == 0L) "" else value.toString(),
        onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }.take(9)
            onValueChange(if (digits.isEmpty()) 0L else digits.toLong())
        },
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("$ 0", style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold)) },
        textStyle = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
        visualTransformation = MoneyVisualTransformation(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

/** Muestra los dígitos escritos con separador de miles y el signo $ delante, sin tocar el valor real. */
internal class MoneyVisualTransformation : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val digits = text.text
        val grouped = if (digits.isEmpty()) "" else Money.group(digits.toLong())
        val out = "$ $grouped"
        val offset = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = out.length
            override fun transformedToOriginal(offset: Int): Int = digits.length
        }
        return androidx.compose.ui.text.input.TransformedText(androidx.compose.ui.text.AnnotatedString(out), offset)
    }
}
