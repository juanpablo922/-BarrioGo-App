package com.barriogo.app.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.data.ProfileEntity
import com.barriogo.app.domain.CuposPerfiles
import com.barriogo.app.domain.Role
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.NumericKeypad
import com.barriogo.app.ui.components.PinSquares
import com.barriogo.app.ui.components.LoadingOverlay
import com.barriogo.app.ui.theme.BarrioColors

/** Lo que se quería hacer cuando se pidió la clave de un administrador. */
private enum class AccionAdmin { AGREGAR, EDITAR }

/**
 * L3 · "¿Quién está atendiendo?": grid 2 columnas de perfiles con avatar circular como en el Figma,
 * más la tarjeta "Agregar perfil" y el modo "Editar" (P2) para cambiar nombre, rol o eliminar.
 * Agregar y editar piden la clave de un administrador (vale 10 minutos).
 * Cupos (CuposPerfiles): máximo 5 perfiles y 2 administradores; siempre queda 1 administrador.
 */
@Composable
fun ProfileSelectorScreen(
    onPick: (Long) -> Unit,
    onCreateProfile: () -> Unit,
    onLogout: () -> Unit = {}
) {
    val vm = barrioViewModel { AuthViewModel(it) }
    val profiles by vm.profiles.collectAsState(initial = emptyList())
    val context = LocalContext.current

    var editando by remember { mutableStateOf(false) }
    var perfilEnEdicion by remember { mutableStateOf<ProfileEntity?>(null) }
    var pedirAdminPara by remember { mutableStateOf<AccionAdmin?>(null) }

    val hayCupo = profiles.size < CuposPerfiles.MAXIMO_PERFILES

    fun hacer(accion: AccionAdmin) {
        when (accion) {
            AccionAdmin.AGREGAR -> onCreateProfile()
            AccionAdmin.EDITAR -> editando = true
        }
    }

    /** Sin perfiles no hay administrador a quién pedirle la clave: el primero se crea directo. */
    fun conAdmin(accion: AccionAdmin) {
        if (profiles.isEmpty() || vm.adminVigente() != null) hacer(accion) else pedirAdminPara = accion
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .systemBarsPadding()
            .padding(horizontal = 24.dp)
            .testTag("profile_selector_screen")
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            // Barra superior: "Editar" / "Listo"
            Row(
                Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (profiles.isNotEmpty()) {
                    TextButton(
                        onClick = { if (editando) editando = false else conAdmin(AccionAdmin.EDITAR) },
                        modifier = Modifier.testTag("edit_profiles_button")
                    ) {
                        Text(
                            if (editando) "Listo" else "Editar",
                            color = BarrioColors.Yellow,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(4.dp))

                Text(
                    if (editando) "Editar perfiles" else "¿Quién está atendiendo?",
                    color = BarrioColors.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("profile_selector_title")
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    if (editando) {
                        "Toca un perfil para cambiar su nombre o su rol, o para eliminarlo."
                    } else {
                        "Hasta ${CuposPerfiles.MAXIMO_PERFILES} perfiles, de ellos ${CuposPerfiles.MAXIMO_ADMINISTRADORES} administradores."
                    },
                    color = BarrioColors.TextHint,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(36.dp))

                // Perfiles de Firestore + la tarjeta "Agregar perfil" mientras haya cupo.
                val celdas: List<ProfileEntity?> = profiles + if (hayCupo) listOf(null) else emptyList()
                Column(
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    celdas.chunked(2).forEach { fila ->
                        Row(
                            Modifier.fillMaxWidth(0.85f),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            fila.forEach { perfil ->
                                if (perfil == null) {
                                    AddProfileItem(onClick = { conAdmin(AccionAdmin.AGREGAR) })
                                } else {
                                    ProfileAvatarItem(
                                        profile = perfil,
                                        editing = editando,
                                        onClick = { if (editando) perfilEnEdicion = perfil else onPick(perfil.id) }
                                    )
                                }
                            }
                            if (fila.size == 1) Spacer(Modifier.width(96.dp))
                        }
                    }
                }

                if (!hayCupo) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        CuposPerfiles.MENSAJE_MAXIMO_PERFILES,
                        color = BarrioColors.TextHint,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("profiles_full_hint")
                    )
                }

                Spacer(Modifier.height(24.dp))
            }

            Text(
                "Salir de la tienda",
                color = BarrioColors.Yellow,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .clickable(onClick = onLogout)
                    .padding(12.dp)
                    .testTag("logout_button")
            )
        }

        pedirAdminPara?.let { accion ->
            AdminPinSheet(
                admins = profiles.filter { it.isAdmin },
                onVerify = { admin, clave, onError ->
                    vm.verificarAdmin(admin, clave, onOk = {
                        pedirAdminPara = null
                        hacer(accion)
                    }, onError = onError)
                },
                onDismiss = { pedirAdminPara = null }
            )
        }

        perfilEnEdicion?.let { perfil ->
            EditProfileSheet(
                perfil = perfil,
                todos = profiles,
                validarNombre = { nombre -> vm.problemaNombre(nombre, profiles, perfil.id) },
                onSave = { nombre, rol, onError ->
                    vm.actualizarPerfil(perfil, nombre, rol, onDone = {
                        perfilEnEdicion = null
                        Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                    }, onError = onError)
                },
                onDelete = { onError ->
                    vm.eliminarPerfil(perfil, onDone = {
                        perfilEnEdicion = null
                        Toast.makeText(context, "Perfil eliminado", Toast.LENGTH_SHORT).show()
                    }, onError = onError)
                },
                onDismiss = { perfilEnEdicion = null }
            )
        }
    }

    vm.ocupado.collectAsState().value?.let { LoadingOverlay(it) }

    // Si el administrador que abrió la edición se quitó a sí mismo, se sale del modo editar.
    LaunchedEffect(editando, profiles) {
        if (editando && profiles.isNotEmpty() && vm.adminVigente() == null) editando = false
    }
}

