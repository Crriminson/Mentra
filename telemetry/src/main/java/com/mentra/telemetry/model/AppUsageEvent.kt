package com.mentra.telemetry.model

import android.app.usage.UsageEvents
import java.time.Instant

/**
 * Represents an app usage event
 */
data class AppUsageEvent(
    val packageName: String,
    val eventType: Type,
    val timestamp: Instant
) {
    enum class Type {
        ACTIVITY_RESUMED,
        ACTIVITY_PAUSED,
        ACTIVITY_STOPPED,
        ACTIVITY_DESTROYED,
        MOVE_TO_FOREGROUND,
        MOVE_TO_BACKGROUND,
        OTHER;
        
        companion object {
            fun fromUsageEvent(event: UsageEvents.Event): Type = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> ACTIVITY_RESUMED
                UsageEvents.Event.ACTIVITY_PAUSED -> ACTIVITY_PAUSED
                UsageEvents.Event.ACTIVITY_STOPPED -> ACTIVITY_STOPPED
                UsageEvents.Event.ACTIVITY_DESTROYED -> ACTIVITY_DESTROYED
                UsageEvents.Event.MOVE_TO_FOREGROUND -> MOVE_TO_FOREGROUND
                UsageEvents.Event.MOVE_TO_BACKGROUND -> MOVE_TO_BACKGROUND
                else -> OTHER
            }
        }
    }
    
    companion object {
        fun fromUsageEvent(event: UsageEvents.Event): AppUsageEvent = AppUsageEvent(
            packageName = event.packageName,
            eventType = Type.fromUsageEvent(event),
            timestamp = Instant.ofEpochMilli(event.timeStamp)
        )
    }
} 