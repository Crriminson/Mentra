package com.mentra.telemetry.model

import java.time.Instant

/**
 * Represents a screen state change event
 */
data class ScreenEvent(
    val type: Type,
    val timestamp: Instant = Instant.now()
) {
    enum class Type {
        SCREEN_ON,
        SCREEN_OFF,
        USER_PRESENT
    }
    
    companion object {
        fun fromIntent(action: String): Type = when (action) {
            android.content.Intent.ACTION_SCREEN_ON -> Type.SCREEN_ON
            android.content.Intent.ACTION_SCREEN_OFF -> Type.SCREEN_OFF
            android.content.Intent.ACTION_USER_PRESENT -> Type.USER_PRESENT
            else -> throw IllegalArgumentException("Unknown action: $action")
        }
    }
} 