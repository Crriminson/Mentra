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
import com.mentra.telemetry.ui.UsageEvent
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.mentra.telemetry.nudge.NudgeOverlayService
import android.provider.Settings
import android.net.Uri
import com.mentra.telemetry.friction.AppLaunchFrictionService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class TelemetryService : Service() {
    
    private val handler = Handler(Looper.getMainLooper())
    private val usageStatsQueryInterval = 5 * 60 * 1000L // 5 minutes
    private lateinit var windowingProcessor: WindowingProcessor
    private var lastNudgeTime = 0L
    private val MIN_NUDGE_INTERVAL = 15 * 60 * 1000L // 15 minutes
    
    // Track app usage durations
    private val appUsageDurations = mutableMapOf<String, Long>()
    private var lastUsageCheck = System.currentTimeMillis()
    
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.let { action ->
                try {
                    val event = ScreenEvent(ScreenEvent.fromIntent(action))
                    windowingProcessor.addScreenEvent(event)
                    
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
        windowingProcessor = WindowingProcessor()
        windowingProcessor.start()
        registerScreenStateReceiver()
        // Start periodic usage stats collection
        handler.post(usageStatsRunnable)
        // Start friction service
        AppLaunchFrictionService.start(this)
        Log.i(TAG, "TelemetryService started")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenStateReceiver)
        handler.removeCallbacks(usageStatsRunnable)
        windowingProcessor.stop()
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
                windowingProcessor.addAppUsageEvent(appEvent)
                updateAppUsage(event)
                logUsageEvent(appEvent)
            }
            
            // Emit current usage stats
            emitUsageEvents()
            lastUsageCheck = now
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Usage stats permission not granted", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying usage stats", e)
        }
    }
    
    private fun updateAppUsage(event: UsageEvents.Event) {
        when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> {
                appUsageDurations[event.packageName] = (appUsageDurations[event.packageName] ?: 0L) +
                    (event.timeStamp - lastUsageCheck)
            }
            UsageEvents.Event.ACTIVITY_PAUSED -> {
                // Reset duration when app is paused
                appUsageDurations[event.packageName]?.let { duration ->
                    val appName = try {
                        packageManager.getApplicationInfo(event.packageName, 0)
                            .loadLabel(packageManager).toString()
                    } catch (e: Exception) {
                        event.packageName
                    }
                    
                    val usageEvent = UsageEvent(
                        appName = appName,
                        packageName = event.packageName,
                        duration = Duration.ofMillis(duration),
                        timestamp = Instant.ofEpochMilli(event.timeStamp)
                    )
                    
                    _usageEvents.tryEmit(usageEvent)
                    appUsageDurations.remove(event.packageName)
                }
            }
        }
    }
    
    private fun emitUsageEvents() {
        val now = System.currentTimeMillis()
        appUsageDurations.forEach { (packageName, duration) ->
            val appName = try {
                packageManager.getApplicationInfo(packageName, 0)
                    .loadLabel(packageManager).toString()
            } catch (e: Exception) {
                packageName
            }
            
            val usageEvent = UsageEvent(
                appName = appName,
                packageName = packageName,
                duration = Duration.ofMillis(duration),
                timestamp = Instant.now()
            )
            
            _usageEvents.tryEmit(usageEvent)
        }
    }
    
    private fun logUsageEvent(event: AppUsageEvent) {
        val timestamp = event.timestamp
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        
        Log.i(TAG, "[$timestamp] App Event: ${event.packageName} - ${event.eventType}")
    }
    
    private fun logScreenEvent(event: ScreenEvent, timestamp: String) {
        val message = when (event.type) {
            ScreenEvent.Type.SCREEN_ON -> "Screen turned ON"
            ScreenEvent.Type.SCREEN_OFF -> "Screen turned OFF"
            ScreenEvent.Type.USER_PRESENT -> "User unlocked device"
        }
        Log.i(TAG, "[$timestamp] $message")
    }
    
    private fun handleBehaviorInsights(insights: BehaviorInsights) {
        val now = System.currentTimeMillis()
        if (insights.indicatesHighUsage && insights.suggestsBreak &&
            now - lastNudgeTime >= MIN_NUDGE_INTERVAL) {
            showNudgeIfPermitted(insights.message)
            lastNudgeTime = now
        }
    }
    
    private fun showNudgeIfPermitted(message: String) {
        if (Settings.canDrawOverlays(this)) {
            NudgeOverlayService.start(this, message)
        } else {
            // Request permission through notification
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            // TODO: Show notification to request permission
        }
    }
    
    companion object {
        private const val TAG = "TelemetryService"
        
        private val _usageEvents = MutableSharedFlow<UsageEvent>()
        val usageEvents: SharedFlow<UsageEvent> = _usageEvents.asSharedFlow()
        
        fun createIntent(context: Context): Intent {
            return Intent(context, TelemetryService::class.java)
        }
        
        fun start(context: Context) {
            val intent = Intent(context, TelemetryService::class.java)
            context.startService(intent)
        }
    }
} 