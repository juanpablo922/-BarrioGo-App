package com.barriogo.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.barriogo.app.ui.nav.BarrioGoNavGraph
import com.barriogo.app.ui.theme.BarrioGoTheme

/** FragmentActivity (que extiende ComponentActivity) porque BiometricPrompt la necesita para la huella. */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La app es de fondo verde oscuro: íconos claros en las barras del sistema.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent { BarrioGoRoot() }
    }
}

/** Raíz de la interfaz: tema + navegación. Nombrada distinto de la clase Application (BarrioGoApp). */
@Composable
private fun BarrioGoRoot() {
    BarrioGoTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize()) {
            BarrioGoNavGraph()
        }
    }
}
