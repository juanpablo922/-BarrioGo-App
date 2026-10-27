package com.barriogo.app.ui.fiados

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.domain.Fiados
import com.barriogo.app.domain.Money
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.BotonFigma
import com.barriogo.app.ui.components.CabeceraFigma
import com.barriogo.app.ui.theme.BarrioColors
import com.barriogo.app.ui.theme.FigmaColors

/**
 * Ficha del cliente (Figma "Fiado · Doña Marta"): iniciales, celular, cuánto debe, "Registrar nuevo
 * fiado", "Registrar pago" y sus últimos fiados y abonos.
 */
@Composable
fun ClienteFiadoScreen(
    clienteId: Long,
    onBack: () -> Unit,
    onNuevoFiado: (Long) -> Unit,
    onRegistrarPago: (Long) -> Unit,
) {
    val vm = barrioViewModel { FiadosViewModel(it) }
    val cuentas by vm.cuentas.collectAsState()
    val todos by vm.movimientos.collectAsState()
    val cuenta = cuentas?.firstOrNull { it.cliente.id == clienteId }
    val movimientos = Fiados.movimientosDe(clienteId, todos, conAnulados = true)

    Box(Modifier.fillMaxSize().background(BarrioColors.Background).statusBarsPadding().testTag("cliente_fiado_screen")) {
        Column(Modifier.fillMaxSize()) {
            CabeceraFigma(cuenta?.cliente?.name.orEmpty(), onBack = onBack)
            if (cuenta == null) return@Column
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)) {
                item {
                    AvatarCliente(cuenta.cliente.name, grande = true)
                    Spacer(Modifier.height(12.dp))
                    Text(cuenta.cliente.name, color = FigmaColors.Crema, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(cuenta.cliente.phone.ifBlank { "Sin celular" }, color = FigmaColors.Tenue, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(FigmaColors.Tarjeta)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .testTag("cliente_saldo_card"),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("Debe actualmente", color = FigmaColors.Dorado, fontSize = 12.sp)
                        Text(Money.cop(cuenta.saldo), color = FigmaColors.Amarillo, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Text(Fiados.fiadosPendientes(cuenta.fiados), color = FigmaColors.Cuerpo, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    BotonFigma("Registrar nuevo fiado", Modifier.testTag("cliente_nuevo_fiado"), tamanoTexto = 15) { onNuevoFiado(clienteId) }
                    Spacer(Modifier.height(12.dp))
                    BotonContornoFiado("Registrar pago", Modifier.testTag("cliente_registrar_pago")) { onRegistrarPago(clienteId) }
                    Spacer(Modifier.height(20.dp))
                    SeccionFiado("Últimos movimientos")
                    if (movimientos.isEmpty()) {
                        Text(
                            "Todavía no tiene fiados ni pagos.",
                            color = FigmaColors.Cuerpo, fontSize = 14.sp,
                            modifier = Modifier.padding(top = 16.dp).testTag("cliente_sin_movimientos"),
                        )
                    }
                }
                items(movimientos, key = { it.id }) { m ->
                    FilaMovimientoFiado(m, ultima = m == movimientos.last())
                }
            }
        }
    }
}
