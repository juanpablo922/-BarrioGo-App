package com.barriogo.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.domain.Role
import com.barriogo.app.ui.theme.BarrioColors

/**
 * P1 · Crea tu perfil (Screen 7 del prototipo Figma).
 */
@Composable
fun CreateProfileScreen(
    onDone: (name: String, role: Role) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf<Role?>(Role.ADMIN) }
    val isValid = name.isNotBlank() && selectedRole != null

    Box(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .systemBarsPadding()
            .imePadding()
            .testTag("create_profile_screen")
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(Modifier.height(20.dp))

                Text(
                    "Crea tu perfil",
                    color = BarrioColors.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("create_profile_title")
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Así te reconocerá la app cada vez que entres a atender.",
                    color = BarrioColors.TextHint,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(28.dp))

                // Field: ¿Cómo te llamas?
                Text("¿Cómo te llamas?", color = BarrioColors.TextHint, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Ej. Don Juan", color = BarrioColors.TextHint.copy(alpha = 0.6f), fontSize = 15.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
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

                Spacer(Modifier.height(28.dp))

                // Section: ¿Qué vas a hacer en la tienda?
                Text("¿Qué vas a hacer en la tienda?", color = BarrioColors.TextHint, fontSize = 14.sp)
                Spacer(Modifier.height(14.dp))

                // Card Administrador
                RoleOptionCard(
                    title = "Administrador",
                    description = "Ve las ventas, maneja precios y crea perfiles",
                    icon = Icons.Outlined.Security,
                    isSelected = selectedRole == Role.ADMIN,
                    onClick = { selectedRole = Role.ADMIN },
                    tag = "role_admin_card"
                )

                Spacer(Modifier.height(14.dp))

                // Card Vendedor
                RoleOptionCard(
                    title = "Vendedor",
                    description = "Registra ventas y atiende a los clientes",
                    icon = Icons.Outlined.Person,
                    isSelected = selectedRole == Role.SELLER,
                    onClick = { selectedRole = Role.SELLER },
                    tag = "role_seller_card"
                )
            }

            Column(
                modifier = Modifier.padding(top = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        if (isValid) {
                            onDone(name.trim(), selectedRole!!)
                        }
                    },
                    enabled = isValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("create_profile_submit_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BarrioColors.Yellow,
                        contentColor = BarrioColors.OnYellow,
                        disabledContainerColor = BarrioColors.Yellow.copy(alpha = 0.5f),
                        disabledContentColor = BarrioColors.OnYellow
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text("Crear perfil", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    if (isValid) "Todo listo, ya puedes crear tu perfil" else "Escribe tu nombre y elige un tipo de perfil",
                    color = if (isValid) BarrioColors.PositiveGreen else BarrioColors.TextHint,
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RoleOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BarrioColors.CardBg)
            .then(
                if (isSelected) Modifier.border(2.dp, BarrioColors.Yellow, RoundedCornerShape(16.dp))
                else Modifier.border(1.dp, BarrioColors.CardSubtle, RoundedCornerShape(16.dp))
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(tag)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) BarrioColors.Yellow.copy(alpha = 0.2f) else BarrioColors.Surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) BarrioColors.Yellow else BarrioColors.TextHint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = if (isSelected) BarrioColors.Yellow else BarrioColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    description,
                    color = BarrioColors.TextHint,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
