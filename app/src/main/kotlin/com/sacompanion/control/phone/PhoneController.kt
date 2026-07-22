package com.sacompanion.control.phone

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class PhoneController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var isTorchOn = false

    // ── Battery ───────────────────────────────────────────────────────────────

    fun getBatteryLevel(): Int {
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun getBatteryStatus(): String {
        val level = getBatteryLevel()
        val isCharging = batteryManager.isCharging
        val chargingStr = if (isCharging) ", charging" else ""
        return "Battery is at $level percent$chargingStr"
    }

    fun getBatteryStatusHindi(): String {
        val level = getBatteryLevel()
        val isCharging = batteryManager.isCharging
        val chargingStr = if (isCharging) ", charge ho rahi hai" else ""
        return "Battery $level percent hai$chargingStr"
    }

    // ── Time & Date ───────────────────────────────────────────────────────────

    fun getCurrentTime(): String {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val min = cal.get(Calendar.MINUTE)
        val period = if (hour < 12) "AM" else "PM"
        val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        return "%d:%02d %s".format(hour12, min, period)
    }

    fun getCurrentTimeHindi(): String {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val min = cal.get(Calendar.MINUTE)
        val period = when {
            hour < 6 -> "raat ke"
            hour < 12 -> "subah ke"
            hour < 17 -> "dopahar ke"
            hour < 20 -> "shaam ke"
            else -> "raat ke"
        }
        val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        return "Abhi $period $hour12 baj kar $min minute hue hain"
    }

    // ── Volume ────────────────────────────────────────────────────────────────

    fun getVolume(): Int {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return (current * 100 / max)
    }

    fun setVolume(percent: Int) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (percent.coerceIn(0, 100) * max / 100)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }

    fun increaseVolume() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_SHOW_UI
        )
    }

    fun decreaseVolume() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
    }

    fun muteVolume() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_MUTE,
            AudioManager.FLAG_SHOW_UI
        )
    }

    // ── Torch / Flashlight ────────────────────────────────────────────────────

    fun toggleTorch(): Boolean {
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return false
            isTorchOn = !isTorchOn
            cameraManager.setTorchMode(cameraId, isTorchOn)
            isTorchOn
        } catch (e: CameraAccessException) {
            false
        }
    }

    fun setTorch(on: Boolean) {
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            cameraManager.setTorchMode(cameraId, on)
            isTorchOn = on
        } catch (e: CameraAccessException) {
            // Camera not available
        }
    }

    fun isTorchOn() = isTorchOn

    // ── Brightness ────────────────────────────────────────────────────────────

    fun getBrightness(): Int {
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Settings.SettingNotFoundException) {
            128
        }
    }

    fun setBrightness(level: Int) {
        // Requires WRITE_SETTINGS permission — request via Settings.ACTION_MANAGE_WRITE_SETTINGS
        try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                level.coerceIn(0, 255)
            )
        } catch (e: SecurityException) {
            // Need WRITE_SETTINGS permission
        }
    }

    // ── Camera ────────────────────────────────────────────────────────────────

    fun openCamera() {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Camera app not available
        }
    }

    // ── App Launcher ──────────────────────────────────────────────────────────

    fun openApp(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (intent != null) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            true
        } else {
            false
        }
    }

    fun openAppByName(appName: String): Boolean {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(0)
        val app = installedApps.firstOrNull { info ->
            pm.getApplicationLabel(info).toString().equals(appName, ignoreCase = true)
        }
        return if (app != null) openApp(app.packageName) else false
    }

    fun openSettings() {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openWifiSettings() {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    // ── System Info ───────────────────────────────────────────────────────────

    fun getDeviceInfo(): Map<String, String> {
        return mapOf(
            "model" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "android" to "Android ${Build.VERSION.RELEASE}",
            "sdk" to Build.VERSION.SDK_INT.toString()
        )
    }

    // ── Command Router ────────────────────────────────────────────────────────

    data class CommandResult(val executed: Boolean, val response: String)

    suspend fun handleCommand(command: String): CommandResult = withContext(Dispatchers.Main) {
        val lower = command.lowercase()
        when {
            lower.contains("battery") || lower.contains("charge") ->
                CommandResult(true, getBatteryStatusHindi())

            lower.contains("time") || lower.contains("baj") || lower.contains("samay") ->
                CommandResult(true, getCurrentTimeHindi())

            lower.contains("volume up") || lower.contains("volume badha") || lower.contains("awaaz badha") -> {
                increaseVolume()
                CommandResult(true, "Volume badha diya")
            }

            lower.contains("volume down") || lower.contains("volume kam") || lower.contains("awaaz kam") -> {
                decreaseVolume()
                CommandResult(true, "Volume kam kar diya")
            }

            lower.contains("mute") -> {
                muteVolume()
                CommandResult(true, "Mute kar diya")
            }

            lower.contains("torch on") || lower.contains("flashlight on") || lower.contains("torch chala") -> {
                setTorch(true)
                CommandResult(true, "Torch on kar diya")
            }

            lower.contains("torch off") || lower.contains("flashlight off") || lower.contains("torch band") -> {
                setTorch(false)
                CommandResult(true, "Torch off kar diya")
            }

            lower.contains("torch") || lower.contains("flashlight") -> {
                val state = toggleTorch()
                CommandResult(true, if (state) "Torch on kar diya" else "Torch off kar diya")
            }

            lower.contains("camera") || lower.contains("photo") -> {
                openCamera()
                CommandResult(true, "Camera khol raha hoon")
            }

            lower.contains("settings") -> {
                openSettings()
                CommandResult(true, "Settings khol raha hoon")
            }

            lower.contains("wifi") -> {
                openWifiSettings()
                CommandResult(true, "WiFi settings khol raha hoon")
            }

            lower.contains("bluetooth") -> {
                openBluetoothSettings()
                CommandResult(true, "Bluetooth settings khol raha hoon")
            }

            else -> CommandResult(false, "")
        }
    }
}
