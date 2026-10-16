package com.barriogo.app

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.barriogo.app.ui.nav.BarrioGoNavGraph
import com.barriogo.app.ui.theme.BarrioGoTheme

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext

/** FragmentActivity (en vez de ComponentActivity) para poder mostrar el lector de huella. */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { BarrioGoRoot() }
    }
}

/** Raíz de la interfaz: tema + navegación. Nombrada distinto de la clase Application (BarrioGoApp). */
@Composable
private fun BarrioGoRoot() {
    val app = LocalContext.current.applicationContext as BarrioGoApp
    val isDark by app.container.store.darkTheme.collectAsState()
    BarrioGoTheme(darkTheme = isDark) {
        Surface(modifier = Modifier.fillMaxSize()) {
            BarrioGoNavGraph()
        }
    }
}
