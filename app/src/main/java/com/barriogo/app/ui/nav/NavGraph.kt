package com.barriogo.app.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.barriogo.app.ui.auth.CreatePinScreen
import com.barriogo.app.ui.auth.CreateProfileScreen
import com.barriogo.app.ui.auth.LoginScreen
import com.barriogo.app.ui.auth.PinLoginScreen
import com.barriogo.app.ui.auth.ProfileSelectorScreen
import com.barriogo.app.ui.auth.RegisterScreen
import com.barriogo.app.ui.auth.SplashScreen
import com.barriogo.app.ui.cash.CashScreen
import com.barriogo.app.ui.cash.OpenCashScreen
import com.barriogo.app.ui.components.BarrioBottomBar
import com.barriogo.app.ui.components.BottomDest
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

/** Grafo de navegación completo: acceso, núcleo del negocio y los dos flujos de IA (voz y escáner). */
@Composable
fun BarrioGoNavGraph() {
    val nav = rememberNavController()
    val app = LocalContext.current.applicationContext as BarrioGoApp
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in BOTTOM_ROUTES) {
                val dest = when (currentRoute) {
                    Routes.HOME -> BottomDest.INICIO
                    Routes.MOVEMENTS_BOOK -> BottomDest.MOVIMIENTOS
                    Routes.REGISTER_MOVEMENT -> BottomDest.REGISTRAR
                    else -> BottomDest.CAJA
                }
                BarrioBottomBar(current = dest) { selected ->
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
                }
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
                        onLoggedIn = { nav.navigate(Routes.PROFILE_SELECTOR) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                        onGoRegister = { nav.navigate(Routes.REGISTER) },
                    )
                }
                composable(Routes.REGISTER) {
                    RegisterScreen(onFinished = { nav.navigate(Routes.CREATE_PIN) })
                }
                composable(Routes.CREATE_PIN) {
                    val vm = com.barriogo.app.ui.barrioViewModel { com.barriogo.app.ui.auth.AuthViewModel(it) }
                    CreatePinScreen(subtitle = "Lo usarás para entrar a la tienda cada día.") { pin ->
                        vm.createAdminProfile(pin) {
                            nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
                        }
                    }
                }
                composable(Routes.CREATE_PROFILE) {
                    CreateProfileScreen(onDone = { nav.popBackStack() })
                }
                composable(Routes.PROFILE_SELECTOR) {
                    ProfileSelectorScreen(
                        onPick = { id -> nav.navigate(Routes.pinLogin(id)) },
                        onCreateProfile = { nav.navigate(Routes.CREATE_PROFILE) },
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
                    )
                }

                composable(Routes.CASH) {
                    CashScreen(onOpenCash = { nav.navigate(Routes.OPEN_CASH) }, onSeeAllMovements = { nav.navigate(Routes.MOVEMENTS_BOOK) })
                }
                composable(Routes.OPEN_CASH) {
                    OpenCashScreen(onOpened = { nav.popBackStack() })
                }

                composable(Routes.REGISTER_MOVEMENT) {
                    RegisterMovementScreen(
                        onSaved = { nav.popBackStack(Routes.HOME, false) },
                        onGoVoice = { mode -> nav.navigate(Routes.voiceCapture(mode.name)) },
                        onGoScan = { nav.navigate(Routes.SCAN_INVOICE) },
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
                        onWriteInstead = { nav.popBackStack() },
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
        }
    }
}
