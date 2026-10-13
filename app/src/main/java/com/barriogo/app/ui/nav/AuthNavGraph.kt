package com.barriogo.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.barriogo.app.BarrioGoApp
import com.barriogo.app.ui.auth.access.FingerprintLoginScreen
import com.barriogo.app.ui.auth.access.LockedScreen
import com.barriogo.app.ui.auth.access.PinLoginScreen
import com.barriogo.app.ui.auth.access.ProfileSelectorScreen
import com.barriogo.app.ui.auth.access.SplashScreen
import com.barriogo.app.ui.auth.login.LoginNotice
import com.barriogo.app.ui.auth.login.LoginScreen
import com.barriogo.app.ui.auth.recovery.NewPasswordScreen
import com.barriogo.app.ui.auth.recovery.NewPinScreen
import com.barriogo.app.ui.auth.recovery.RecoverCodeScreen
import com.barriogo.app.ui.auth.recovery.RecoverEmailScreen
import com.barriogo.app.ui.auth.recovery.RecoveryTarget
import com.barriogo.app.ui.auth.recovery.RecoveryViewModel
import com.barriogo.app.ui.auth.register.CategoryScreen
import com.barriogo.app.ui.auth.register.CreatePasswordScreen
import com.barriogo.app.ui.auth.register.OwnerDataScreen
import com.barriogo.app.ui.auth.register.RegisterSuccessScreen
import com.barriogo.app.ui.auth.register.RegisterViewModel
import com.barriogo.app.ui.auth.setup.CreatePinScreen
import com.barriogo.app.ui.auth.setup.CreateProfileScreen
import com.barriogo.app.ui.auth.setup.EnableFingerprintScreen
import com.barriogo.app.ui.auth.setup.ProfileSetupViewModel
import com.barriogo.app.ui.barrioViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val profileArg = listOf(navArgument("profileId") { type = NavType.LongType })

private fun NavBackStackEntry.profileId(): Long = arguments?.getLong("profileId") ?: 0L

/** Entrada del subgrafo [route]: dueña del ViewModel que comparten sus pantallas. */
@Composable
private fun NavHostController.graphEntry(entry: NavBackStackEntry, route: String): NavBackStackEntry =
    remember(entry) { getBackStackEntry(route) }

/** Va a [route] y borra toda la pila: no se puede volver atrás al flujo de acceso. */
private fun NavHostController.navigateClearing(route: String) = navigate(route) {
    popUpTo(graph.id) { inclusive = true }
}

/**
 * Flujos de acceso del Figma: splash (L1), ingreso (L2-L6), registro (R1-R4), primer perfil (R5-R7)
 * y recuperación de contraseña (RP1-RP3) y de clave (CP1-CP3).
 */
