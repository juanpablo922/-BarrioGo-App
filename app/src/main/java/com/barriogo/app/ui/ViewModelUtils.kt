package com.barriogo.app.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.barriogo.app.AppContainer
import com.barriogo.app.BarrioGoApp

/**
 * Crea un ViewModel pasándole el [AppContainer] de la aplicación, sin necesidad de Hilt.
 * Uso: `val vm = barrioViewModel { HomeViewModel(it) }`
 */
@Composable
inline fun <reified VM : ViewModel> barrioViewModel(crossinline creator: (AppContainer) -> VM): VM {
    val app = LocalContext.current.applicationContext as BarrioGoApp
    return viewModel(factory = viewModelFactory { initializer { creator(app.container) } })
}
