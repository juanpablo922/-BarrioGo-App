package com.barriogo.app.ui.fiados

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.ui.theme.BarrioColors

data class FiadoClientUi(
    val id: Long,
    val name: String,
    val subtitle: String,
    val amount: String
)

val DEFAULT_FIADOS = listOf(
    FiadoClientUi(1, "Marta Ramírez", "Desde el 2 de marzo · 3 fiados", "$ 12.000"),
    FiadoClientUi(2, "Pedro Gómez", "Desde el 5 de marzo · 1 fiado", "$ 8.000"),
    FiadoClientUi(3, "Rosa Hernández", "Desde el 6 de marzo · 2 fiados", "$ 5.000")
)

/**
 * F1 · Fiados (Screen 19 del prototipo Figma).
 */
@Composable
fun FiadosScreen(
    onNewFiado: () -> Unit,
    onOpenClient: (Long) -> Unit,
    onBack: () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) DEFAULT_FIADOS
        else DEFAULT_FIADOS.filter { it.name.contains(query, ignoreCase = true) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .systemBarsPadding()
            .testTag("fiados_screen")
    ) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(14.dp)) }

            // Header: Back Arrow + Title
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("fiados_back_arrow")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = BarrioColors.Yellow,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Fiados",
                        color = BarrioColors.Yellow,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("fiados_title")
                    )
                }
            }

            // Card: Te deben en total
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(BarrioColors.CardBg)
                        .border(1.dp, BarrioColors.CardSubtle, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            "Te deben en total",
                            color = BarrioColors.TextHint,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$ 25.000",
                            color = BarrioColors.TextPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "3 clientes con fiado pendiente",
                            color = BarrioColors.TextHint,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Button: + Nuevo fiado
            item {
                Button(
                    onClick = onNewFiado,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("new_fiado_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BarrioColors.Yellow,
                        contentColor = BarrioColors.OnYellow
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text("+ Nuevo fiado", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar por nombre o teléfono", color = BarrioColors.TextHint.copy(alpha = 0.7f), fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, null, tint = BarrioColors.TextHint, modifier = Modifier.size(20.dp))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("search_fiado_input"),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = TextStyle(color = BarrioColors.TextPrimary, fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BarrioColors.Surface,
                        unfocusedContainerColor = BarrioColors.Surface,
                        focusedBorderColor = BarrioColors.Yellow,
                        unfocusedBorderColor = BarrioColors.YellowStroke
                    )
                )
            }

            // List of Clients
            items(filtered) { client ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BarrioColors.CardBg)
                        .border(1.dp, BarrioColors.CardSubtle, RoundedCornerShape(16.dp))
                        .clickable { onOpenClient(client.id) }
                        .padding(16.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(BarrioColors.Surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Person, null, tint = BarrioColors.Yellow, modifier = Modifier.size(22.dp))
                            }

                            Spacer(Modifier.width(12.dp))

                            Column {
                                Text(
                                    client.name,
                                    color = BarrioColors.TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    client.subtitle,
                                    color = BarrioColors.TextHint,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Text(
                            client.amount,
                            color = BarrioColors.Yellow,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(90.dp)) }
        }
    }
}
