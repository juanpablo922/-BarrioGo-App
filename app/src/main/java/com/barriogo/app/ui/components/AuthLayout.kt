package com.barriogo.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.barriogo.app.R
import com.barriogo.app.ui.theme.BarrioTheme

enum class LogoSize { Large, Medium }

/** Marca BarrioGo: ícono de la tienda + lettering "BARRIO" (crema) "GO" (amarillo) en Inter Bold. */
@Composable
fun BarrioLogo(size: LogoSize = LogoSize.Medium, modifier: Modifier = Modifier) {
    val sizes = BarrioTheme.sizes
    val (iconSize, textStyle, gap) = when (size) {
        LogoSize.Large -> Triple(sizes.logoIconLarge, BarrioTheme.textStyles.brandLarge, BarrioTheme.spacing.xs)
        LogoSize.Medium -> Triple(sizes.logoIconMedium, BarrioTheme.textStyles.brandMedium, 7.dp)
    }
    Column(
        modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        Image(
            painter = painterResource(R.drawable.logo_barriogo_icon),
            contentDescription = "BarrioGo",
            modifier = Modifier.size(iconSize),
        )
        Row {
            Text("BARRIO", style = textStyle, color = BarrioTheme.colors.heading)
            Text("GO", style = textStyle, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Título de pantalla (22 SemiBold) marcado como encabezado para TalkBack. */
@Composable
fun ScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    large: Boolean = false,
) {
    Text(
        text,
        modifier = modifier.semantics { heading() },
        style = if (large) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
        color = BarrioTheme.colors.heading,
        textAlign = textAlign,
    )
}

/** Flecha de volver amarilla de 40 dp (Figma "Botón Atrás"). */
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.size(BarrioTheme.sizes.iconButton)) {
        Icon(
            painterResource(R.drawable.ic_arrow_back),
            contentDescription = "Volver",
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Barra superior de los flujos de registro y recuperación: título amarillo y línea dorada inferior. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowTopBar(title: String, onBack: () -> Unit) {
    Column {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
            },
            navigationIcon = { BackButton(onBack, Modifier.padding(start = BarrioTheme.spacing.xs)) },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        HorizontalDivider(thickness = BarrioTheme.sizes.borderThin, color = MaterialTheme.colorScheme.outline)
    }
}

/** Zona fija inferior con la acción principal (Figma "Fixed Bottom Action Area"). */
@Composable
fun BottomActionBar(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column {
            HorizontalDivider(thickness = BarrioTheme.sizes.borderThin, color = BarrioTheme.colors.divider)
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = BarrioTheme.spacing.screenHorizontal, vertical = BarrioTheme.spacing.md),
                content = content,
            )
        }
    }
}

/**
 * Estructura común de las pantallas de acceso: fondo verde, barra superior opcional, contenido
 * desplazable centrado con ancho máximo (448 dp en tablets) y acción fija inferior opcional.
 */
@Composable
fun AuthScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    scrollable: Boolean = true,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    contentPadding: Dp = BarrioTheme.spacing.screenHorizontal,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .widthIn(max = BarrioTheme.sizes.contentMaxWidth)
                    .fillMaxSize()
                    .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = contentPadding),
                horizontalAlignment = horizontalAlignment,
                verticalArrangement = verticalArrangement,
                content = content,
            )
        }
    }
}
