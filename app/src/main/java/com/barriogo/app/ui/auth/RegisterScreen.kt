package com.barriogo.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.domain.Validaciones
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.theme.BarrioColors

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

    Box(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .systemBarsPadding()
            .imePadding()
            .testTag("register_screen")
    ) {
        Column(Modifier.fillMaxSize()) {
            // FIXED HEADER: Barra superior y Stepper siempre visibles
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(BarrioColors.Background)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                // Top Bar: Back arrow + Title
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (s.step > 0) vm.prevStep() else onGoLogin()
                        },
                        modifier = Modifier.testTag("register_back_arrow")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = BarrioColors.Yellow,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Registro de Negocio",
                        color = BarrioColors.Yellow,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("register_header_title")
                    )
                }

                Spacer(Modifier.height(18.dp))

                // Step Indicator: 1 - 2 - 3 (queda fijo arriba)
                RegisterStepIndicator(currentStep = s.step)

                Spacer(Modifier.height(20.dp))
            }

            // CONTENIDO DESPLAZABLE: Formulario de datos con scroll independiente
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                // Step 1: Datos del propietario
                if (s.step == 0) {
                    Text(
                        "Datos del propietario",
                        color = BarrioColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("step_0_title")
                    )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Cuéntanos un poco sobre ti y tu negocio",
                    color = BarrioColors.TextHint,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(20.dp))

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
                    leadingIcon = { Icon(Icons.Outlined.Phone, null, tint = BarrioColors.TextHint, modifier = Modifier.size(20.dp)) },
                    error = if (attemptedSubmit && !Validaciones.celularValido(s.phone)) "El celular debe tener 10 números y empezar por 3" else null,
                    tag = "phone_input"
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        attemptedSubmit = true
                        if (s.problem == null) {
                            attemptedSubmit = false
                            vm.nextStep()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("continue_step_0_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BarrioColors.Yellow,
                        contentColor = BarrioColors.OnYellow
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text("Continuar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Step 2: Selecciona tu categoría
            if (s.step == 1) {
                Text(
                    "Selecciona tu categoría",
                    color = BarrioColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().testTag("step_1_title")
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Elige el rubro principal de tu negocio",
                    color = BarrioColors.TextHint,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

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
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (s.category.isNotBlank()) {
                            vm.nextStep()
                        }
                    },
                    enabled = s.category.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("continue_step_1_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BarrioColors.Yellow,
                        contentColor = BarrioColors.OnYellow,
                        disabledContainerColor = BarrioColors.Yellow.copy(alpha = 0.5f),
                        disabledContentColor = BarrioColors.OnYellow
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text("Continuar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Step 3: Crea tu contraseña
            if (s.step == 2) {
                Text(
                    "Crea tu contraseña",
                    color = BarrioColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("step_2_title")
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Elige una clave segura para proteger el acceso a tu tienda.",
                    color = BarrioColors.TextHint,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(24.dp))

                // Nueva contraseña
                Text("Nueva contraseña", color = BarrioColors.TextHint, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = s.password,
                    onValueChange = { vm.updateRegister { r -> r.copy(password = it) } },
                    placeholder = { Text("Ingresa tu nueva contraseña", color = BarrioColors.TextHint, fontSize = 15.sp) },
                    singleLine = true,
                    visualTransformation = if (newPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { newPassVisible = !newPassVisible }) {
                            Icon(
                                if (newPassVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = null,
                                tint = BarrioColors.TextPrimary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("new_password_input"),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = TextStyle(color = BarrioColors.TextPrimary, fontSize = 15.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BarrioColors.Surface,
                        unfocusedContainerColor = BarrioColors.Surface,
                        focusedBorderColor = BarrioColors.Yellow,
                        unfocusedBorderColor = BarrioColors.YellowStroke,
                        cursorColor = BarrioColors.Yellow
                    )
                )

                Spacer(Modifier.height(18.dp))

                // Confirmar contraseña
                Text("Confirmar contraseña", color = BarrioColors.TextHint, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = s.confirmPassword,
                    onValueChange = { vm.updateRegister { r -> r.copy(confirmPassword = it) } },
                    placeholder = { Text("Vuelve a ingresar la contraseña", color = BarrioColors.TextHint, fontSize = 15.sp) },
                    singleLine = true,
                    visualTransformation = if (confirmPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPassVisible = !confirmPassVisible }) {
                            Icon(
                                if (confirmPassVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = null,
                                tint = BarrioColors.TextPrimary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("confirm_password_input"),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = TextStyle(color = BarrioColors.TextPrimary, fontSize = 15.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BarrioColors.Surface,
                        unfocusedContainerColor = BarrioColors.Surface,
                        focusedBorderColor = BarrioColors.Yellow,
                        unfocusedBorderColor = BarrioColors.YellowStroke,
                        cursorColor = BarrioColors.Yellow
                    )
                )

                Spacer(Modifier.height(20.dp))

                // Requirements check list
                val has8Chars = s.password.length >= 8
                val hasUpper = s.password.any { it.isUpperCase() }
                val hasNumOrSpecial = s.password.any { it.isDigit() || !it.isLetterOrDigit() }

                PasswordRequirementRow("Al menos 8 caracteres", has8Chars)
                PasswordRequirementRow("Una letra mayúscula", hasUpper)
                PasswordRequirementRow("Un número o carácter especial", hasNumOrSpecial)

                Spacer(Modifier.height(28.dp))

                RegisterError(s.error)
                if (s.error != null) Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        vm.finishRegistration { showSuccessModal = true }
                    },
                    enabled = !s.loading && s.password.isNotBlank() && s.password == s.confirmPassword,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("finish_register_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BarrioColors.Yellow,
                        contentColor = BarrioColors.OnYellow,
                        disabledContainerColor = BarrioColors.Yellow.copy(alpha = 0.5f),
                        disabledContentColor = BarrioColors.OnYellow
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text(
                        if (s.loading) "Creando tu tienda…" else "Finalizar registro",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

        // Dialog for "Otros" category
        if (showOtherDialog) {
            AlertDialog(
                onDismissRequest = { showOtherDialog = false },
                containerColor = BarrioColors.Background,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        "¿De qué es tu negocio?",
                        color = BarrioColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            "Escríbelo con tus palabras. Así la app se adapta a lo que vendes.",
                            color = BarrioColors.TextHint,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = otherCustomCategory,
                            onValueChange = { otherCustomCategory = it },
                            placeholder = { Text("Ej. Floristería, Papelería, Peluquería", color = BarrioColors.TextHint, fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("custom_category_input"),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(color = BarrioColors.TextPrimary, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = BarrioColors.Surface,
                                unfocusedContainerColor = BarrioColors.Surface,
                                focusedBorderColor = BarrioColors.Yellow,
                                unfocusedBorderColor = BarrioColors.YellowStroke,
                                cursorColor = BarrioColors.Yellow
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (otherCustomCategory.isNotBlank()) {
                                vm.updateRegister { it.copy(category = otherCustomCategory.trim()) }
                            }
                            showOtherDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BarrioColors.Yellow, contentColor = BarrioColors.OnYellow),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOtherDialog = false }) {
                        Text("Cancelar", color = BarrioColors.TextHint)
                    }
                }
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

                        Button(
                            onClick = {
                                showSuccessModal = false
                                onFinished()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("create_access_key_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BarrioColors.Yellow,
                                contentColor = BarrioColors.OnYellow
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                        ) {
                            Text("Crear mi clave de acceso", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegisterStepIndicator(currentStep: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
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
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                when {
                    isCompleted -> BarrioColors.PositiveGreen
                    isActive -> BarrioColors.Yellow
                    else -> Color.Transparent
                }
            )
            .then(
                if (!isCompleted && !isActive) Modifier.border(1.5.dp, BarrioColors.YellowStroke, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = BarrioColors.OnYellow, modifier = Modifier.size(18.dp))
        } else {
            Text(
                "$stepNumber",
                color = if (isActive) BarrioColors.OnYellow else BarrioColors.Yellow,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StepLine(isCompleted: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(2.dp)
            .padding(horizontal = 8.dp)
            .background(if (isCompleted) BarrioColors.PositiveGreen else BarrioColors.YellowStroke)
    )
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

@Composable
private fun RegisterField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: (@Composable () -> Unit)? = null,
    error: String? = null,
    tag: String
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Text(label, color = BarrioColors.TextHint, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = BarrioColors.TextHint.copy(alpha = 0.6f), fontSize = 15.sp) },
            singleLine = true,
            leadingIcon = leadingIcon,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth().testTag(tag),
            shape = RoundedCornerShape(14.dp),
            textStyle = TextStyle(color = BarrioColors.TextPrimary, fontSize = 15.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = BarrioColors.Surface,
                unfocusedContainerColor = BarrioColors.Surface,
                focusedBorderColor = BarrioColors.Yellow,
                unfocusedBorderColor = if (error != null) BarrioColors.Error else BarrioColors.YellowStroke,
                cursorColor = BarrioColors.Yellow
            )
        )
        if (error != null) {
            Text(error, color = BarrioColors.Yellow, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
        }
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
