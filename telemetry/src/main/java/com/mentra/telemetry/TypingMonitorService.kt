package com.mentra.telemetry

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TypingMonitorService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val text = event.text.joinToString()
            val timestamp = Instant.now()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            
            // Log the typing event with timestamp
            Log.d(TAG, "[$timestamp] Text changed in ${event.packageName}: length=${text.length}")
            
            // TODO: Store typing event in local database
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Typing monitor service connected")
    }

    companion object {
        private const val TAG = "TypingMonitor"
    }
} 