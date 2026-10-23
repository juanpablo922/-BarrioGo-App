package com.barriogo.app.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import android.widget.Toast
import com.barriogo.app.BarrioGoApp
import com.barriogo.app.data.auth.Destino
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
                // Con la ventana "¿Cómo quieres registrar?" abierta, el resaltado va sobre Registrar
                val dest = if (showRegisterSheet) BottomDest.REGISTRAR else when (currentRoute) {
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
            // Con el menú "¿Cómo quieres registrar?" abierto, la pantalla de fondo se ve desenfocada (Android 12+)
            NavHost(
                navController = nav,
                startDestination = Routes.SPLASH,
                modifier = if (showRegisterSheet) Modifier.blur(12.dp) else Modifier,
            ) {

                composable(Routes.SPLASH) {
                    SplashScreen(decide = {
                        val c = app.container
                        val ruta = if (c.authRepo.correoActual == null) {
                            // Sin cuenta de Firebase: solo la demo (datos de ejemplo en el celular) entra directo.
                            if (c.store.demo && c.store.signedIn && c.db.profileDao().count() > 0) Routes.PROFILE_SELECTOR else Routes.LOGIN
                        } else {
                            val inicio = c.authRepo.destinoInicial(c.store.tiendaId)
                            inicio.tiendaId?.let { c.store.tiendaId = it }
                            c.sincronizarPerfiles()
                            when (inicio.destino) {
                                Destino.INGRESAR -> Routes.LOGIN
                                Destino.COMPLETAR_REGISTRO -> Routes.REGISTER     // cuenta creada pero sin tienda
                                Destino.CREAR_CLAVE -> Routes.CREATE_PIN          // tienda sin perfiles
                                Destino.ELEGIR_PERFIL -> Routes.PROFILE_SELECTOR
                            }
                        }
                        nav.navigate(ruta) { popUpTo(Routes.SPLASH) { inclusive = true } }
                    })
                }

                composable(Routes.LOGIN) {
                    LoginScreen(
                        onLoggedIn = {
                            app.container.store.signedIn = true
                            // El Splash decide si falta la tienda, la clave o solo elegir perfil.
                            nav.navigate(Routes.SPLASH) { popUpTo(Routes.LOGIN) { inclusive = true } }
                        },
                        onGoRegister = { nav.navigate(Routes.REGISTER) },
                        onForgotPassword = { nav.navigate(Routes.RECUPERAR) },
                        onToggleTheme = {
                            val curr = app.container.store.darkTheme.value
                            app.container.store.setDark(!curr)
                        },
                    )
                }

                composable(Routes.RECUPERAR) {
                    RecuperarScreen(
                        onVolver = { nav.popBackStack() },
                        onListo = { nav.popBackStack(Routes.LOGIN, inclusive = false) },
                    )
                }

                composable(Routes.REGISTER) {
                    RegisterScreen(
                        onFinished = { nav.navigate(Routes.CREATE_PIN) },
                        onGoLogin = { nav.navigate(Routes.LOGIN) { popUpTo(Routes.REGISTER) { inclusive = true } } },
                    )
                }

                // R5 · "Crea tu clave" → R6 · "Crea tu perfil": un solo perfil, con esa clave.
                composable(Routes.CREATE_PIN) {
                    val vm = com.barriogo.app.ui.barrioViewModel { com.barriogo.app.ui.auth.AuthViewModel(it) }
                    CreatePinScreen { pin ->
                        vm.guardarClaveEnCurso(pin)
                        nav.navigate(Routes.CREATE_PROFILE) { popUpTo(Routes.CREATE_PIN) { inclusive = true } }
                    }
                }

                composable(Routes.CREATE_PROFILE) {
                    val vm = com.barriogo.app.ui.barrioViewModel { com.barriogo.app.ui.auth.AuthViewModel(it) }
                    val context = LocalContext.current
                    LaunchedEffect(Unit) {
                        // Sin clave no hay perfil: primero R5.
                        if (!vm.hayClaveEnCurso) nav.navigate(Routes.CREATE_PIN) { popUpTo(Routes.CREATE_PROFILE) { inclusive = true } }
                    }
                    val perfiles by vm.profiles.collectAsState(initial = emptyList())
                    val adminLleno = perfiles.count { it.isAdmin } >= com.barriogo.app.domain.CuposPerfiles.MAXIMO_ADMINISTRADORES
                    val ocupado by vm.ocupado.collectAsState()
                    CreateProfileScreen(adminLleno = adminLleno, cargando = ocupado) { name, role ->
                        vm.crearPerfil(
                            name,
                            role,
                            onDone = { nav.navigate(Routes.BIOMETRIC) { popUpTo(Routes.CREATE_PROFILE) { inclusive = true } } },
                            onError = { mensaje -> Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show() },
                        )
                    }
                }

                composable(Routes.BIOMETRIC) {
                    val vm = com.barriogo.app.ui.barrioViewModel { com.barriogo.app.ui.auth.AuthViewModel(it) }
                    BiometricScreen { enabled ->
                        vm.guardarHuella(enabled)
                        nav.navigate(Routes.PROFILE_SELECTOR) {
                            popUpTo(nav.graph.id) { inclusive = true }
                        }
                    }
                }

                composable(Routes.PROFILE_SELECTOR) {
                    ProfileSelectorScreen(
                        onPick = { id -> nav.navigate(Routes.pinLogin(id)) },
                        onCreateProfile = { nav.navigate(Routes.CREATE_PIN) },
                        onLogout = {
                            app.container.signOutOfStore()
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
