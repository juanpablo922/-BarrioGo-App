package com.barriogo.app.ui.auth.access

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.clickable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.barriogo.app.R
import com.barriogo.app.data.ProfileEntity
import com.barriogo.app.domain.PinPolicy
import com.barriogo.app.ui.auth.Biometrics
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.AuthScaffold
import com.barriogo.app.ui.components.BackButton
import com.barriogo.app.ui.components.BarrioLogo
import com.barriogo.app.ui.components.LogoSize
import com.barriogo.app.ui.components.NumericKeypad
import com.barriogo.app.ui.components.PinBoxes
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.components.ProfileAvatar
import com.barriogo.app.ui.components.ScreenTitle
import com.barriogo.app.ui.components.SecondaryButton
import com.barriogo.app.ui.components.TextLinkButton
import com.barriogo.app.ui.theme.BarrioTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Duración del splash según el Figma ("Loader · Splash 3s"). */
private const val SPLASH_MS = 3_000

/** Colores de avatar por perfil: rotan en el orden del Figma (amarillo, verde, dorado, crema). */
@Composable
fun avatarColorsFor(profile: ProfileEntity?): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> {
    val palette = BarrioTheme.colors.avatarColors
    return palette[(((profile?.id ?: 1L) - 1).mod(palette.size.toLong())).toInt()]
}

/** L1 · Splash: marca, lema y cargador de 3 s; luego [onFinished] decide a dónde ir. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var started by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(SPLASH_MS, easing = LinearEasing),
        label = "splash",
    )
    LaunchedEffect(Unit) {
        started = true
        delay(SPLASH_MS.toLong())
        onFinished()
    }
    AuthScaffold(scrollable = false, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        BarrioLogo(LogoSize.Large)
        Spacer(Modifier.height(BarrioTheme.spacing.xl))
        Text(
            "Tu tienda al día. Sin dejar de atender.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(BarrioTheme.sizes.loader),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            strokeWidth = 4.dp,
            strokeCap = StrokeCap.Round,
        )
        Spacer(Modifier.height(BarrioTheme.spacing.xxxl))
    }
}

/** L3 · ¿Quién está atendiendo? */
@Composable
fun ProfileSelectorScreen(profiles: List<ProfileEntity>, onPick: (ProfileEntity) -> Unit, onSignOut: () -> Unit) {
    val spacing = BarrioTheme.spacing
    AuthScaffold(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Spacer(Modifier.height(spacing.xxxl))
        ScreenTitle("¿Quién está atendiendo?", textAlign = TextAlign.Center)
        Spacer(Modifier.height(spacing.xxxl))
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(spacing.xxl)) {
            profiles.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.huge)) {
                    row.forEach { p -> ProfileTile(p, Modifier.weight(1f)) { onPick(p) } }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(spacing.xxxl * 2))
        TextLinkButton("Salir de la tienda", color = MaterialTheme.colorScheme.outline, underline = true, onClick = onSignOut)
        Spacer(Modifier.height(spacing.xxxl))
    }
}

