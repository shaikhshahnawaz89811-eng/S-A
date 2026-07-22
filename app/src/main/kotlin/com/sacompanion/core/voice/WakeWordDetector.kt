package com.sacompanion.core.voice

import java.util.Locale

class WakeWordDetector {

    private val wakeWords = listOf(
        "SA", "S.A.", "एसए", "Hey SA", "Ok SA", "Aye SA"
    )

    data class WakeWordMatch(
        val matched: Boolean,
        val command: String,
        val confidence: Float
    )

    fun detect(text: String): WakeWordMatch {
        val trimmed = text.trim()
        val upper = trimmed.uppercase(Locale.ROOT)

        for (wake in wakeWords) {
            val wakeUpper = wake.uppercase(Locale.ROOT)
            when {
                upper.startsWith(wakeUpper) -> {
                    val command = trimmed.substring(wake.length).trim()
                    return WakeWordMatch(true, command, 1.0f)
                }
                upper.contains(wakeUpper) -> {
                    val idx = upper.indexOf(wakeUpper)
                    val command = trimmed.substring(idx + wake.length).trim()
                    return WakeWordMatch(true, command, 0.8f)
                }
            }
        }
        return WakeWordMatch(false, trimmed, 0f)
    }

    fun extractCommand(text: String): String {
        val result = detect(text)
        return if (result.matched) result.command else text
    }
}
