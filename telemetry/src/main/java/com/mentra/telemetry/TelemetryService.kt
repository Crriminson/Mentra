package com.mentra.telemetry

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.mentra.telemetry.model.AppUsageEvent
import com.mentra.telemetry.model.ScreenEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TelemetryService : Service() {
    
    private val handler = Handler(Looper.getMainLooper())
    private val usageStatsQueryInterval = 5 * 60 * 1000L // 5 minutes
    
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.let { action ->
                try {
                    val event = ScreenEvent(ScreenEvent.fromIntent(action))
                    val timestamp = event.timestamp
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    
                    logScreenEvent(event, timestamp)
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Unknown action received: $action", e)
                }
            }
        }
    }
    
    private val usageStatsRunnable = object : Runnable {
        override fun run() {
            queryUsageStats()
            // Schedule next run
            handler.postDelayed(this, usageStatsQueryInterval)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        registerScreenStateReceiver()
        // Start periodic usage stats collection
        handler.post(usageStatsRunnable)
        Log.i(TAG, "TelemetryService started")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenStateReceiver)
        handler.removeCallbacks(usageStatsRunnable)
        Log.i(TAG, "TelemetryService stopped")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Return sticky to ensure service restarts if killed
        return START_STICKY
    }
    
    private fun registerScreenStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenStateReceiver, filter)
    }
    
    private fun queryUsageStats() {
        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val hourAgo = now - 3600000 // 1 hour ago
            
            val events = usageStatsManager.queryEvents(hourAgo, now)
            val event = UsageEvents.Event()
            
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val appEvent = AppUsageEvent.fromUsageEvent(event)
                logUsageEvent(appEvent)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Usage stats permission not granted", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying usage stats", e)
        }
    }
    
    private fun logUsageEvent(event: AppUsageEvent) {
        val timestamp = event.timestamp
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        
        Log.i(TAG, "[$timestamp] App Event: ${event.packageName} - ${event.eventType}")
        // TODO: Store event in local database
    }
    
    private fun logScreenEvent(event: ScreenEvent, timestamp: String) {
        val message = when (event.type) {
            ScreenEvent.Type.SCREEN_ON -> "Screen turned ON"
            ScreenEvent.Type.SCREEN_OFF -> "Screen turned OFF"
            ScreenEvent.Type.USER_PRESENT -> "User unlocked device"
        }
        Log.i(TAG, "[$timestamp] $message")
        // TODO: Store event in local database
    }
    
    companion object {
        private const val TAG = "TelemetryService"
        
        fun createIntent(context: Context): Intent {
            return Intent(context, TelemetryService::class.java)
        }
    }
} 