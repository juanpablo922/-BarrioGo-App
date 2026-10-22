package com.barriogo.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.domain.Validaciones
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.FigmaInput
import com.barriogo.app.ui.components.FigmaInputColors
import com.barriogo.app.ui.components.PrimaryButton
import com.barriogo.app.ui.components.LoadingOverlay
import com.barriogo.app.ui.theme.BarrioColors
import androidx.compose.ui.focus.FocusDirection
import kotlinx.coroutines.launch

data class CategoryItem(val name: String, val icon: ImageVector)

val CATEGORY_ITEMS = listOf(
    CategoryItem("Abarrotes", Icons.Outlined.Storefront),
    CategoryItem("Panadería", Icons.Outlined.BakeryDining),
    CategoryItem("Farmacia", Icons.Outlined.LocalPharmacy),
    CategoryItem("Ferretería", Icons.Outlined.Handyman),
    CategoryItem("Distribuidoras", Icons.Outlined.LocalShipping),
    CategoryItem("Otros", Icons.Outlined.MoreHoriz),
)

@Composable
fun RegisterScreen(onFinished: () -> Unit, onGoLogin: () -> Unit = {}) {
    val vm = barrioViewModel { AuthViewModel(it) }
    val s by vm.register.collectAsState()

    var newPassVisible by remember { mutableStateOf(false) }
    var confirmPassVisible by remember { mutableStateOf(false) }
    var showOtherDialog by remember { mutableStateOf(false) }
    var otherCustomCategory by remember { mutableStateOf("") }
    var showSuccessModal by remember { mutableStateOf(false) }
    var attemptedSubmit by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Cada paso empieza arriba.
    LaunchedEffect(s.step) { scrollState.scrollTo(0) }

    Box(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("register_screen")
    ) {
        Column(Modifier.fillMaxSize()) {
            // ENCABEZADO FIJO: flecha + título centrado, línea amarilla de lado a lado y stepper
            RegisterHeader(onBack = { if (s.step > 0) vm.prevStep() else onGoLogin() })
            RegisterStepIndicator(currentStep = s.step)

            // CONTENIDO DESPLAZABLE: formulario del paso con scroll independiente
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp)
            ) {
                // Step 1: Datos del propietario
                if (s.step == 0) {
                    StepTitle("Datos del propietario", "Cuéntanos un poco sobre ti y tu negocio", "step_0_title")

                    Spacer(Modifier.height(24.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        RegisterField(
                            label = "Nombre completo",
                            placeholder = "Ej. Juan Pérez",
                            value = s.ownerName,
                            onValueChange = { vm.updateRegister { r -> r.copy(ownerName = it) } },
                            error = if (attemptedSubmit && s.ownerName.isBlank()) "Ingresa tu nombre completo" else null,
                            tag = "owner_name_input"
                        )

                        RegisterField(
                            label = "Nombre de la tienda",
                            placeholder = "Ej. Tienda Doña María",
                            value = s.storeName,
                            onValueChange = { vm.updateRegister { r -> r.copy(storeName = it) } },
                            error = if (attemptedSubmit && s.storeName.isBlank()) "Ingresa el nombre de tu tienda" else null,
                            tag = "store_name_input"
                        )

                        RegisterField(
                            label = "Dirección del negocio",
                            placeholder = "Ej. Calle 10 # 5-20",
                            value = s.address,
                            onValueChange = { vm.updateRegister { r -> r.copy(address = it) } },
                            error = if (attemptedSubmit && s.address.isBlank()) "Ingresa la dirección del negocio" else null,
                            tag = "address_input"
                        )

                        RegisterField(
                            label = "Correo electrónico",
                            placeholder = "tu@correo.com",
                            value = s.email,
                            onValueChange = { vm.updateRegister { r -> r.copy(email = it) } },
                            keyboardType = KeyboardType.Email,
                            error = if (attemptedSubmit && !Validaciones.correoValido(s.email)) "Ingresa un correo electrónico válido" else null,
                            tag = "email_input"
                        )

                        RegisterField(
                            label = "Número de celular",
                            placeholder = "300 000 0000",
                            value = s.phone,
                            onValueChange = { vm.updateRegister { r -> r.copy(phone = it) } },
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done,
                            leadingIcon = { Icon(Icons.Outlined.Phone, null, tint = FigmaInputColors.tenue, modifier = Modifier.size(20.dp)) },
                            error = if (attemptedSubmit && !Validaciones.celularValido(s.phone)) "El celular debe tener 10 números y empezar por 3" else null,
                            tag = "phone_input"
                        )
                    }
                }

                // Step 2: Selecciona tu categoría
                if (s.step == 1) {
                    StepTitle("Selecciona tu categoría", "Elige el rubro principal de tu negocio", "step_1_title")

                    Spacer(Modifier.height(24.dp))

                    // 2 columns x 3 rows grid
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        for (i in CATEGORY_ITEMS.indices step 2) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                val item1 = CATEGORY_ITEMS[i]
                                CategoryCard(
                                    item = item1,
                                    isSelected = s.category == item1.name,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (item1.name == "Otros") {
                                            showOtherDialog = true
                                        } else {
                                            vm.updateRegister { it.copy(category = item1.name) }
                                        }
                                    }
                                )

                                if (i + 1 < CATEGORY_ITEMS.size) {
                                    val item2 = CATEGORY_ITEMS[i + 1]
                                    CategoryCard(
                                        item = item2,
                                        isSelected = s.category == item2.name || (item2.name == "Otros" && s.category.isNotBlank() && CATEGORY_ITEMS.none { it.name == s.category }),
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            if (item2.name == "Otros") {
                                                showOtherDialog = true
                                            } else {
                                                vm.updateRegister { it.copy(category = item2.name) }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        if (s.category.isBlank()) "Selecciona una categoría para continuar" else "Categoría: ${s.category}",
                        color = BarrioColors.Yellow,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    RegisterError(s.error)
                }

                // Step 3: Crea tu contraseña
                if (s.step == 2) {
                    StepTitle("Crea tu contraseña", "Elige una clave segura para proteger el acceso a tu tienda.", "step_2_title")

                    Spacer(Modifier.height(24.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        RegisterPasswordField(
                            label = "Nueva contraseña",
                            placeholder = "Ingresa tu nueva contraseña",
                            value = s.password,
                            visible = newPassVisible,
                            onToggle = { newPassVisible = !newPassVisible },
                            onValueChange = { vm.updateRegister { r -> r.copy(password = it) } },
                            tag = "new_password_input"
                        )
                        RegisterPasswordField(
                            label = "Confirmar contraseña",
                            placeholder = "Vuelve a ingresar la contraseña",
                            value = s.confirmPassword,
                            visible = confirmPassVisible,
                            onToggle = { confirmPassVisible = !confirmPassVisible },
                            onValueChange = { vm.updateRegister { r -> r.copy(confirmPassword = it) } },
                            // Avisa en cuanto se escribe algo distinto: por eso "Finalizar registro" sigue deshabilitado
                            error = if (s.confirmPassword.isNotEmpty() && s.confirmPassword != s.password) "Las contraseñas no coinciden" else null,
                            tag = "confirm_password_input"
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Requirements check list
                    val has8Chars = s.password.length >= 8
                    val hasUpper = s.password.any { it.isUpperCase() }
                    val hasNumOrSpecial = s.password.any { it.isDigit() || !it.isLetterOrDigit() }

                    PasswordRequirementRow("Al menos 8 caracteres", has8Chars)
                    PasswordRequirementRow("Una letra mayúscula", hasUpper)
                    PasswordRequirementRow("Un número o carácter especial", hasNumOrSpecial)

                    Spacer(Modifier.height(16.dp))
                    RegisterError(s.error)
                }

                Spacer(Modifier.height(24.dp))
            }

            // PIE FIJO: línea divisoria y el botón del paso, siempre visible
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (BarrioColors.isDark) Color.White.copy(alpha = 0.08f) else Color(0x1A0F2E22))
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(BarrioColors.Background)
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)
            ) {
                when (s.step) {
                    0 -> PrimaryButton(
                        "Continuar",
                        modifier = Modifier.testTag("continue_step_0_button"),
                    ) {
                        attemptedSubmit = true
                        if (s.problem == null) {
                            attemptedSubmit = false
                            vm.nextStep()
                        } else {
                            // Cierra el teclado y sube el formulario: así se ven todas las validaciones.
                            focusManager.clearFocus()
                            scope.launch { scrollState.animateScrollTo(0) }
                        }
                    }

                    1 -> PrimaryButton(
                        "Continuar",
                        enabled = s.category.isNotBlank(),
                        modifier = Modifier.testTag("continue_step_1_button"),
                    ) { if (s.category.isNotBlank()) vm.nextStep() }

                    else -> PrimaryButton(
                        if (s.loading) "Creando tu tienda…" else "Finalizar registro",
                        enabled = !s.loading && s.password.isNotBlank() && s.password == s.confirmPassword,
                        modifier = Modifier.testTag("finish_register_button"),
                    ) { vm.finishRegistration { showSuccessModal = true } }
                }
            }
        }

        // Creando la cuenta y la tienda en Firebase
        if (s.loading) LoadingOverlay("Creando tu tienda…")

        // Hoja inferior "Otros" (Figma: Hoja · Otro negocio)
        if (showOtherDialog) {
            OtherCategorySheet(
                value = otherCustomCategory,
                onValueChange = { otherCustomCategory = it },
                onSave = { vm.updateRegister { it.copy(category = otherCustomCategory.trim()) } },
                onDismiss = { showOtherDialog = false }
            )
        }

        // Success Modal "¡Registro Exitoso!"
        if (showSuccessModal) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .padding(16.dp)
                        .testTag("success_modal"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = BarrioColors.Background),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BarrioColors.YellowStroke)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Big Yellow Check Circle
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(BarrioColors.Yellow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = BarrioColors.OnYellow,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        Text(
                            "¡Registro Exitoso!",
                            color = BarrioColors.TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "Tu tienda ya es parte de la comunidad BarrioGo. Todo está listo para que empieces a vender.",
                            color = BarrioColors.TextHint,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(Modifier.height(28.dp))

                        PrimaryButton(
                            "Crear mi clave de acceso",
                            modifier = Modifier.testTag("create_access_key_button"),
                        ) {
                            showSuccessModal = false
                            onFinished()
                        }
                    }
                }
            }
        }
    }
}