@Composable
private fun ProfileTile(p: ProfileEntity, modifier: Modifier, onClick: () -> Unit) {
    val (container, content) = avatarColorsFor(p)
    Column(
        modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(role = Role.Button, onClickLabel = "Entrar como ${p.name}", onClick = onClick)
            .padding(vertical = BarrioTheme.spacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BarrioTheme.spacing.xs),
    ) {
        ProfileAvatar(p.initial, container, content, BarrioTheme.sizes.avatarLarge, isAdmin = p.isAdmin)
        Text(p.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
    }
}

/** Avatar de 64 dp + nombre que encabeza L4 y L5. */
@Composable
private fun ProfileHeader(profile: ProfileEntity?) {
    val (container, content) = avatarColorsFor(profile)
    ProfileAvatar(
        initial = profile?.initial ?: "",
        container = container,
        content = content,
        size = BarrioTheme.sizes.avatarMedium,
        initialStyle = MaterialTheme.typography.headlineSmall,
    )
    Spacer(Modifier.height(BarrioTheme.spacing.md))
    ScreenTitle(profile?.name ?: "", textAlign = TextAlign.Center)
}

/** L4 · Ingreso con la clave de 4 números del perfil. */
@Composable
fun PinLoginScreen(
    profileId: Long,
    onSuccess: () -> Unit,
    onLocked: () -> Unit,
    onUseFingerprint: () -> Unit,
    onForgotPin: () -> Unit,
    onBack: () -> Unit,
) {
    val vm = barrioViewModel { PinLoginViewModel(it, profileId) }
    val s by vm.state.collectAsState()
    val spacing = BarrioTheme.spacing
    LaunchedEffect(Unit) { if (vm.isLocked()) onLocked() }

    Box(Modifier.fillMaxSize()) {
        AuthScaffold(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(spacing.xxxl))
            ProfileHeader(s.profile)
            Spacer(Modifier.height(spacing.xs))
            Text("Ingresa tu clave de 4 números", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(spacing.xxxl))
            PinBoxes(length = PinPolicy.LENGTH, filled = s.pin.length, isError = s.error != null, gap = spacing.md)
            Text(
                s.error ?: " ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = spacing.sm)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
            Spacer(Modifier.height(spacing.xxl))
            NumericKeypad(
                onDigit = { vm.pressDigit(it, onSuccess, onLocked) },
                onBackspace = vm::backspace,
                bordered = true,
                horizontalGap = BarrioTheme.sizes.keypadGapWide,
                bottomStart = if (s.profile?.biometric == true) {
                    {
                        IconButton(onClick = onUseFingerprint, modifier = Modifier.size(BarrioTheme.sizes.keypadKey)) {
                            Icon(
                                painterResource(R.drawable.ic_fingerprint_key),
                                contentDescription = "Ingresar con huella",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                } else null,
            )
            Spacer(Modifier.height(spacing.xxxl))
            TextLinkButton("Olvidé mi clave", underline = true, onClick = onForgotPin)
            Spacer(Modifier.height(spacing.xxxl))
        }
        BackButton(onBack, Modifier.padding(start = spacing.lg, top = spacing.md))
    }
}

/** L5 · Ingreso con huella. Abre el lector apenas entra; "Usar mi clave" vuelve a L4. */
@Composable
fun FingerprintLoginScreen(profileId: Long, onSuccess: () -> Unit, onUsePin: () -> Unit) {
    val vm = barrioViewModel { PinLoginViewModel(it, profileId) }
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val spacing = BarrioTheme.spacing

    val prompt: () -> Unit = {
        Biometrics.authenticate(
            context,
            title = "Ingresa con tu huella",
            subtitle = s.profile?.name ?: "",
            negativeText = "Usar mi clave",
            onSuccess = { vm.signIn(); onSuccess() },
            onFailure = { msg -> if (msg != null) scope.launch { snackbar.showSnackbar(msg) } },
        )
    }
    LaunchedEffect(s.profile) { if (s.profile != null) prompt() }

    AuthScaffold(snackbarHostState = snackbar, scrollable = false, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(spacing.huge + spacing.xl))
        ProfileHeader(s.profile)
        Spacer(Modifier.weight(1f))
        com.barriogo.app.ui.components.FingerprintSensor(onClick = prompt)
        Spacer(Modifier.height(spacing.xxl))
        Text("Toca el sensor para ingresar", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(spacing.huge))
        SecondaryButton("Usar mi clave", onClick = onUsePin)
        Spacer(Modifier.height(spacing.huge + spacing.xxxl))
    }
}

/** L6 · Bloqueo temporal tras 3 intentos fallidos, con cuenta regresiva y barra de progreso. */
@Composable
fun LockedScreen(profileId: Long, onRetry: () -> Unit, onForgotPin: () -> Unit) {
    val app = LocalContext.current.applicationContext as com.barriogo.app.BarrioGoApp
    val store = app.container.store
    var remaining by remember { mutableLongStateOf((store.lockUntil(profileId) - System.currentTimeMillis()).coerceAtLeast(0)) }
    LaunchedEffect(profileId) {
        while (remaining > 0) {
            delay(250)
            remaining = (store.lockUntil(profileId) - System.currentTimeMillis()).coerceAtLeast(0)
        }
    }
    val seconds = ((remaining + 999) / 1000).toInt()
    val spacing = BarrioTheme.spacing

    AuthScaffold(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(
            painterResource(R.drawable.ic_lock_large),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = spacing.xxxl),
        )
        ScreenTitle("Demasiados intentos", textAlign = TextAlign.Center)
        Spacer(Modifier.height(spacing.md))
        Text(
            "Por seguridad, espera un momento antes de volver a intentar",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )
        Spacer(Modifier.height(spacing.xxxl))
        Text(
            "00:%02d".format(seconds),
            style = BarrioTheme.textStyles.timer,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics {
                contentDescription = if (seconds > 0) "Faltan $seconds segundos" else "Ya puedes volver a intentar"
                liveRegion = LiveRegionMode.Polite
            },
        )
        Spacer(Modifier.height(spacing.md))
        LinearProgressIndicator(
            progress = { remaining / PinPolicy.LOCK_MS.toFloat() },
            modifier = Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .height(BarrioTheme.sizes.progressTrack),
            color = MaterialTheme.colorScheme.primary,
            trackColor = BarrioTheme.colors.fieldContainer,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        Spacer(Modifier.height(spacing.xxxl + spacing.xs))
        PrimaryButton("Volver a intentar", enabled = remaining == 0L, onClick = onRetry, modifier = Modifier.widthIn(max = 320.dp))
        Spacer(Modifier.height(spacing.md))
        TextLinkButton("Olvidé mi clave", underline = true, onClick = onForgotPin)
    }
}
