package com.barriogo.app.ai

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Envoltorio sobre CameraX + ML Kit Text Recognition v2 (escritura latina, en el dispositivo).
 * Aquí termina la "percepción" de la factura: toma la foto y devuelve el texto leído.
 * [InvoiceAgent] es quien interpreta ese texto (proveedor, fecha, conceptos, total).
 */
class InvoiceScanner {
    private var imageCapture: ImageCapture? = null
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun bind(context: Context, lifecycleOwner: LifecycleOwner, previewView: PreviewView, onReady: () -> Unit = {}) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val capture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
            imageCapture = capture
            try {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                onReady()
            } catch (_: Exception) { /* el celular no tiene cámara trasera disponible */ }
        }, ContextCompat.getMainExecutor(context))
    }

    /** Toma la foto, la pasa por OCR y entrega el texto plano leído (una línea por renglón de la factura). */
    fun captureAndRead(context: Context, onText: (String) -> Unit, onError: (String) -> Unit) {
        val capture = imageCapture ?: return onError("La cámara todavía no está lista")
        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                    val mediaImage = image.image
                    if (mediaImage == null) { image.close(); onError("No se pudo leer la imagen"); return }
                    val input = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
                    recognizer.process(input)
                        .addOnSuccessListener { result -> onText(result.text) }
                        .addOnFailureListener { onError("No se pudo leer el texto de la factura") }
                        .addOnCompleteListener { image.close() }
                }
                override fun onError(exception: ImageCaptureException) { onError("No se pudo tomar la foto") }
            },
        )
    }
}