@Composable
private fun ProfileAvatarItem(profile: ProfileEntity, editing: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(96.dp)
            .clickable(onClick = onClick)
            .testTag("profile_item_${profile.id}")
    ) {
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(BarrioColors.Surface)
                    .border(
                        if (editing) 2.dp else 1.5.dp,
                        if (editing) BarrioColors.Yellow else BarrioColors.YellowStroke,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    profile.initial,
                    color = BarrioColors.TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Gold crown/star badge for Admin
            if (profile.isAdmin) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(BarrioColors.Yellow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Admin",
                        tint = BarrioColors.OnYellow,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Lápiz en modo editar
            if (editing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(BarrioColors.Background)
                        .border(1.5.dp, BarrioColors.Yellow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Editar ${profile.name}",
                        tint = BarrioColors.Yellow,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            profile.name,
            color = BarrioColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/** Tarjeta "Agregar perfil": círculo punteado con un + (mismo tamaño que un avatar). */
@Composable
private fun AddProfileItem(onClick: () -> Unit) {
    val trazo = BarrioColors.Yellow
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(96.dp)
            .clickable(onClick = onClick)
            .testTag("add_profile_card")
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .padding(2.dp)
                .drawBehind {
                    drawCircle(
                        color = trazo,
                        radius = size.minDimension / 2 - 1.dp.toPx(),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()))
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = trazo, modifier = Modifier.size(32.dp))
        }

        Spacer(Modifier.height(10.dp))

        Text(
            "Agregar perfil",
            color = BarrioColors.Yellow,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

/** Pide la clave de 4 números de un administrador antes de agregar o editar perfiles. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminPinSheet(
    admins: List<ProfileEntity>,
    onVerify: (admin: ProfileEntity, clave: String, onError: (String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var elegido by remember(admins) { mutableStateOf(admins.firstOrNull()) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = BarrioColors.Background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("admin_pin_sheet")
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Clave de administrador",
                color = BarrioColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Solo un administrador puede agregar o editar perfiles.",
                color = BarrioColors.TextHint,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            if (admins.size > 1) {
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    admins.forEach { a ->
                        FilterChip(
                            selected = elegido?.id == a.id,
                            onClick = {
                                elegido = a
                                pin = ""
                                error = null
                            },
                            label = { Text(a.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BarrioColors.Yellow,
                                selectedLabelColor = BarrioColors.OnYellow,
                                labelColor = BarrioColors.TextPrimary
                            )
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(10.dp))
                Text(elegido?.name.orEmpty(), color = BarrioColors.Yellow, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(20.dp))
            PinSquares(length = 4, filled = pin.length, modifier = Modifier.testTag("admin_pin_squares"))
            Text(
                error.orEmpty(),
                color = BarrioColors.Error,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 10.dp).heightIn(min = 18.dp)
            )
            Spacer(Modifier.height(12.dp))

            NumericKeypad(
                onDigit = { d ->
                    val admin = elegido ?: return@NumericKeypad
                    if (pin.length < 4) {
                        pin += d
                        error = null
                        if (pin.length == 4) {
                            onVerify(admin, pin) {
                                error = it
                                pin = ""
                            }
                        }
                    }
                },
                onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                modifier = Modifier.testTag("admin_pin_keypad")
            )
        }
    }
}

/** P2 · Editar perfil: nombre, rol y eliminar, respetando los cupos de administradores. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(
    perfil: ProfileEntity,
    todos: List<ProfileEntity>,
    validarNombre: (String) -> String?,
    onSave: (nombre: String, rol: Role, onError: (String) -> Unit) -> Unit,
    onDelete: (onError: (String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val rolActual = if (perfil.isAdmin) Role.ADMIN else Role.SELLER
    var nombre by remember(perfil.id) { mutableStateOf(perfil.name) }
    var rol by remember(perfil.id) { mutableStateOf(rolActual) }
    var error by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }
    var confirmarEliminar by remember { mutableStateOf(false) }

    val admins = todos.count { it.isAdmin }
    val esUnicoAdmin = perfil.isAdmin && admins <= 1
    val adminLleno = !perfil.isAdmin && admins >= CuposPerfiles.MAXIMO_ADMINISTRADORES
    val problemaNombre = validarNombre(nombre)
    val cambio = nombre.trim() != perfil.name || rol != rolActual
    val fallar: (String) -> Unit = {
        error = it
        guardando = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = BarrioColors.Background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("edit_profile_sheet")
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            Text("Editar perfil", color = BarrioColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))

            Text("Nombre", color = BarrioColors.TextHint, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = nombre,
                onValueChange = {
                    nombre = it
                    error = null
                },
                singleLine = true,
                isError = problemaNombre != null,
                modifier = Modifier.fillMaxWidth().testTag("edit_profile_name_input"),
                shape = RoundedCornerShape(14.dp),
                textStyle = TextStyle(color = BarrioColors.TextPrimary, fontSize = 15.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = BarrioColors.Surface,
                    unfocusedContainerColor = BarrioColors.Surface,
                    errorContainerColor = BarrioColors.Surface,
                    focusedBorderColor = BarrioColors.Yellow,
                    unfocusedBorderColor = BarrioColors.YellowStroke,
                    cursorColor = BarrioColors.Yellow
                )
            )
            if (problemaNombre != null) {
                Text(problemaNombre, color = BarrioColors.Yellow, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
            }

            Spacer(Modifier.height(20.dp))
            Text("Rol", color = BarrioColors.TextHint, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            RoleOptionCard(
                title = "Administrador",
                description = if (adminLleno) "Ya hay ${CuposPerfiles.MAXIMO_ADMINISTRADORES} administradores, el máximo"
                else "Ve las ventas, maneja precios y crea perfiles",
                icon = Icons.Outlined.Security,
                isSelected = rol == Role.ADMIN,
                enabled = !adminLleno,
                onClick = { rol = Role.ADMIN },
                tag = "edit_role_admin_card"
            )
            Spacer(Modifier.height(10.dp))
            RoleOptionCard(
                title = "Vendedor",
                description = if (esUnicoAdmin) "Es el único administrador: primero nombra a otro"
                else "Registra ventas y atiende a los clientes",
                icon = Icons.Outlined.Person,
                isSelected = rol == Role.SELLER,
                enabled = !esUnicoAdmin,
                onClick = { rol = Role.SELLER },
                tag = "edit_role_seller_card"
            )

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error!!, color = BarrioColors.Error, fontSize = 14.sp, modifier = Modifier.testTag("edit_profile_error"))
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    guardando = true
                    onSave(nombre, rol, fallar)
                },
                enabled = cambio && problemaNombre == null && !guardando,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("edit_profile_save"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BarrioColors.Yellow,
                    contentColor = BarrioColors.OnYellow,
                    disabledContainerColor = BarrioColors.Yellow.copy(alpha = 0.5f),
                    disabledContentColor = BarrioColors.OnYellow
                )
            ) {
                Text(if (guardando) "Guardando…" else "Guardar cambios", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { confirmarEliminar = true },
                enabled = !esUnicoAdmin && !guardando,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("edit_profile_delete")
            ) {
                Text(
                    "Eliminar perfil",
                    color = if (esUnicoAdmin) BarrioColors.TextHint else BarrioColors.NegativeRed,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (esUnicoAdmin) {
                Text(
                    "No se puede eliminar: la tienda necesita al menos un administrador.",
                    color = BarrioColors.TextHint,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            containerColor = BarrioColors.Background,
            shape = RoundedCornerShape(20.dp),
            title = { Text("¿Eliminar a ${perfil.name}?", color = BarrioColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Ya no podrá entrar a la tienda. Sus ventas siguen en el libro con su nombre.",
                    color = BarrioColors.TextHint,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmarEliminar = false
                        guardando = true
                        onDelete(fallar)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BarrioColors.NegativeRed, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_delete_profile")
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminar = false }) {
                    Text("Cancelar", color = BarrioColors.Yellow)
                }
            }
        )
    }
}
