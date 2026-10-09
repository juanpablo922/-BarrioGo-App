package com.barriogo.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.ui.components.NumericKeypad
import com.barriogo.app.ui.components.PinSquares
import com.barriogo.app.ui.theme.BarrioColors

/**
 * R5 · Crea tu clave (PIN de 4 números con teclado virtual).
 */
@Composable
fun CreatePinScreen(
    subtitle: String = "Con estos 4 números entrarás a tu tienda todos los días, sin internet.",
    onCreated: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    val isComplete = pin.length == 4

    Box(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .systemBarsPadding()
            .testTag("create_pin_screen")
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(28.dp))

                Text(
                    "Crea tu clave",
                    color = BarrioColors.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("create_pin_title")
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    subtitle,
                    color = BarrioColors.TextHint,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(36.dp))

                PinSquares(
                    length = 4,
                    filled = pin.length,
                    modifier = Modifier.testTag("pin_squares_indicator")
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NumericKeypad(
                    onDigit = { digit ->
                        if (pin.length < 4) pin += digit
                    },
                    onBackspace = {
                        if (pin.isNotEmpty()) pin = pin.dropLast(1)
                    },
                    modifier = Modifier.testTag("pin_keypad")
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (isComplete) {
                            onCreated(pin)
                        }
                    },
                    enabled = isComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_pin_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BarrioColors.Yellow,
                        contentColor = BarrioColors.OnYellow,
                        disabledContainerColor = BarrioColors.Yellow.copy(alpha = 0.5f),
                        disabledContentColor = BarrioColors.OnYellow
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text("Guardar mi clave", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    if (isComplete) "Tu clave quedó lista" else "Ingresa 4 números para continuar",
                    color = if (isComplete) BarrioColors.PositiveGreen else BarrioColors.TextHint,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
