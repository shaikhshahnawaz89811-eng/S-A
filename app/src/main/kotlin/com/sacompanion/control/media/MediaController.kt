package com.sacompanion.control.media

import android.content.Context
import android.media.AudioManager
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class MediaRoute { SPEAKER, BLUETOOTH, AUTO }
enum class SAVoiceRoute { SPEAKER, EARBUDS, AUTO }

data class MediaState(
    val isPlaying: Boolean = false,
    val currentTitle: String = "",
    val currentUri: String = "",
    val mediaRoute: MediaRoute = MediaRoute.AUTO,
    val voiceRoute: SAVoiceRoute = SAVoiceRoute.AUTO,
    val volume: Int = 80
)

class MediaController(private val context: Context) {

    private var player: ExoPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _state = MutableStateFlow(MediaState())
    val state: StateFlow<MediaState> = _state.asStateFlow()

    fun initialize() {
        player = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.value = _state.value.copy(isPlaying = isPlaying)
                }
            })
        }
    }

    fun playUri(uri: String) {
        player?.apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            play()
        }
        _state.value = _state.value.copy(currentUri = uri, isPlaying = true)
    }

    fun playPause() {
        player?.apply {
            if (isPlaying) pause() else play()
        }
    }

    fun stop() {
        player?.stop()
        _state.value = _state.value.copy(isPlaying = false)
    }

    fun next() {
        player?.seekToNextMediaItem()
    }

    fun previous() {
        player?.seekToPreviousMediaItem()
    }

    fun setMediaRoute(route: MediaRoute) {
        _state.value = _state.value.copy(mediaRoute = route)
    }

    fun setSAVoiceRoute(route: SAVoiceRoute) {
        _state.value = _state.value.copy(voiceRoute = route)
    }

    fun isPlaying() = player?.isPlaying ?: false

    // ── Smart Audio Routing ───────────────────────────────────────────────────
    // Music plays on speaker; SA voice on earbuds (when connected)

    fun routeMusicToSpeaker() {
        audioManager.isSpeakerphoneOn = true
    }

    fun routeSAVoiceToEarbuds() {
        // When Bluetooth/wired headset connected, SA voice goes to earbuds
        val isBluetoothConnected = audioManager.isBluetoothScoOn ||
            audioManager.isBluetoothA2dpOn
        if (isBluetoothConnected) {
            audioManager.startBluetoothSco()
            audioManager.isBluetoothScoOn = true
        }
    }

    fun resetAudioRouting() {
        audioManager.isBluetoothScoOn = false
        audioManager.stopBluetoothSco()
        audioManager.isSpeakerphoneOn = false
    }

    fun handleCommand(command: String): Pair<Boolean, String> {
        val lower = command.lowercase()
        return when {
            lower.contains("music chala") || lower.contains("play music") || lower.contains("gaana chala") -> {
                // Open default music player
                val intent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
                    ?: context.packageManager.getLaunchIntentForPackage("com.google.android.music")
                    ?: context.packageManager.getLaunchIntentForPackage("com.sec.android.app.music")
                intent?.let {
                    it.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(it)
                }
                true to "Music chala raha hoon"
            }
            lower.contains("music band") || lower.contains("stop music") || lower.contains("pause") -> {
                stop()
                true to "Music rok diya"
            }
            lower.contains("next") || lower.contains("agla") -> {
                next()
                true to "Agla song"
            }
            lower.contains("previous") || lower.contains("pichla") -> {
                previous()
                true to "Pichla song"
            }
            else -> false to ""
        }
    }

    fun destroy() {
        player?.release()
        player = null
    }
}
