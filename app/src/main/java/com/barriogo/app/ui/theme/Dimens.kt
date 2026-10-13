package com.barriogo.app.ui.theme

import androidx.compose.ui.unit.dp

/** Escala de espaciado del Figma (múltiplos de 4). */
object BarrioSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 40.dp
    val huge = 48.dp

    /** Margen lateral de todas las pantallas de acceso. */
    val screenHorizontal = xl
}

/** Tamaños fijos de componentes del Figma. */
object BarrioSizes {
    val fieldHeight = 56.dp
    val buttonHeight = 56.dp
    val textButtonHeight = 48.dp
    val topBarHeight = 56.dp
    val iconButton = 40.dp
    val appearanceButton = 44.dp
    val borderThin = 1.dp
    val borderStepper = 1.5.dp
    val borderEmphasis = 2.dp

    val keypadKey = 72.dp
    /** Separación horizontal del teclado de ingreso L4 (R5 usa spacing.xl). */
    val keypadGapWide = 34.dp
    val keypadGapVertical = 16.dp
    val pinBox = 50.dp
    val otpBoxWidth = 48.dp
    val otpBoxHeight = 56.dp

    val stepCurrent = 32.dp
    val stepOther = 28.dp
    val stepConnector = 3.dp

    val avatarLarge = 88.dp
    val avatarMedium = 64.dp
    val avatarSmall = 48.dp

    val logoIconLarge = 120.dp
    val logoIconMedium = 100.dp
    val successHero = 96.dp
    val fingerprintSensor = 104.dp
    val fingerprintRingInset = 18.dp
    val categoryCardHeight = 112.dp
    val roleCardHeight = 84.dp
    val progressTrack = 8.dp
    val loader = 40.dp

    /** Ancho máximo del contenido en tablets / pantallas anchas (igual al max-width del Figma). */
    val contentMaxWidth = 448.dp
}
