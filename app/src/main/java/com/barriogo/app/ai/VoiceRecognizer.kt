package com.barriogo.app.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Envoltorio delgado sobre [SpeechRecognizer] (paquete android.speech, parte de Android).
 * Aquí termina la "percepción": convierte audio en texto. Toda la interpretación (qué tipo de
 * movimiento es, qué campos tiene) la hace [MovementAgent], no esta clase.
 */
class VoiceRecognizer(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(onListening: () -> Unit, onResult: (String) -> Unit, onError: (String) -> Unit) {
        if (!isAvailable) { onError("Este celular no tiene reconocimiento de voz disponible."); return }
        recognizer?.destroy()
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-CO")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { onListening() }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                onResult(text)
            }
            override fun onError(error: Int) { onError(messageFor(error)) }
            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
        })
        r.startListening(intent)
    }

    fun stop() { recognizer?.stopListening() }
    fun release() { recognizer?.destroy(); recognizer = null }

    private fun messageFor(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se escuchó nada. Vuelve a intentar."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "El reconocimiento de voz necesita internet y no hay conexión. Puedes escribirlo a mano."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Falta el permiso del micrófono."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Espera un momento e intenta de nuevo."
        else -> "No se pudo escuchar. Intenta de nuevo o escríbelo a mano."
    }
}
