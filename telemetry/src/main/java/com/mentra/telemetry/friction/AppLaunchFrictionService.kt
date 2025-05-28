package com.mentra.telemetry.friction

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Service that introduces friction by delaying launches of target apps
 */
class AppLaunchFrictionService : Service() {
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var packageManager: PackageManager
    private val handler = Handler(Looper.getMainLooper())
    
    // Track last launch times to prevent duplicate delays
    private val lastLaunchTimes = ConcurrentHashMap<String, Long>()
    private val LAUNCH_COOLDOWN = 500L // Minimum time between launches
    
    // List of apps to apply friction to
    private val targetApps = setOf(
        "com.instagram.android",
        "com.facebook.katana",
        "com.twitter.android",
        "com.snapchat.android",
        "com.tiktok.android"
    )
    
    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        packageManager = packageManager
        startMonitoring()
        Log.i(TAG, "App launch friction service started")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    private fun startMonitoring() {
        // Check for app launches every 100ms
        handler.postDelayed(object : Runnable {
            override fun run() {
                checkRecentLaunches()
                handler.postDelayed(this, CHECK_INTERVAL)
            }
        }, CHECK_INTERVAL)
    }
    
    private fun checkRecentLaunches() {
        try {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 1000 // Look back 1 second
            
            val events = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED && 
                    targetApps.contains(event.packageName)) {
                    handleAppLaunch(event.packageName)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking app launches", e)
        }
    }
    
    private fun handleAppLaunch(packageName: String) {
        val now = System.currentTimeMillis()
        val lastLaunch = lastLaunchTimes[packageName] ?: 0L
        
        // Check if enough time has passed since last launch
        if (now - lastLaunch < LAUNCH_COOLDOWN) {
            Log.d(TAG, "Ignoring duplicate launch of $packageName")
            return
        }
        
        lastLaunchTimes[packageName] = now
        
        try {
            // Get the launch intent for the app
            packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                
                // Kill the current app instance
                packageManager.clearPackagePreferredActivities(packageName)
                @Suppress("DEPRECATION")
                stopApp(packageName)
                
                // Delay the relaunch
                handler.postDelayed({
                    try {
                        Log.i(TAG, "Relaunching $packageName after delay")
                        startActivity(launchIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error relaunching $packageName", e)
                    }
                }, LAUNCH_DELAY)
                
                Log.i(TAG, "Added ${LAUNCH_DELAY}ms friction delay to $packageName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling launch of $packageName", e)
        }
    }
    
    @Suppress("DEPRECATION")
    private fun stopApp(packageName: String) {
        try {
            // For API 28 and below
            activityManager?.killBackgroundProcesses(packageName)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping $packageName", e)
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    companion object {
        private const val TAG = "LaunchFriction"
        private const val CHECK_INTERVAL = 100L // How often to check for launches
        private const val LAUNCH_DELAY = 1000L // 1 second delay
        
        fun start(context: Context) {
            val intent = Intent(context, AppLaunchFrictionService::class.java)
            context.startService(intent)
        }
    }
} 