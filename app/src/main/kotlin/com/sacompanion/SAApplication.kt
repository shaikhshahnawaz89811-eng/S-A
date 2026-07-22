package com.sacompanion

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.sacompanion.database.SADatabase
import com.sacompanion.core.ai.GroqAIClient
import com.sacompanion.core.memory.MemoryManager
import com.sacompanion.core.tts.TTSManager

class SAApplication : Application() {

    lateinit var database: SADatabase
        private set

    lateinit var memoryManager: MemoryManager
        private set

    lateinit var ttsManager: TTSManager
        private set

    lateinit var groqAIClient: GroqAIClient
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        initDatabase()
        initManagers()
        createNotificationChannels()
    }

    private fun initDatabase() {
        database = SADatabase.getInstance(this)
    }

    private fun initManagers() {
        memoryManager = MemoryManager(this, database)
        ttsManager = TTSManager(this)
        groqAIClient = GroqAIClient()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Assistant Service Channel
            val assistantChannel = NotificationChannel(
                CHANNEL_ASSISTANT,
                "SA Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "SA Companion background assistant service"
                setShowBadge(false)
            }

            // Alerts Channel
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "SA Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "SA Companion security and important alerts"
            }

            // Media Channel
            val mediaChannel = NotificationChannel(
                CHANNEL_MEDIA,
                "SA Media",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "SA Companion media playback"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(
                listOf(assistantChannel, alertsChannel, mediaChannel)
            )
        }
    }

    companion object {
        lateinit var instance: SAApplication
            private set

        const val CHANNEL_ASSISTANT = "sa_assistant_channel"
        const val CHANNEL_ALERTS = "sa_alerts_channel"
        const val CHANNEL_MEDIA = "sa_media_channel"
        const val NOTIFICATION_ID_SERVICE = 1001
        const val NOTIFICATION_ID_FLOATING = 1002
    }
}
