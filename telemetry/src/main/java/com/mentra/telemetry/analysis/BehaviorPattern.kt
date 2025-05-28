package com.mentra.telemetry.analysis

/**
 * Represents detected behavioral patterns in a time window
 */
data class BehaviorPattern(
    val isDistracted: Boolean,
    val isCompulsiveChecking: Boolean,
    val isHighTypingActivity: Boolean,
    val reasons: List<String>
) {
    companion object {
        // Threshold constants
        const val HIGH_UNLOCK_COUNT = 5
        const val HIGH_SCREEN_TIME_MS = 300_000L // 5 minutes
        const val HIGH_TYPING_COUNT = 500 // characters in 10 seconds
        const val RAPID_APP_SWITCHES = 8 // different apps in 10 seconds
    }
} 