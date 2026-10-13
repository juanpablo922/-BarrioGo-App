package com.barriogo.app.ui.auth.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.barriogo.app.R
import com.barriogo.app.ui.auth.NewPasswordForm
import com.barriogo.app.ui.auth.StepHeader
import com.barriogo.app.ui.components.AuthScaffold
import com.barriogo.app.ui.components.BarrioTextField
import com.barriogo.app.ui.components.BottomActionBar
import com.barriogo.app.ui.components.CategoryCard
import com.barriogo.app.ui.components.FieldVariant
import com.barriogo.app.ui.components.FlowTopBar
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.components.ScreenTitle
import com.barriogo.app.ui.components.SuccessHero
import com.barriogo.app.ui.theme.BarrioTheme

private const val REGISTER_TITLE = "Registro de Negocio"

/** R1 · Datos del propietario. */
@Composable
fun OwnerDataScreen(vm: RegisterViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    val s by vm.state.collectAsState()
    val focus = LocalFocusManager.current
    val next = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
    val submit = { focus.clearFocus(); if (vm.validateOwner()) onContinue() }

    AuthScaffold(
        topBar = { FlowTopBar(REGISTER_TITLE, onBack) },
        bottomBar = { BottomActionBar { PrimaryButton("Continuar", onClick = submit) } },
    ) {
        Spacer(Modifier.height(BarrioTheme.spacing.md))
        com.barriogo.app.ui.components.RegisterStepper(current = 1)
        Spacer(Modifier.height(BarrioTheme.spacing.xxl))
        Text("Datos del propietario", style = MaterialTheme.typography.titleLarge, color = BarrioTheme.colors.heading)
        Spacer(Modifier.height(BarrioTheme.spacing.xs))
        Text("Cuéntanos un poco sobre ti y tu negocio", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(BarrioTheme.spacing.xxxl))

        Column(verticalArrangement = Arrangement.spacedBy(BarrioTheme.spacing.md)) {
            BarrioTextField(
                s.ownerName, { v -> vm.update { it.copy(ownerName = v) } }, "Nombre completo",
                placeholder = "Ej. Juan Pérez", variant = FieldVariant.Solid,
                errorText = s.ownerErrors.ownerName, keyboardActions = next,
            )
            BarrioTextField(
                s.storeName, { v -> vm.update { it.copy(storeName = v) } }, "Nombre de la tienda",
                placeholder = "Ej. Tienda Doña María", variant = FieldVariant.Solid,
                errorText = s.ownerErrors.storeName, keyboardActions = next,
            )
            BarrioTextField(
                s.address, { v -> vm.update { it.copy(address = v) } }, "Dirección del negocio",
                placeholder = "Ej. Calle 10 # 5-20", variant = FieldVariant.Solid,
                errorText = s.ownerErrors.address, keyboardActions = next,
            )
            BarrioTextField(
                s.email, { v -> vm.update { it.copy(email = v.trim()) } }, "Correo electrónico",
                placeholder = "tu@correo.com", variant = FieldVariant.Solid,
                errorText = s.ownerErrors.email, keyboardType = KeyboardType.Email, keyboardActions = next,
            )
            BarrioTextField(
                s.phone, { v -> vm.update { it.copy(phone = v.filter { c -> c.isDigit() || c == ' ' }.take(12)) } },
                "Número de celular",
                placeholder = "300 000 0000", variant = FieldVariant.Solid,
                leadingIcon = painterResource(R.drawable.ic_phone),
                errorText = s.ownerErrors.phone, keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
        }
        Spacer(Modifier.height(BarrioTheme.spacing.xl))
    }
}

private val CATEGORY_ICONS = mapOf(
    "Abarrotes" to R.drawable.ic_cat_abarrotes,
    "Panadería" to R.drawable.ic_cat_panaderia,
    "Farmacia" to R.drawable.ic_cat_farmacia,
    "Ferretería" to R.drawable.ic_cat_ferreteria,
    "Distribuidoras" to R.drawable.ic_cat_distribuidoras,
    "Otros" to R.drawable.ic_cat_otros,
)

/** R2 · Categoría de negocio (grilla de 2 columnas). */
@Composable
fun CategoryScreen(vm: RegisterViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    val s by vm.state.collectAsState()
    val spacing = BarrioTheme.spacing
    AuthScaffold(
        topBar = { FlowTopBar(REGISTER_TITLE, onBack) },
        bottomBar = {
            BottomActionBar { PrimaryButton("Continuar", enabled = s.category != null, onClick = onContinue) }
        },
    ) {
        StepHeader(step = 2, title = "Selecciona tu categoría", subtitle = "Elige el rubro principal de tu negocio", centered = true)
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            vm.categories.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    row.forEach { cat ->
                        CategoryCard(
                            label = cat,
                            icon = painterResource(CATEGORY_ICONS[cat] ?: R.drawable.ic_cat_otros),
                            selected = s.category == cat,
                            onClick = { vm.update { it.copy(category = cat) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        if (s.category == null) {
            Spacer(Modifier.height(spacing.md))
            Text(
                "Selecciona una categoría para continuar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(spacing.xl))
    }
}

/** R3 · Crear contraseña de la tienda. */
@Composable
fun CreatePasswordScreen(vm: RegisterViewModel, onBack: () -> Unit, onRegistered: () -> Unit) {
    val s by vm.state.collectAsState()
    AuthScaffold(topBar = { FlowTopBar(REGISTER_TITLE, onBack) }) {
        StepHeader(
            step = 3,
            title = "Crea tu contraseña",
            subtitle = "Elige una clave segura para proteger el acceso a tu tienda.",
        )
        NewPasswordForm(
            password = s.password,
            confirm = s.confirmPassword,
            passwordError = s.passwordError,
            confirmError = s.confirmError,
            buttonText = "Finalizar registro",
            onPasswordChange = { v -> vm.update { it.copy(password = v) } },
            onConfirmChange = { v -> vm.update { it.copy(confirmPassword = v) } },
            onSubmit = { vm.finish(onRegistered) },
        )
        Spacer(Modifier.height(BarrioTheme.spacing.xxxl))
    }
}

/** R4 · Registro completado. */
@Composable
fun RegisterSuccessScreen(onContinue: () -> Unit) {
    val spacing = BarrioTheme.spacing
    AuthScaffold(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SuccessHero()
        Spacer(Modifier.height(spacing.xxl - spacing.md))
        ScreenTitle("¡Registro Exitoso!", textAlign = TextAlign.Center)
        Spacer(Modifier.height(spacing.md))
        Text(
            "Tu tienda ya es parte de la comunidad BarrioGo. Todo está listo para que empieces a vender.",
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.625),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(spacing.xxl))
        PrimaryButton("Crear mi clave de acceso", onClick = onContinue)
    }
}
