package com.barriogo.app.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.barriogo.app.BarrioGoApp
import com.barriogo.app.domain.MovementType
import com.barriogo.app.ui.auth.*
import com.barriogo.app.ui.cash.CashScreen
import com.barriogo.app.ui.cash.OpenCashScreen
import com.barriogo.app.ui.components.BarrioBottomBar
import com.barriogo.app.ui.components.BottomDest
import com.barriogo.app.ui.components.RegisterChooserSheet
import com.barriogo.app.ui.expenses.ExpensesScreen
import com.barriogo.app.ui.expenses.RegisterExpenseScreen
import com.barriogo.app.ui.fiados.FiadosScreen
import com.barriogo.app.ui.fiados.NewFiadoScreen
import com.barriogo.app.ui.fiados.RegisterPaymentScreen
import com.barriogo.app.ui.home.HomeScreen
import com.barriogo.app.ui.movements.MovementsBookScreen
import com.barriogo.app.ui.movements.RegisterMovementScreen
import com.barriogo.app.ui.scan.ScanInvoiceScreen
import com.barriogo.app.ui.voice.VoiceCaptureScreen

private val BOTTOM_ROUTES = setOf(Routes.HOME, Routes.MOVEMENTS_BOOK, Routes.REGISTER_MOVEMENT, Routes.CASH)

