package com.barriogo.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.barriogo.app.ui.theme.FigmaColors
import kotlinx.coroutines.delay

/** "Header - TopAppBar" del Figma: flecha amarilla de 40, título Inter Bold 20 amarillo y acción opcional a la derecha. */
@Composable
fun CabeceraFigma(titulo: String, onBack: (() -> Unit)?, accion: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().height(57.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack).testTag("back_button"),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = FigmaColors.Amarillo, modifier = Modifier.size(22.dp)) }
        }
        Text(titulo, color = FigmaColors.Amarillo, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        accion()
    }
}

/** Píldora "Paso 1 de 3" de c2–c4. */
@Composable
fun PildoraPaso(texto: String) {
    Text(
        texto,
        color = FigmaColors.Dorado,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(100.dp)).background(FigmaColors.Tarjeta).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/**
 * Botón amarillo del Figma. G2: 56 de alto y texto 17 (valores por defecto).
 * c2–c4 y a1: 48 de alto (py 14) y texto 16. Deshabilitado: dorado #CCAC6D al 45 %.
 */
@Composable
fun BotonFigma(
    texto: String,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    esquinas: Int = 12,
    alto: Int = 56,
    tamanoTexto: Int = 17,
    onClick: () -> Unit,
) {
    val forma = RoundedCornerShape(esquinas.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(alto.dp)
            .alpha(if (habilitado) 1f else 0.45f)
            .clip(forma)
            .background(if (habilitado) FigmaColors.AmarilloBoton else Color(0xFFCCAC6D))
            .clickable(enabled = habilitado, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(texto, color = FigmaColors.SobreAmarillo, fontSize = tamanoTexto.sp, fontWeight = FontWeight.Bold) }
}

/** Botón de c2–c4 y a1: 48 de alto, texto Bold 16. */
@Composable
fun BotonPaso(texto: String, modifier: Modifier = Modifier, habilitado: Boolean = true, onClick: () -> Unit) =
    BotonFigma(texto, modifier, habilitado, alto = 48, tamanoTexto = 16, onClick = onClick)

/**
 * Diálogo de K1 ("Revisa antes de guardar", "Este es tu último cambio", "Llegaste al límite de hoy"):
 * ícono de 44 arriba, título 19 y mensaje 14 centrados, botones de esquinas 14.
 * [acento] es amarillo normalmente y naranja #F2B544 en el último cambio o en el límite.
 */
@Composable
fun DialogoFigma(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    mensaje: String,
    principal: String,
    onPrincipal: () -> Unit,
    secundario: String? = null,
    onSecundario: () -> Unit = {},
    acentoNaranja: Boolean = false,
) {
    val acento = if (acentoNaranja) Color(0xFFF2B544) else FigmaColors.AmarilloBoton
    val forma = RoundedCornerShape(20.dp)
    Dialog(onDismissRequest = secundario?.let { { onSecundario() } } ?: onPrincipal, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 364.dp)
                .fillMaxWidth()
                .shadow(24.dp, forma, ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = 0.6f))
                .clip(forma)
                .background(FigmaColors.Tarjeta)
                .border(1.dp, if (acentoNaranja) acento else FigmaColors.Dorado, forma)
                .padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 22.dp)
                .testTag("dialogo_figma"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icono, null, tint = acento, modifier = Modifier.size(44.dp))
            Text(titulo, color = FigmaColors.Crema, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(mensaje, color = FigmaColors.Tenue, fontSize = 14.sp, lineHeight = 19.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(acento).clickable(onClick = onPrincipal).testTag("dialogo_principal"),
                contentAlignment = Alignment.Center,
            ) { Text(principal, color = FigmaColors.SobreAmarillo, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            if (secundario != null) {
                Box(
                    Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(FigmaColors.Hundido)
                        .border(1.dp, FigmaColors.Dorado, RoundedCornerShape(14.dp)).clickable(onClick = onSecundario).testTag("dialogo_secundario"),
                    contentAlignment = Alignment.Center,
                ) { Text(secundario, color = FigmaColors.Crema, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

/** "Botón secundario" del Figma: fondo #22593F, borde dorado de 1 y texto crema SemiBold 15. */
@Composable
fun BotonSecundarioFigma(texto: String, modifier: Modifier = Modifier, borde: Color? = null, colorTexto: Color? = null, onClick: () -> Unit) {
    val forma = RoundedCornerShape(14.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(forma)
            .background(FigmaColors.Tarjeta)
            .border(1.dp, borde ?: FigmaColors.Dorado, forma)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(texto, color = colorTexto ?: FigmaColors.Crema, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
}

/** Aviso gris oscuro (#103528) con texto tenue 13: "La apertura es la base del día…". */
@Composable
fun NotaFigma(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        color = FigmaColors.Tenue,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FigmaColors.Hundido).padding(horizontal = 14.dp, vertical = 12.dp),
    )
}

/**
 * "Teclado" de K0/K1: fondo #103528, teclas #22593F de 54 de alto con esquinas 12,
 * ⌫ abajo a la izquierda y "Listo" amarillo abajo a la derecha.
 */
@Composable
fun TecladoMonto(onDigito: (Char) -> Unit, onBorrar: () -> Unit, onListo: () -> Unit, modifier: Modifier = Modifier, listoHabilitado: Boolean = true) {
    Column(
        modifier.fillMaxWidth().background(FigmaColors.Hundido).padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 20.dp).testTag("teclado_monto"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("123", "456", "789", "⌫0L").forEach { fila ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { tecla ->
                    val listo = tecla == 'L'
                    Box(
                        Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            // "Listo" deshabilitado (K1 · Error cero): amarillo al 28 % y texto al 50 %.
                            .background(
                                when {
                                    !listo -> FigmaColors.Tarjeta
                                    listoHabilitado -> FigmaColors.AmarilloBoton
                                    else -> FigmaColors.AmarilloBoton.copy(alpha = 0.28f)
                                }
                            )
                            .clickable(enabled = !listo || listoHabilitado) {
                                when (tecla) {
                                    '⌫' -> onBorrar()
                                    'L' -> onListo()
                                    else -> onDigito(tecla)
                                }
                            }
                            .testTag("tecla_$tecla"),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (tecla) {
                            '⌫' -> Icon(Icons.AutoMirrored.Outlined.Backspace, "Borrar", tint = FigmaColors.Crema, modifier = Modifier.size(22.dp))
                            'L' -> Text(
                                "Listo",
                                color = FigmaColors.SobreAmarillo.copy(alpha = if (listoHabilitado) 1f else 0.5f),
                                fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            )
                            else -> Text(tecla.toString(), color = FigmaColors.Crema, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/** Agrega un dígito a un monto escrito con el teclado, máximo 10 cifras (hasta 1.000 millones se valida aparte). */
fun montoConDigito(monto: Long, d: Char): Long = if (monto.toString().length >= 10) monto else monto * 10 + (d - '0')
fun montoSinDigito(monto: Long): Long = monto / 10

/**
 * Ventana de vidrio para avisar un error o pedir confirmación (mismo vidrio que "Editar venta").
 * [secundario] es opcional: "Cancelar", "Conservar"…
 */
@Composable
fun AvisoDialogo(
    titulo: String,
    mensaje: String,
    principal: String,
    onPrincipal: () -> Unit,
    secundario: String? = null,
    onSecundario: () -> Unit = {},
    onCerrar: () -> Unit = onSecundario,
    principalPeligro: Boolean = false,
) {
    val forma = RoundedCornerShape(20.dp)
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 36.dp)
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .shadow(24.dp, forma, ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = 0.6f))
                .clip(forma)
                .background(FigmaColors.Tarjeta)
                .border(1.dp, FigmaColors.Dorado, forma)
                .padding(24.dp)
                .testTag("aviso_dialogo"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(titulo, color = FigmaColors.Crema, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(mensaje, color = FigmaColors.Cuerpo, fontSize = 15.sp, lineHeight = 20.sp)
            if (principalPeligro) {
                // G1 · "¿Anular este gasto?": Conservar (amarillo) arriba y la acción peligrosa con borde naranja.
                if (secundario != null) BotonDialogo(secundario, relleno = true, onClick = onSecundario)
                BotonDialogo(principal, relleno = false, onClick = onPrincipal)
            } else {
                BotonDialogo(principal, relleno = true, onClick = onPrincipal)
                if (secundario != null) BotonDialogo(secundario, relleno = false, onClick = onSecundario)
            }
        }
    }
}

@Composable
private fun BotonDialogo(texto: String, relleno: Boolean, onClick: () -> Unit) {
    val forma = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(forma)
            .then(if (relleno) Modifier.background(FigmaColors.AmarilloBoton) else Modifier.border(1.dp, FigmaColors.Naranja, forma))
            .clickable(onClick = onClick)
            .testTag("dialogo_$texto"),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, color = if (relleno) FigmaColors.SobreAmarillo else FigmaColors.Naranja, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

/** Mensaje corto que aparece abajo y se va solo: "Gasto anulado. Tu caja se actualizó." (G1 · Anulado). */
data class MensajeFlotante(val texto: String, val exito: Boolean = true, val id: Long = System.nanoTime())

@Composable
fun BoxScope.AvisoFlotante(mensaje: MensajeFlotante?, onTerminar: () -> Unit, abajo: Int = 104) {
    var visible by remember(mensaje?.id) { mutableStateOf(mensaje != null) }
    LaunchedEffect(mensaje?.id) {
        if (mensaje == null) return@LaunchedEffect
        visible = true
        delay(3_000)
        visible = false
        delay(300)
        onTerminar()
    }
    AnimatedVisibility(
        visible = visible && mensaje != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, end = 24.dp, bottom = abajo.dp),
    ) {
        val color = if (mensaje?.exito != false) FigmaColors.Verde else FigmaColors.Alerta
        Text(
            mensaje?.texto.orEmpty(),
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(FigmaColors.Tarjeta)
                .border(1.dp, color, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .testTag("aviso_flotante"),
        )
    }
}

/** Fila "etiqueta ........ valor" de los resúmenes c3 / c4. */
@Composable
fun FilaResumen(etiqueta: String, valor: String, colorValor: Color = FigmaColors.Crema, destacada: Boolean = false, negrita: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(
            etiqueta,
            color = if (destacada) FigmaColors.Dorado else FigmaColors.Cuerpo,
            fontSize = if (destacada) 14.sp else 13.sp,
            fontWeight = if (destacada) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            valor,
            color = colorValor,
            fontSize = if (destacada) 16.sp else 14.sp,
            fontWeight = if (destacada || negrita) FontWeight.Bold else FontWeight.SemiBold,
        )
    }
}

/** Línea divisoria de las tarjetas de resumen. */
@Composable
fun LineaResumen() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(FigmaColors.Dorado.copy(alpha = 0.3f)))
}

/** Tarjeta #22593F de esquinas 14 con título en mayúsculas dorado ("RESUMEN DEL DÍA"). */
@Composable
fun TarjetaResumen(titulo: String, modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(FigmaColors.Tarjeta).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(titulo.uppercase(), color = FigmaColors.Dorado, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        LineaResumen()
        contenido()
    }
}
