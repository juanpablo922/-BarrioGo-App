package com.barriogo.app.ui.auth.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.barriogo.app.R
import com.barriogo.app.domain.Role
import com.barriogo.app.ui.auth.Biometrics
import com.barriogo.app.ui.auth.PinCreationContent
import com.barriogo.app.ui.components.AuthScaffold
import com.barriogo.app.ui.components.BarrioTextField
import com.barriogo.app.ui.components.FieldVariant
import com.barriogo.app.ui.components.FingerprintSensor
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.components.RoleCard
import com.barriogo.app.ui.components.ScreenTitle
import com.barriogo.app.ui.components.TextLinkButton
import com.barriogo.app.ui.theme.BarrioTheme
import kotlinx.coroutines.launch

/** R5 · Crear tu clave de 4 números. */
@Composable
fun CreatePinScreen(vm: ProfileSetupViewModel, onSaved: () -> Unit) {
    val s by vm.state.collectAsState()
    val spacing = BarrioTheme.spacing
    AuthScaffold(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(spacing.xxl * 2))
        ScreenTitle("Crea tu clave", textAlign = TextAlign.Center)
        Spacer(Modifier.height(spacing.lg))
        Text(
            "Con estos 4 números entrarás a tu tienda todos los días, sin internet.",
            style = BarrioTheme.textStyles.subtitle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        Spacer(Modifier.height(spacing.lg))
        PinCreationContent(
            pin = s.pin,
            onDigit = vm::pressDigit,
            onBackspace = vm::backspace,
            buttonText = "Guardar mi clave",
            onSave = onSaved,
        )
        Spacer(Modifier.height(spacing.xl))
    }
}

/** R6 · Crear perfil: nombre y rol (Administrador o Vendedor). */
@Composable
fun CreateProfileScreen(vm: ProfileSetupViewModel, onCreated: () -> Unit) {
    val s by vm.state.collectAsState()
    val focus = LocalFocusManager.current
    val spacing = BarrioTheme.spacing
    AuthScaffold {
        Spacer(Modifier.height(spacing.huge))
        ScreenTitle("Crea tu perfil", large = true)
        Spacer(Modifier.height(spacing.lg))
        Text(
            "Así te reconocerá la app cada vez que entres a atender.",
            style = BarrioTheme.textStyles.subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(spacing.lg))
        BarrioTextField(
            value = s.name,
            onValueChange = vm::setName,
            label = "¿Cómo te llamas?",
            placeholder = "Ej. Don Juan",
            variant = FieldVariant.Solid,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        )
        Spacer(Modifier.height(spacing.lg))
        Text("¿Qué vas a hacer en la tienda?", style = BarrioTheme.textStyles.subtitle, color = BarrioTheme.colors.heading)
        Spacer(Modifier.height(spacing.lg))
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            RoleCard(
                title = Role.ADMIN.label,
                description = "Ve las ventas, maneja precios y crea perfiles",
                icon = painterResource(R.drawable.ic_role_admin),
                avatarColor = MaterialTheme.colorScheme.primary,
                selected = s.role == Role.ADMIN,
                onClick = { vm.setRole(Role.ADMIN) },
            )
            RoleCard(
                title = Role.SELLER.label,
                description = "Registra ventas y atiende a los clientes",
                icon = painterResource(R.drawable.ic_role_seller),
                avatarColor = BarrioTheme.colors.success,
                selected = s.role == Role.SELLER,
                onClick = { vm.setRole(Role.SELLER) },
            )
        }
        Spacer(Modifier.height(spacing.lg))
        PrimaryButton("Crear perfil", enabled = s.profileReady, loading = s.saving, onClick = { vm.createProfile(onCreated) })
        Spacer(Modifier.height(spacing.lg))
        Text(
            if (s.profileReady) "Todo listo, crea tu perfil" else "Escribe tu nombre y elige un tipo de perfil",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(spacing.xxl))
    }
}

/** R7 · Activar la huella para el perfil recién creado. */
@Composable
fun EnableFingerprintScreen(vm: ProfileSetupViewModel, onFinished: () -> Unit) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val spacing = BarrioTheme.spacing

    val activate: () -> Unit = {
        if (!Biometrics.isAvailable(context)) {
            scope.launch { snackbar.showSnackbar("Este celular no tiene una huella registrada. Puedes activarla más tarde.") }
        } else {
            Biometrics.authenticate(
                context,
                title = "Activa tu huella",
                subtitle = "Toca el sensor para confirmar que eres tú",
                negativeText = "Cancelar",
                onSuccess = { vm.enableBiometric(onFinished) },
                onFailure = { msg -> if (msg != null) scope.launch { snackbar.showSnackbar(msg) } },
            )
        }
    }

    AuthScaffold(
        snackbarHostState = snackbar,
        scrollable = false,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(spacing.huge + spacing.xl))
        Column(
            Modifier.padding(horizontal = spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            ScreenTitle("Entra con tu huella", large = true, textAlign = TextAlign.Center)
            Text(
                "Es más rápido que escribir tu clave y funciona sin internet. Puedes activarla ahora o más tarde.",
                style = BarrioTheme.textStyles.subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.weight(1f))
        FingerprintSensor(onClick = activate)
        Spacer(Modifier.height(spacing.xxl + spacing.xs))
        PrimaryButton("Activar mi huella", onClick = activate)
        Spacer(Modifier.height(spacing.xs))
        TextLinkButton("Ahora no, usar solo mi clave", color = MaterialTheme.colorScheme.outline, onClick = onFinished)
        Spacer(Modifier.height(spacing.xs))
        Text(
            "Tu huella se queda guardada en este celular. No la enviamos a ningún lado.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.huge))
    }
}
