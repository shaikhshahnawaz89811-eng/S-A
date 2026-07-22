package com.sacompanion.core.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

enum class TTSEngine { ANDROID, PIPER, COQUI }
enum class TTSState { IDLE, SPEAKING, ERROR }

class TTSManager(private val context: Context) {

        private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var selectedEngine = TTSEngine.ANDROID
    private var isInitialized = false
    private var selectedEngine = TTSEngine.ANDROID
    private var isSpeaking = false

        private val _ttsState = MutableStateFlow(TTSState.IDLE)
    val ttsState: StateFlow<TTSState> = _ttsState.asStateFlow()
    val ttsState: StateFlow<TTSState> = _ttsState.asStateFlow()

    private val speechQueue = ArrayDeque<String>()
    private var onSpeechDone: (() -> Unit)? = null
    private var onSpeechStart: (() -> Unit)? = null

    fun initialize(onReady: (() -> Unit)? = null) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                configureTTS()
                onReady?.invoke()
            }
        }
    }

    private fun configureTTS() {
        tts?.apply {
            // Try Hindi first, fallback to English
            val hindiLocale = Locale("hi", "IN")
            val result = setLanguage(hindiLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                setLanguage(Locale.ENGLISH)
            }

            // Female voice — softer, natural speaking
            setSpeechRate(0.9f)   // Slightly slower = more natural
            setPitch(1.1f)        // Slightly higher pitch for female voice

            // Try to select a female voice if available
            voices?.filter { voice ->
                !voice.isNetworkConnectionRequired &&
                voice.name.contains("female", ignoreCase = true)
            }?.minByOrNull { it.quality }?.let { femaleVoice ->
                setVoice(femaleVoice)
            }

            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _ttsState.value = TTSState.SPEAKING
                    onSpeechStart?.invoke()
                }

                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    _ttsState.value = TTSState.IDLE
                    onSpeechDone?.invoke()
                    processNextInQueue()
                }

                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    _ttsState.value = TTSState.ERROR
                    processNextInQueue()
                }
            })
        }
    }

    fun speak(text: String, immediate: Boolean = false) {
        if (!isInitialized) {
            initialize { speak(text, immediate) }
            return
        }

        val cleanText = preprocessText(text)

        if (immediate) {
            speechQueue.clear()
            tts?.stop()
            isSpeaking = false
        }

        if (isSpeaking && !immediate) {
            speechQueue.addLast(cleanText)
        } else {
            speakNow(cleanText)
        }
    }

    private fun speakNow(text: String) {
        if (text.isBlank()) return
        isSpeaking = true
        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private fun processNextInQueue() {
        if (speechQueue.isNotEmpty()) {
            speakNow(speechQueue.removeFirst())
        }
    }

    fun stopSpeaking() {
        speechQueue.clear()
        tts?.stop()
        isSpeaking = false
        _ttsState.value = TTSState.IDLE
    }

    fun isSpeaking() = isSpeaking

    fun setOnSpeechDoneListener(listener: () -> Unit) {
        onSpeechDone = listener
    }

    fun setOnSpeechStartListener(listener: () -> Unit) {
        onSpeechStart = listener
    }

    fun setEngine(engine: TTSEngine) {
        selectedEngine = engine
        // For Piper/Coqui — future integration point with native library
    }

    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }

    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
    }

    fun setLanguage(locale: Locale) {
        tts?.setLanguage(locale)
    }

    /**
     * Preprocess text for better TTS rendering:
     * - Remove action tags ([ACTION: ...])
     * - Expand common abbreviations
     * - Handle Hindi/Hinglish naturally
     */
    private fun preprocessText(text: String): String {
        return text
            .replace(Regex("\\[ACTION:[^\\]]*\\]"), "")
            .replace("SA", "एस ए")
            .replace("OK", "ठीक है")
            .replace("ok", "ठीक है")
            .replace("%", "प्रतिशत")
            .trim()
    }

    fun isInitialized() = isInitialized

    fun getAvailableEngines(): List<TTSEngine> = listOf(TTSEngine.ANDROID)

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