/** Encabezado del registro: flecha amarilla, "Registro de Negocio" centrado y línea amarilla de borde a borde. */
@Composable
private fun RegisterHeader(onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp)
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
                    .testTag("register_back_arrow"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color(0xFFF2C849), modifier = Modifier.size(24.dp))
            }
            Text(
                "Registro de Negocio",
                color = Color(0xFFF2C849),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("register_header_title")
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(if (BarrioColors.isDark) Color(0xFFCCAC6D) else Color(0xFF6B5628).copy(alpha = 0.6f))
        )
    }
}

/** Stepper 1 · 2 · 3 de lado a lado (24 de margen), con líneas finas entre círculos. */
@Composable
private fun RegisterStepIndicator(currentStep: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepCircle(stepNumber = 1, isCompleted = currentStep > 0, isActive = currentStep == 0)
        StepLine(isCompleted = currentStep > 0, modifier = Modifier.weight(1f))
        StepCircle(stepNumber = 2, isCompleted = currentStep > 1, isActive = currentStep == 1)
        StepLine(isCompleted = currentStep > 1, modifier = Modifier.weight(1f))
        StepCircle(stepNumber = 3, isCompleted = false, isActive = currentStep == 2)
    }
}

@Composable
private fun StepCircle(stepNumber: Int, isCompleted: Boolean, isActive: Boolean) {
    val amarillo = Color(0xFFF2C849)
    val dorado = if (BarrioColors.isDark) Color(0xFFCCAC6D) else Color(0xFF6B5628)
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(
                when {
                    isCompleted -> BarrioColors.PositiveGreen
                    isActive -> amarillo
                    else -> Color.Transparent
                }
            )
            .then(
                if (!isCompleted && !isActive) Modifier.border(1.5.dp, dorado, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = BarrioColors.OnYellow, modifier = Modifier.size(16.dp))
        } else {
            Text(
                "$stepNumber",
                color = if (isActive) BarrioColors.OnYellow else (if (BarrioColors.isDark) amarillo else dorado),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StepLine(isCompleted: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .height(1.5.dp)
            .background(
                if (isCompleted) BarrioColors.PositiveGreen
                else if (BarrioColors.isDark) Color(0xFFCCAC6D) else Color(0xFF6B5628).copy(alpha = 0.6f)
            )
    )
}

/** Título y subtítulo de cada paso. */
@Composable
private fun StepTitle(title: String, subtitle: String, tag: String) {
    Text(
        title,
        color = FigmaInputColors.texto,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.testTag(tag)
    )
    Spacer(Modifier.height(6.dp))
    Text(subtitle, color = FigmaInputColors.texto, fontSize = 14.sp, lineHeight = 20.sp)
}

@Composable
private fun CategoryCard(
    item: CategoryItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(105.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BarrioColors.CardBg)
            .then(
                if (isSelected) Modifier.border(2.dp, BarrioColors.Yellow, RoundedCornerShape(16.dp))
                else Modifier.border(1.dp, BarrioColors.CardSubtle, RoundedCornerShape(16.dp))
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("category_card_${item.name}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                item.icon,
                contentDescription = item.name,
                tint = if (isSelected) BarrioColors.Yellow else BarrioColors.TextHint,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                item.name,
                color = if (isSelected) BarrioColors.Yellow else BarrioColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Hoja inferior para escribir una categoría libre cuando se elige "Otros".
 * Estados Figma: Otros escribir / Otros error / Otros lleno (R2 Categoría).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OtherCategorySheet(
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = BarrioColors.isDark
    val sheetBg = if (isDark) Color(0xFF22593F) else BarrioColors.Surface
    val subtitleColor = if (isDark) Color(0xFFD9E0DB) else BarrioColors.TextHint
    val placeholderColor = if (isDark) Color(0xFFA6B8AD) else BarrioColors.TextHint
    val gold = Color(0xFFCCAC6D)
    val warning = Color(0xFFF2B544)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var touched by remember { mutableStateOf(false) }
    val isBlank = value.isBlank()
    val showError = touched && isBlank

    fun close(afterHide: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            afterHide()
            onDismiss()
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = sheetBg,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(gold, RoundedCornerShape(2.dp))
            )
        },
        modifier = Modifier.testTag("custom_category_sheet")
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "¿De qué es tu negocio?",
                color = BarrioColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Escríbelo con tus palabras. Así la app se adapta a lo que vendes.",
                color = subtitleColor,
                fontSize = 14.sp
            )

            Column(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = {
                        touched = true
                        onValueChange(it)
                    },
                    singleLine = true,
                    textStyle = TextStyle(color = BarrioColors.TextPrimary, fontSize = 16.sp),
                    cursorBrush = SolidColor(BarrioColors.Yellow),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (!isBlank) close(onSave) else touched = true
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("custom_category_input"),
                    decorationBox = { inner ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BarrioColors.Background)
                                .border(2.dp, if (showError) warning else BarrioColors.Yellow, RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (value.isEmpty()) {
                                Text("Ej. Floristería, Papelería, Peluquería", color = placeholderColor, fontSize = 16.sp)
                            }
                            inner()
                        }
                    }
                )
                // Reserva el espacio del error para que la hoja no salte
                Text(
                    "Escribe de qué es tu negocio",
                    color = warning,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (showError) 1f else 0f)
                        .testTag("custom_category_error")
                )
            }

            Column(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { close(onSave) },
                    enabled = !isBlank,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("custom_category_save"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BarrioColors.Yellow,
                        contentColor = BarrioColors.OnYellow,
                        disabledContainerColor = gold.copy(alpha = 0.45f),
                        disabledContentColor = BarrioColors.OnYellow.copy(alpha = 0.6f)
                    )
                ) {
                    Text("Guardar", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = { close() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("custom_category_cancel"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancelar", color = gold, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Etiqueta de campo del registro: blanco encendido, SemiBold 15. */
@Composable
private fun RegisterLabel(label: String) {
    Text(
        label,
        color = FigmaInputColors.texto,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun RegisterField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    leadingIcon: (@Composable () -> Unit)? = null,
    error: String? = null,
    tag: String
) {
    val focus = LocalFocusManager.current
    Column(Modifier.fillMaxWidth()) {
        RegisterLabel(label)
        FigmaInput(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            leading = leadingIcon,
            keyboardType = keyboardType,
            imeAction = imeAction,
            onNext = { focus.moveFocus(FocusDirection.Down) },
            onDone = { focus.clearFocus() },
            error = error,
            highlightBorder = error != null,
            reserveErrorSpace = false,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun RegisterPasswordField(
    label: String,
    placeholder: String,
    value: String,
    visible: Boolean,
    onToggle: () -> Unit,
    onValueChange: (String) -> Unit,
    error: String? = null,
    tag: String
) {
    val texto = FigmaInputColors.texto
    Column(Modifier.fillMaxWidth()) {
        RegisterLabel(label)
        FigmaInput(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            trailing = {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = texto,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            keyboardType = KeyboardType.Password,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            error = error,
            highlightBorder = error != null,
            reserveErrorSpace = false,
            modifier = Modifier.testTag(tag)
        )
    }
}

/** Error del paso actual (validación o respuesta de Firebase). */
@Composable
private fun RegisterError(error: String?) {
    if (error != null) {
        Text(
            error,
            color = BarrioColors.Error,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth().testTag("register_error"),
        )
    }
}

@Composable
private fun PasswordRequirementRow(text: String, isMet: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isMet) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = if (isMet) BarrioColors.PositiveGreen else BarrioColors.TextHint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            color = if (isMet) BarrioColors.TextPrimary else BarrioColors.TextHint,
            fontSize = 13.sp
        )
    }
}
