package com.barriogo.app.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.barriogo.app.ai.InvoiceScanner
import com.barriogo.app.ui.barrioViewModel
import com.barriogo.app.ui.components.BarrioTopBar
import com.barriogo.app.ui.components.SecondaryButton
import com.barriogo.app.ui.theme.Accent
import com.barriogo.app.ui.theme.OnAccent

/** C1/C2 · Escanear factura: cámara en vivo, botón de captura y lectura por OCR. */
@Composable
fun ScanInvoiceScreen(onBack: () -> Unit, onDone: () -> Unit, onWriteInstead: () -> Unit) {
    val vm = barrioViewModel { ScanViewModel(it) }
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanner = remember { InvoiceScanner() }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }

    if (s.phase == ScanPhase.REVIEW) {
        ReviewInvoiceScreen(draft = s.draft!!, onBack = onBack, onRetry = { vm.retry() }, onConfirm = { vm.confirm(onDone) })
        return
    }

    Column(Modifier.fillMaxSize()) {
        BarrioTopBar("Escanear factura", onBack = onBack)
        Box(Modifier.fillMaxSize()) {
            if (hasPermission) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { pv -> scanner.bind(ctx, lifecycleOwner, pv) }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("BarrioGo necesita la cámara para leer la factura.", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton("Dar permiso de cámara") { permLauncher.launch(Manifest.permission.CAMERA) }
                }
            }

            if (s.phase == ScanPhase.READING) {
                Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Accent)
                        Text("Leyendo la factura…", color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }
            if (s.phase == ScanPhase.ERROR) {
                Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text(s.error ?: "No se pudo leer la factura", color = androidx.compose.ui.graphics.Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        SecondaryButton("Volver a intentar") { vm.retry() }
                        Spacer(Modifier.height(8.dp))
                        SecondaryButton("Escribir el gasto a mano") { onWriteInstead() }
                    }
                }
            }

            if (hasPermission && s.phase == ScanPhase.CAMERA) {
                Box(
                    Modifier.fillMaxWidth().padding(bottom = 32.dp).align(Alignment.BottomCenter),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(76.dp).clip(CircleShape).background(Accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.material3.IconButton(
                            onClick = {
                                vm.onReading()
                                scanner.captureAndRead(context, onText = { vm.onOcrText(it) }, onError = { vm.onOcrError(it) })
                            },
                            modifier = Modifier.size(76.dp),
                        ) { androidx.compose.material3.Icon(Icons.Filled.CameraAlt, contentDescription = "Tomar foto", tint = OnAccent) }
                    }
                }
            }
        }
    }
}
