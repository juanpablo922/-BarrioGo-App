package com.barriogo.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barriogo.app.data.ProfileEntity
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.theme.BarrioColors

/**
 * L3 · "¿Quién está atendiendo?": Grid de 2x2 perfiles con avatar circular como en el Figma.
 */
@Composable
fun ProfileSelectorScreen(
    onPick: (Long) -> Unit,
    onCreateProfile: () -> Unit,
    onLogout: () -> Unit = {}
) {
    val vm = barrioViewModel { AuthViewModel(it) }
    val profiles by vm.profiles.collectAsState(initial = emptyList())

    Box(
        Modifier
            .fillMaxSize()
            .background(BarrioColors.Background)
            .systemBarsPadding()
            .padding(24.dp)
            .testTag("profile_selector_screen")
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(36.dp))

                Text(
                    "¿Quién está atendiendo?",
                    color = BarrioColors.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("profile_selector_title")
                )

                Spacer(Modifier.height(48.dp))

                // Grid 2x2 of circular avatars
                Column(
                    verticalArrangement = Arrangement.spacedBy(36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val displayList = if (profiles.isNotEmpty()) profiles else listOf(
                        ProfileEntity(1, "Don Juan", "ADMIN", "", ""),
                        ProfileEntity(2, "María", "SELLER", "", ""),
                        ProfileEntity(3, "Carlos", "SELLER", "", ""),
                        ProfileEntity(4, "Luisa", "SELLER", "", "")
                    )

                    for (i in displayList.indices step 2) {
                        Row(
                            Modifier.fillMaxWidth(0.85f),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            val p1 = displayList[i]
                            ProfileAvatarItem(profile = p1, onClick = { onPick(p1.id) })

                            if (i + 1 < displayList.size) {
                                val p2 = displayList[i + 1]
                                ProfileAvatarItem(profile = p2, onClick = { onPick(p2.id) })
                            } else {
                                Spacer(Modifier.size(80.dp))
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Salir de la tienda",
                    color = BarrioColors.Yellow,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(onClick = onLogout)
                        .padding(12.dp)
                        .testTag("logout_button")
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatarItem(profile: ProfileEntity, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
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
                    .border(1.5.dp, BarrioColors.YellowStroke, CircleShape),
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
        }

        Spacer(Modifier.height(10.dp))

        Text(
            profile.name,
            color = BarrioColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
