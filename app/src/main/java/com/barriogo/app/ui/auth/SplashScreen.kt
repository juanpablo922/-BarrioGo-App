package com.barriogo.app.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.R
import com.barriogo.app.ui.theme.BarrioColors
import kotlinx.coroutines.delay

/** Pantalla de arranque: muestra la marca BarrioGo y navega al Login del prototipo. */
@Composable
fun SplashScreen(
    isRegistered: Boolean,
    isSignedIn: Boolean,
    hasProfiles: Boolean,
    onGoLogin: () -> Unit,
    onGoRegister: () -> Unit,
    onGoProfileSelector: () -> Unit,
) {
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, animationSpec = tween(600))
        delay(800)
        if (isSignedIn && hasProfiles) {
            onGoProfileSelector()
        } else {
            onGoLogin()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .padding(24.dp)
            .testTag("splash_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_barriogo),
            contentDescription = "BarrioGo Logo",
            modifier = Modifier
                .width(200.dp)
                .alpha(alphaAnim.value)
                .testTag("splash_logo"),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Tu tienda al día. Sin dejar de atender.",
            color = BarrioColors.TextHint,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .alpha(alphaAnim.value)
                .testTag("splash_subtitle"),
        )
        Spacer(Modifier.height(36.dp))
        CircularProgressIndicator(
            modifier = Modifier
                .width(28.dp)
                .height(28.dp)
                .alpha(alphaAnim.value),
            color = BarrioColors.Yellow,
            strokeWidth = 2.5.dp,
        )
    }
}