fun NavGraphBuilder.authGraph(nav: NavHostController, app: BarrioGoApp) {
    val container = app.container

    composable(Routes.SPLASH) {
        val scope = rememberCoroutineScope()
        SplashScreen(onFinished = {
            scope.launch {
                val store = container.store
                val hasProfiles = container.repo.profiles.first().isNotEmpty()
                nav.navigateClearing(
                    when {
                        !store.registered || !store.signedIn -> Routes.login()
                        !hasProfiles -> Routes.SETUP_GRAPH
                        else -> Routes.PROFILE_SELECTOR
                    },
                )
            }
        })
    }

    // ---- L2 · Ingreso con correo ----
    composable(
        Routes.LOGIN,
        arguments = listOf(navArgument("notice") { type = NavType.StringType; nullable = true; defaultValue = null }),
    ) { entry ->
        val notice = entry.arguments?.getString("notice")?.let { runCatching { LoginNotice.valueOf(it) }.getOrNull() }
        val scope = rememberCoroutineScope()
        LoginScreen(
            notice = notice,
            onLoggedIn = {
                scope.launch {
                    val hasProfiles = container.repo.profiles.first().isNotEmpty()
                    nav.navigateClearing(if (hasProfiles) Routes.PROFILE_SELECTOR else Routes.SETUP_GRAPH)
                }
            },
            onForgotPassword = { nav.navigate(Routes.RECOVER_PASSWORD_GRAPH) },
            onRegister = { nav.navigate(Routes.REGISTER_GRAPH) },
        )
    }

    // ---- L3 · Selector de perfil ----
    composable(Routes.PROFILE_SELECTOR) {
        val profiles by container.repo.profiles.collectAsState(initial = emptyList())
        ProfileSelectorScreen(
            profiles = profiles,
            onPick = { p -> nav.navigate(if (p.biometric) Routes.fingerprintLogin(p.id) else Routes.pinLogin(p.id)) },
            onSignOut = {
                container.activeProfileId.value = null
                container.store.signedIn = false
                nav.navigateClearing(Routes.login(LoginNotice.SIGNED_OUT))
            },
        )
    }

    // ---- L4 · Clave de 4 números ----
    composable(Routes.PIN_LOGIN, arguments = profileArg) { entry ->
        val id = entry.profileId()
        PinLoginScreen(
            profileId = id,
            onSuccess = { nav.navigateClearing(Routes.HOME) },
            onLocked = { nav.navigate(Routes.locked(id)) { popUpTo(Routes.PIN_LOGIN) { inclusive = true } } },
            onUseFingerprint = { nav.navigate(Routes.fingerprintLogin(id)) { popUpTo(Routes.PIN_LOGIN) { inclusive = true } } },
            onForgotPin = { nav.navigate(Routes.recoverPin(id)) },
            onBack = { nav.popBackStack() },
        )
    }

    // ---- L5 · Huella ----
    composable(Routes.FINGERPRINT_LOGIN, arguments = profileArg) { entry ->
        val id = entry.profileId()
        FingerprintLoginScreen(
            profileId = id,
            onSuccess = { nav.navigateClearing(Routes.HOME) },
            onUsePin = { nav.navigate(Routes.pinLogin(id)) { popUpTo(Routes.FINGERPRINT_LOGIN) { inclusive = true } } },
        )
    }

    // ---- L6 · Bloqueo ----
    composable(Routes.LOCKED, arguments = profileArg) { entry ->
        val id = entry.profileId()
        LockedScreen(
            profileId = id,
            onRetry = { nav.navigate(Routes.pinLogin(id)) { popUpTo(Routes.LOCKED) { inclusive = true } } },
            onForgotPin = { nav.navigate(Routes.recoverPin(id)) },
        )
    }

    // ---- R1-R4 · Registro del negocio ----
    navigation(startDestination = Routes.REGISTER_OWNER, route = Routes.REGISTER_GRAPH) {
        composable(Routes.REGISTER_OWNER) { entry ->
            val vm = barrioViewModel(nav.graphEntry(entry, Routes.REGISTER_GRAPH)) { RegisterViewModel(it) }
            OwnerDataScreen(vm, onBack = { nav.popBackStack() }, onContinue = { nav.navigate(Routes.REGISTER_CATEGORY) })
        }
        composable(Routes.REGISTER_CATEGORY) { entry ->
            val vm = barrioViewModel(nav.graphEntry(entry, Routes.REGISTER_GRAPH)) { RegisterViewModel(it) }
            CategoryScreen(vm, onBack = { nav.popBackStack() }, onContinue = { nav.navigate(Routes.REGISTER_PASSWORD) })
        }
        composable(Routes.REGISTER_PASSWORD) { entry ->
            val vm = barrioViewModel(nav.graphEntry(entry, Routes.REGISTER_GRAPH)) { RegisterViewModel(it) }
            CreatePasswordScreen(
                vm,
                onBack = { nav.popBackStack() },
                onRegistered = { nav.navigateClearing(Routes.REGISTER_SUCCESS) },
            )
        }
    }
    composable(Routes.REGISTER_SUCCESS) {
        RegisterSuccessScreen(onContinue = { nav.navigateClearing(Routes.SETUP_GRAPH) })
    }

    // ---- R5-R7 · Primer perfil ----
    navigation(startDestination = Routes.SETUP_PIN, route = Routes.SETUP_GRAPH) {
        composable(Routes.SETUP_PIN) { entry ->
            val vm = barrioViewModel(nav.graphEntry(entry, Routes.SETUP_GRAPH)) { ProfileSetupViewModel(it) }
            CreatePinScreen(vm, onSaved = { nav.navigate(Routes.SETUP_PROFILE) })
        }
        composable(Routes.SETUP_PROFILE) { entry ->
            val vm = barrioViewModel(nav.graphEntry(entry, Routes.SETUP_GRAPH)) { ProfileSetupViewModel(it) }
            CreateProfileScreen(vm, onCreated = {
                nav.navigate(Routes.SETUP_FINGERPRINT) { popUpTo(Routes.SETUP_PIN) { inclusive = false } }
            })
        }
        composable(Routes.SETUP_FINGERPRINT) { entry ->
            val vm = barrioViewModel(nav.graphEntry(entry, Routes.SETUP_GRAPH)) { ProfileSetupViewModel(it) }
            EnableFingerprintScreen(vm, onFinished = { nav.navigateClearing(Routes.HOME) })
        }
    }

    // ---- RP1-RP3 · Recuperar contraseña ----
    navigation(startDestination = Routes.RECOVER_PASSWORD_EMAIL, route = Routes.RECOVER_PASSWORD_GRAPH) {
        val vmFor: @Composable (NavBackStackEntry) -> RecoveryViewModel = { entry ->
            barrioViewModel(nav.graphEntry(entry, Routes.RECOVER_PASSWORD_GRAPH)) {
                RecoveryViewModel(it, RecoveryTarget.PASSWORD, profileId = null)
            }
        }
        composable(Routes.RECOVER_PASSWORD_EMAIL) { entry ->
            RecoverEmailScreen(vmFor(entry), onBack = { nav.popBackStack() }, onCodeSent = { nav.navigate(Routes.RECOVER_PASSWORD_CODE) })
        }
        composable(Routes.RECOVER_PASSWORD_CODE) { entry ->
            RecoverCodeScreen(vmFor(entry), onBack = { nav.popBackStack() }, onVerified = {
                nav.navigate(Routes.RECOVER_PASSWORD_NEW) { popUpTo(Routes.RECOVER_PASSWORD_CODE) { inclusive = true } }
            })
        }
        composable(Routes.RECOVER_PASSWORD_NEW) { entry ->
            NewPasswordScreen(vmFor(entry), onBack = { nav.popBackStack() }, onSaved = {
                nav.navigateClearing(Routes.login(LoginNotice.PASSWORD_RESET))
            })
        }
    }

    // ---- CP1-CP3 · Recuperar la clave de un perfil ----
    navigation(
        startDestination = Routes.RECOVER_PIN_EMAIL,
        route = Routes.RECOVER_PIN_GRAPH,
        arguments = profileArg,
    ) {
        val vmFor: @Composable (NavBackStackEntry) -> RecoveryViewModel = { entry ->
            val graph = nav.graphEntry(entry, Routes.RECOVER_PIN_GRAPH)
            barrioViewModel(graph) { RecoveryViewModel(it, RecoveryTarget.PIN, profileId = graph.profileId()) }
        }
        composable(Routes.RECOVER_PIN_EMAIL) { entry ->
            RecoverEmailScreen(vmFor(entry), onBack = { nav.popBackStack() }, onCodeSent = { nav.navigate(Routes.RECOVER_PIN_CODE) })
        }
        composable(Routes.RECOVER_PIN_CODE) { entry ->
            RecoverCodeScreen(vmFor(entry), onBack = { nav.popBackStack() }, onVerified = {
                nav.navigate(Routes.RECOVER_PIN_NEW) { popUpTo(Routes.RECOVER_PIN_CODE) { inclusive = true } }
            })
        }
        composable(Routes.RECOVER_PIN_NEW) { entry ->
            val id = nav.graphEntry(entry, Routes.RECOVER_PIN_GRAPH).profileId()
            NewPinScreen(vmFor(entry), onBack = { nav.popBackStack() }, onSaved = {
                nav.navigate(Routes.pinLogin(id)) { popUpTo(Routes.PROFILE_SELECTOR) { inclusive = false } }
            })
        }
    }
}