/** Grafo de navegación completo con el flujo exacto del prototipo Figma. */
@Composable
fun BarrioGoNavGraph() {
    val nav = rememberNavController()
    val app = LocalContext.current.applicationContext as BarrioGoApp
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    var showRegisterSheet by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (currentRoute in BOTTOM_ROUTES) {
                val dest = when (currentRoute) {
                    Routes.HOME -> BottomDest.INICIO
                    Routes.MOVEMENTS_BOOK -> BottomDest.MOVIMIENTOS
                    Routes.REGISTER_MOVEMENT -> BottomDest.REGISTRAR
                    else -> BottomDest.CAJA
                }
                BarrioBottomBar(
                    current = dest,
                    onSelect = { selected ->
                        val route = when (selected) {
                            BottomDest.INICIO -> Routes.HOME
                            BottomDest.MOVIMIENTOS -> Routes.MOVEMENTS_BOOK
                            BottomDest.REGISTRAR -> Routes.REGISTER_MOVEMENT
                            BottomDest.CAJA -> Routes.CASH
                        }
                        nav.navigate(route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenRegisterMenu = {
                        showRegisterSheet = true
                    }
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController = nav, startDestination = Routes.SPLASH) {

                composable(Routes.SPLASH) {
                    val profiles by app.container.repo.profiles.collectAsState(initial = null)
                    if (profiles != null) {
                        SplashScreen(
                            isRegistered = app.container.store.registered,
                            isSignedIn = app.container.store.signedIn,
                            hasProfiles = profiles!!.isNotEmpty(),
                            onGoRegister = { nav.navigate(Routes.REGISTER) { popUpTo(Routes.SPLASH) { inclusive = true } } },
                            onGoLogin = { nav.navigate(Routes.LOGIN) { popUpTo(Routes.SPLASH) { inclusive = true } } },
                            onGoProfileSelector = { nav.navigate(Routes.PROFILE_SELECTOR) { popUpTo(Routes.SPLASH) { inclusive = true } } },
                        )
                    }
                }

                composable(Routes.LOGIN) {
                    LoginScreen(
                        onLoggedIn = {
                            app.container.store.signedIn = true
                            nav.navigate(Routes.PROFILE_SELECTOR) { popUpTo(Routes.LOGIN) { inclusive = true } }
                        },
                        onGoRegister = { nav.navigate(Routes.REGISTER) },
                        onToggleTheme = {
                            val curr = app.container.store.darkTheme.value
                            app.container.store.setDark(!curr)
                        },
                    )
                }

                composable(Routes.REGISTER) {
                    RegisterScreen(
                        onFinished = { nav.navigate(Routes.CREATE_PIN) },
                        onGoLogin = { nav.navigate(Routes.LOGIN) { popUpTo(Routes.REGISTER) { inclusive = true } } },
                    )
                }

                composable(Routes.CREATE_PIN) {
                    val vm = com.barriogo.app.ui.barrioViewModel { com.barriogo.app.ui.auth.AuthViewModel(it) }
                    CreatePinScreen { pin ->
                        vm.createAdminProfile(pin) {
                            nav.navigate(Routes.CREATE_PROFILE) { popUpTo(Routes.CREATE_PIN) { inclusive = true } }
                        }
                    }
                }

                composable(Routes.CREATE_PROFILE) {
                    val vm = com.barriogo.app.ui.barrioViewModel { com.barriogo.app.ui.auth.AuthViewModel(it) }
                    CreateProfileScreen { name, role ->
                        vm.createProfile(name, role, "1234") {
                            nav.navigate(Routes.BIOMETRIC)
                        }
                    }
                }

                composable(Routes.BIOMETRIC) {
                    BiometricScreen { enabled ->
                        nav.navigate(Routes.PROFILE_SELECTOR) {
                            popUpTo(Routes.REGISTER) { inclusive = true }
                        }
                    }
                }

                composable(Routes.PROFILE_SELECTOR) {
                    ProfileSelectorScreen(
                        onPick = { id -> nav.navigate(Routes.pinLogin(id)) },
                        onCreateProfile = { nav.navigate(Routes.CREATE_PROFILE) },
                        onLogout = {
                            app.container.store.signedIn = false
                            nav.navigate(Routes.LOGIN) { popUpTo(Routes.PROFILE_SELECTOR) { inclusive = true } }
                        }
                    )
                }

                composable(
                    Routes.PIN_LOGIN,
                    arguments = listOf(navArgument("profileId") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("profileId") ?: 0L
                    PinLoginScreen(
                        profileId = id,
                        onSuccess = { nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } } },
                        onBack = { nav.popBackStack() },
                    )
                }

                composable(Routes.HOME) {
                    HomeScreen(
                        onSeeAllMovements = { nav.navigate(Routes.MOVEMENTS_BOOK) },
                        onGoFiados = { nav.navigate(Routes.FIADOS) },
                        onSwitchProfile = { nav.navigate(Routes.PROFILE_SELECTOR) },
                        onEditCash = { nav.navigate(Routes.CASH) }
                    )
                }

                composable(Routes.CASH) {
                    CashScreen(
                        onOpenCash = { nav.navigate(Routes.OPEN_CASH) },
                        onSeeAllMovements = { nav.navigate(Routes.MOVEMENTS_BOOK) }
                    )
                }

                composable(Routes.OPEN_CASH) {
                    OpenCashScreen(onOpened = { nav.popBackStack() })
                }

                composable(Routes.REGISTER_MOVEMENT) {
                    RegisterMovementScreen(
                        onSaved = { nav.popBackStack(Routes.HOME, false) },
                        onGoVoice = { mode -> nav.navigate(Routes.voiceCapture(mode.name)) },
                        onGoScan = { nav.navigate(Routes.SCAN_INVOICE) },
                        onBack = { nav.popBackStack() }
                    )
                }

                composable(Routes.MOVEMENTS_BOOK) {
                    MovementsBookScreen(onBack = { nav.popBackStack() })
                }

                composable(Routes.EXPENSES) {
                    ExpensesScreen(onBack = { nav.popBackStack() }, onNewExpense = { nav.navigate(Routes.REGISTER_EXPENSE) })
                }

                composable(Routes.REGISTER_EXPENSE) {
                    RegisterExpenseScreen(onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
                }

                composable(Routes.FIADOS) {
                    FiadosScreen(
                        onNewFiado = { nav.navigate(Routes.NEW_FIADO) },
                        onOpenClient = { id -> nav.navigate(Routes.registerPayment(id)) },
                        onBack = { nav.popBackStack() }
                    )
                }

                composable(Routes.NEW_FIADO) {
                    NewFiadoScreen(onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
                }

                composable(
                    Routes.REGISTER_PAYMENT,
                    arguments = listOf(navArgument("clientId") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("clientId") ?: 0L
                    RegisterPaymentScreen(clientId = id, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
                }

                composable(
                    Routes.VOICE_CAPTURE,
                    arguments = listOf(navArgument("mode") { type = NavType.StringType }),
                ) { entry ->
                    val mode = MovementType.valueOf(entry.arguments?.getString("mode") ?: MovementType.VENTA.name)
                    VoiceCaptureScreen(
                        initialMode = mode,
                        onBack = { nav.popBackStack() },
                        onConfirmed = { nav.popBackStack(Routes.HOME, false) },
                        onWriteInstead = { nav.navigate(Routes.REGISTER_MOVEMENT) { popUpTo(Routes.VOICE_CAPTURE) { inclusive = true } } },
                    )
                }

                composable(Routes.SCAN_INVOICE) {
                    ScanInvoiceScreen(
                        onBack = { nav.popBackStack() },
                        onDone = { nav.popBackStack(Routes.HOME, false) },
                        onWriteInstead = { nav.navigate(Routes.REGISTER_EXPENSE) { popUpTo(Routes.SCAN_INVOICE) { inclusive = true } } },
                    )
                }
            }

            // Bottom sheet modal for "+ Registrar" options
            if (showRegisterSheet) {
                RegisterChooserSheet(
                    onDismiss = { showRegisterSheet = false },
                    onVoice = { nav.navigate(Routes.voiceCapture("VENTA")) },
                    onScan = { nav.navigate(Routes.SCAN_INVOICE) },
                    onManual = { nav.navigate(Routes.REGISTER_MOVEMENT) }
                )
            }
        }
    }
}
