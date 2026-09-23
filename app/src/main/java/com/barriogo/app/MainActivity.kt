package com.barriogo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.barriogo.app.ui.nav.BarrioGoNavGraph
import com.barriogo.app.ui.theme.BarrioGoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
