package com.sacompanion.service.background

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.sacompanion.MainActivity
import com.sacompanion.R
import com.sacompanion.SAApplication
import com.sacompanion.control.media.MediaController
import com.sacompanion.control.phone.PhoneController
import com.sacompanion.core.ai.CommandExtractor
import com.sacompanion.core.ai.GroqAIClient
import com.sacompanion.core.memory.MemoryManager
import com.sacompanion.core.tts.TTSManager
import com.sacompanion.core.voice.VoiceManager
import com.sacompanion.core.voice.VoiceState
import com.sacompanion.core.voice.WakeWordDetector
import kotlinx.coroutines.*

class SAAssistantService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var voiceManager: VoiceManager
    private lateinit var ttsManager: TTSManager
    private lateinit var groqAIClient: GroqAIClient
    private lateinit var memoryManager: MemoryManager
    private lateinit var phoneController: PhoneController
    private lateinit var mediaController: MediaController
    private lateinit var wakeWordDetector: WakeWordDetector

    private var wakeLock: PowerManager.WakeLock? = null
    private var isListeningForWakeWord = false
    private var isActiveSession = false
    private var currentApiKey = ""

    companion object {
        const val ACTION_START = "com.sacompanion.START"
        const val ACTION_STOP = "com.sacompanion.STOP"
        const val ACTION_WAKE = "com.sacompanion.WAKE"
        const val ACTION_SEND_MESSAGE = "com.sacompanion.SEND_MESSAGE"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_API_KEY = "api_key"

        // Broadcast for UI updates
        const val BROADCAST_STATE = "com.sacompanion.STATE_UPDATE"
        const val EXTRA_STATE = "state"
        const val EXTRA_RESPONSE = "response"
    }

    override fun onCreate() {
        super.onCreate()
        val app = application as SAApplication
        voiceManager = VoiceManager(this)
        ttsManager = app.ttsManager
        groqAIClient = app.groqAIClient
        memoryManager = app.memoryManager
        phoneController = PhoneController(this)
        mediaController = MediaController(this)
        wakeWordDetector = WakeWordDetector()

        voiceManager.initialize()
        mediaController.initialize()
        setupVoiceListeners()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(SAApplication.NOTIFICATION_ID_SERVICE, buildNotification("SA Core Online"))

        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_SEND_MESSAGE -> {
                val msg = intent.getStringExtra(EXTRA_MESSAGE) ?: return START_STICKY
                val key = intent.getStringExtra(EXTRA_API_KEY) ?: ""
                if (key.isNotEmpty()) currentApiKey = key
                processUserInput(msg)
            }
            else -> {
                val key = intent?.getStringExtra(EXTRA_API_KEY) ?: ""
                if (key.isNotEmpty()) currentApiKey = key
                startWakeWordListening()
            }
        }

        acquireWakeLock()
        return START_STICKY
    }

    private fun setupVoiceListeners() {
        voiceManager.setOnResultListener { result ->
            if (isListeningForWakeWord) {
                val match = wakeWordDetector.detect(result.text)
                if (match.matched) {
                    isActiveSession = true
                    isListeningForWakeWord = false
                    val command = match.command.trim()
                    if (command.isNotEmpty()) {
                        processUserInput(command)
                    } else {
                        ttsManager.speak("Haan Boss, bataiye?")
                        serviceScope.launch {
                            delay(500)
                            startCommandListening()
                        }
                    }
                }
            } else if (isActiveSession) {
                processUserInput(result.text)
            }
        }

        voiceManager.setOnErrorListener { error ->
            if (isListeningForWakeWord || isActiveSession) {
                serviceScope.launch {
                    delay(1000)
                    if (isListeningForWakeWord) startWakeWordListening()
                    else if (isActiveSession) startCommandListening()
                }
            }
        }
    }

    private fun startWakeWordListening() {
        isListeningForWakeWord = true
        isActiveSession = false
        updateNotification("Listening for wake word...")
        voiceManager.startListening(continuous = false)
        serviceScope.launch {
            delay(8000) // 8s window, then restart
            if (isListeningForWakeWord) {
                voiceManager.stopListening()
                delay(300)
                startWakeWordListening()
            }
        }
    }

    private fun startCommandListening() {
        isListeningForWakeWord = false
        updateNotification("Listening...")
        voiceManager.startListening(continuous = false)
    }

    private fun processUserInput(input: String) {
        broadcastState("PROCESSING", "")
        updateNotification("Processing: $input")

        serviceScope.launch {
            // 1. Check phone commands first
            val phoneResult = phoneController.handleCommand(input)
            if (phoneResult.executed) {
                ttsManager.speak(phoneResult.response)
                broadcastState("RESPONSE", phoneResult.response)
                updateNotification("SA Core Online")
                scheduleReturnToWakeWord()
                return@launch
            }

            // 2. Check media commands
            val (mediaHandled, mediaResponse) = mediaController.handleCommand(input)
            if (mediaHandled) {
                ttsManager.speak(mediaResponse)
                broadcastState("RESPONSE", mediaResponse)
                updateNotification("SA Core Online")
                scheduleReturnToWakeWord()
                return@launch
            }

            // 3. AI Brain
            val context = memoryManager.buildContextString()
            val enrichedInput = if (context.isNotEmpty()) "$input\n\n[Context]\n$context" else input

            val key = currentApiKey.ifEmpty { com.sacompanion.BuildConfig.GROQ_API_KEY }
            val result = groqAIClient.chat(enrichedInput, key)

            result.fold(
                onSuccess = { response ->
                    // Extract and execute any action commands
                    val commands = CommandExtractor.extract(response)
                    commands.forEach { cmd ->
                        executeActionCommand(cmd.action, cmd.parameter)
                    }
                    val cleanResponse = CommandExtractor.stripActions(response)
                    memoryManager.saveConversation(input, cleanResponse)
                    broadcastState("RESPONSE", cleanResponse)
                    ttsManager.speak(cleanResponse)
                    updateNotification("SA Core Online")
                },
                onFailure = { error ->
                    val errorMsg = "Maafi kijiye, abhi AI se connect nahi ho pa raha. ${error.message}"
                    broadcastState("ERROR", errorMsg)
                    ttsManager.speak("Maafi kijiye, kuch technical problem hai.")
                    updateNotification("SA Core Online")
                }
            )
            scheduleReturnToWakeWord()
        }
    }

    private fun executeActionCommand(action: String, param: String?) {
        when (action) {
            "battery_check" -> phoneController.getBatteryStatusHindi()
            "play_music" -> mediaController.handleCommand("music chala")
            "torch_on" -> phoneController.setTorch(true)
            "torch_off" -> phoneController.setTorch(false)
            "volume_up" -> phoneController.increaseVolume()
            "volume_down" -> phoneController.decreaseVolume()
            "open_camera" -> phoneController.openCamera()
            "open_settings" -> phoneController.openSettings()
            "open_app" -> param?.let { phoneController.openAppByName(it) }
        }
    }

    private fun scheduleReturnToWakeWord() {
        serviceScope.launch {
            delay(3000) // Wait for TTS to finish
            isActiveSession = false
            startWakeWordListening()
        }
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SAApplication.CHANNEL_ASSISTANT)
            .setContentTitle("SA Companion")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(SAApplication.NOTIFICATION_ID_SERVICE, notification)
    }

    private fun broadcastState(state: String, response: String) {
        val intent = Intent(BROADCAST_STATE).apply {
            putExtra(EXTRA_STATE, state)
            putExtra(EXTRA_RESPONSE, response)
        }
        sendBroadcast(intent)
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SACompanion:AssistantWakeLock"
        ).apply { acquire(10 * 60 * 1000L) } // 10 min max
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        voiceManager.destroy()
        mediaController.destroy()
        wakeLock?.release()
        super.onDestroy()
    }
}
