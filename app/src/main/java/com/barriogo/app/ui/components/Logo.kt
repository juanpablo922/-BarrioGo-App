package com.barriogo.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.R
import com.barriogo.app.ui.theme.BarrioColors

/** "Logo BarrioGo" del Figma: la tiendita y el lettering BARRIO + GO. */
@Composable
fun BarrioLogo(
    modifier: Modifier = Modifier,
    iconWidth: Dp = 125.dp,
    iconHeight: Dp = 120.dp,
    fontSize: TextUnit = 42.sp,
    gap: Dp = 8.dp,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap)) {
        BarrioLogoIcon(Modifier.size(iconWidth, iconHeight))
        BarrioLettering(fontSize)
    }
}

/** Solo la tiendita del logo. */
@Composable
fun BarrioLogoIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.logo_icono),
        contentDescription = "BarrioGo",
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

/** Lettering BARRIO (crema) + GO (amarillo), tracking -2% como en el Figma. */
@Composable
fun BarrioLettering(fontSize: TextUnit = 42.sp) {
    Row {
        val tracking = (fontSize.value * -0.02f).sp
        Text("BARRIO", color = BarrioColors.Title, fontSize = fontSize, fontWeight = FontWeight.Bold, letterSpacing = tracking, maxLines = 1)
        Text("GO", color = BarrioColors.BrandGo, fontSize = fontSize, fontWeight = FontWeight.Bold, letterSpacing = tracking, maxLines = 1)
    }
}
