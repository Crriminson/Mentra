package com.mentra.telemetry

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.mentra.telemetry.analysis.BehaviorAnalyzer
import com.mentra.telemetry.model.AppUsageEvent
import com.mentra.telemetry.model.ScreenEvent
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Processes telemetry events in fixed time windows and computes aggregate metrics
 */
class WindowingProcessor(context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val windowDuration = 10_000L // 10 seconds in milliseconds
    private val behaviorAnalyzer = BehaviorAnalyzer(context)
    
    // In-memory event buffers
    private val screenEvents = mutableListOf<ScreenEvent>()
    private val appUsageEvents = mutableListOf<AppUsageEvent>()
    private val typingEvents = mutableListOf<TypingEvent>()
    
    private val windowingRunnable = object : Runnable {
        override fun run() {
            try {
                processCurrentWindow()
            } finally {
                // Schedule next window processing
                handler.postDelayed(this, windowDuration)
            }
        }
    }
    
    fun start() {
        handler.post(windowingRunnable)
        Log.i(TAG, "Started windowing processor")
    }
    
    fun stop() {
        handler.removeCallbacks(windowingRunnable)
        Log.i(TAG, "Stopped windowing processor")
    }
    
    fun addScreenEvent(event: ScreenEvent) {
        synchronized(screenEvents) {
            screenEvents.add(event)
        }
    }
    
    fun addAppUsageEvent(event: AppUsageEvent) {
        synchronized(appUsageEvents) {
            appUsageEvents.add(event)
        }
    }
    
    fun addTypingEvent(event: TypingEvent) {
        synchronized(typingEvents) {
            typingEvents.add(event)
        }
    }
    
    private fun processCurrentWindow() {
        val now = System.currentTimeMillis()
        val windowStart = now - windowDuration
        val windowStartInstant = Instant.ofEpochMilli(windowStart)
        val windowEndInstant = Instant.ofEpochMilli(now)
        
        // Get events in current window
        val windowScreenEvents = synchronized(screenEvents) {
            screenEvents.filter { it.timestamp.toEpochMilli() >= windowStart }
        }
        
        val windowAppUsageEvents = synchronized(appUsageEvents) {
            appUsageEvents.filter { it.timestamp.toEpochMilli() >= windowStart }
        }
        
        val windowTypingEvents = synchronized(typingEvents) {
            typingEvents.filter { it.timestamp.toEpochMilli() >= windowStart }
        }
        
        // Compute metrics
        val metrics = WindowMetrics(
            windowStartTime = windowStartInstant,
            windowEndTime = windowEndInstant,
            windowDurationSeconds = Duration.between(windowStartInstant, windowEndInstant).seconds,
            screenOnDuration = computeScreenOnDuration(windowScreenEvents),
            unlockCount = windowScreenEvents.count { it.type == ScreenEvent.Type.USER_PRESENT },
            keysTyped = windowTypingEvents.sumOf { it.textLength },
            activeApps = windowAppUsageEvents
                .filter { it.eventType == AppUsageEvent.Type.MOVE_TO_FOREGROUND }
                .map { it.packageName }
                .distinct()
        )
        
        // Analyze behavior patterns using ML model
        val patterns = behaviorAnalyzer.analyzeWindow(metrics)
        
        // Log metrics and patterns
        val timestamp = metrics.windowStartTime
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        
        Log.i(TAG, """
            [$timestamp] Window Metrics:
            - Screen on duration: ${metrics.screenOnDuration}ms
            - Unlock count: ${metrics.unlockCount}
            - Keys typed: ${metrics.keysTyped}
            - Active apps: ${metrics.activeApps.size}
            
            Behavior Analysis:
            - Distracted: ${patterns.isDistracted}
            - Compulsive checking: ${patterns.isCompulsiveChecking}
            - High typing activity: ${patterns.isHighTypingActivity}
            
            Reasons:
            ${patterns.reasons.joinToString("\n") { "- $it" }}
        """.trimIndent())
        
        // TODO: Store metrics and patterns in database
        
        // Clear old events
        pruneOldEvents(windowStart)
    }
    
    private fun computeScreenOnDuration(events: List<ScreenEvent>): Long {
        var duration = 0L
        var lastOnTime: Long? = null
        
        for (event in events.sortedBy { it.timestamp }) {
            when (event.type) {
                ScreenEvent.Type.SCREEN_ON -> {
                    lastOnTime = event.timestamp.toEpochMilli()
                }
                ScreenEvent.Type.SCREEN_OFF -> {
                    lastOnTime?.let { onTime ->
                        duration += event.timestamp.toEpochMilli() - onTime
                        lastOnTime = null
                    }
                }
                ScreenEvent.Type.USER_PRESENT -> { /* Ignore */ }
            }
        }
        
        // Add duration until now if screen is still on
        lastOnTime?.let { onTime ->
            duration += System.currentTimeMillis() - onTime
        }
        
        return duration
    }
    
    private fun pruneOldEvents(cutoffTime: Long) {
        synchronized(screenEvents) {
            screenEvents.removeAll { it.timestamp.toEpochMilli() < cutoffTime }
        }
        synchronized(appUsageEvents) {
            appUsageEvents.removeAll { it.timestamp.toEpochMilli() < cutoffTime }
        }
        synchronized(typingEvents) {
            typingEvents.removeAll { it.timestamp.toEpochMilli() < cutoffTime }
        }
    }
    
    companion object {
        private const val TAG = "WindowingProcessor"
    }
}

/**
 * Represents aggregated metrics for a time window
 */
data class WindowMetrics(
    val windowStartTime: Instant,
    val windowEndTime: Instant,
    val windowDurationSeconds: Long,
    val screenOnDuration: Long,
    val unlockCount: Int,
    val keysTyped: Int,
    val activeApps: List<String>
)

/**
 * Represents a typing event with metadata
 */
data class TypingEvent(
    val packageName: String,
    val textLength: Int,
    val timestamp: Instant
) 