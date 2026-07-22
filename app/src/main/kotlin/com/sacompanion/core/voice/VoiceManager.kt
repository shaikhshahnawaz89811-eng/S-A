package com.sacompanion.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceState {
    IDLE, LISTENING, PROCESSING, SPEAKING, ERROR
}

data class VoiceResult(
    val text: String,
    val confidence: Float,
    val isWakeWord: Boolean = false
)

class VoiceManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var continuousMode = false

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _partialResult = MutableStateFlow("")
    val partialResult: StateFlow<String> = _partialResult.asStateFlow()

    private var onResult: ((VoiceResult) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    companion object {
        const val WAKE_WORD = "SA"
        val WAKE_WORD_VARIANTS = listOf("SA", "sa", "Sa", "S.A", "S A", "एसए", "एस ए")
    }

    fun initialize() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            createRecognizer()
        }
    }

    private fun createRecognizer() {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createListener())
        }
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _voiceState.value = VoiceState.LISTENING
        }

        override fun onBeginningOfSpeech() {
            _partialResult.value = ""
        }

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _voiceState.value = VoiceState.PROCESSING
        }

        override fun onError(error: Int) {
            isListening = false
            _voiceState.value = VoiceState.IDLE

            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Unknown recognition error"
            }

            onError?.invoke(errorMsg)

            // Auto restart in continuous mode (except for fatal errors)
            if (continuousMode && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (continuousMode) startListening()
                }, 1000)
            }
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            _voiceState.value = VoiceState.IDLE

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

            if (!matches.isNullOrEmpty()) {
                val text = matches[0]
                val confidence = confidences?.getOrNull(0) ?: 0.8f

                val voiceResult = VoiceResult(
                    text = text,
                    confidence = confidence,
                    isWakeWord = containsWakeWord(text)
                )
                onResult?.invoke(voiceResult)
            }

            // Restart in continuous mode
            if (continuousMode) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (continuousMode) startListening()
                }, 500)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return
            _partialResult.value = partial
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startListening(continuous: Boolean = false) {
        if (isListening) return
        this.continuousMode = continuous

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_ALSO_RETURN_RECOGNITION_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
        }

        try {
            speechRecognizer?.startListening(intent)
            isListening = true
            _voiceState.value = VoiceState.LISTENING
        } catch (e: Exception) {
            onError?.invoke("Failed to start listening: ${e.message}")
        }
    }

    fun stopListening() {
        continuousMode = false
        isListening = false
        speechRecognizer?.stopListening()
        _voiceState.value = VoiceState.IDLE
    }

    fun setOnResultListener(listener: (VoiceResult) -> Unit) {
        onResult = listener
    }

    fun setOnErrorListener(listener: (String) -> Unit) {
        onError = listener
    }

    fun setSpeakingState() {
        _voiceState.value = VoiceState.SPEAKING
    }

    fun setIdleState() {
        _voiceState.value = VoiceState.IDLE
    }

    private fun containsWakeWord(text: String): Boolean {
        val upperText = text.uppercase(Locale.ROOT)
        return WAKE_WORD_VARIANTS.any { variant ->
            upperText.contains(variant.uppercase(Locale.ROOT))
        }
    }

    fun extractCommandAfterWakeWord(text: String): String {
        val upperText = text.uppercase(Locale.ROOT)
        WAKE_WORD_VARIANTS.forEach { variant ->
            val idx = upperText.indexOf(variant.uppercase(Locale.ROOT))
            if (idx >= 0) {
                return text.substring(idx + variant.length).trim()
            }
        }
        return text
    }

    fun isAvailable() = SpeechRecognizer.isRecognitionAvailable(context)

    fun destroy() {
        continuousMode = false
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
