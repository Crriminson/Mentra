package com.mentra.telemetry.nudge

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class NudgeOverlayService : Service() {
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra(EXTRA_MESSAGE)?.let { message ->
            showNudge(message)
        }
        return START_NOT_STICKY
    }
    
    private fun showNudge(message: String) {
        val nudgeIntent = Intent(this, NudgeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_MESSAGE, message)
        }
        startActivity(nudgeIntent)
        stopSelf()
    }
    
    companion object {
        private const val EXTRA_MESSAGE = "message"
        
        fun start(context: Context, message: String) {
            val intent = Intent(context, NudgeOverlayService::class.java).apply {
                putExtra(EXTRA_MESSAGE, message)
            }
            context.startService(intent)
        }
    }
} 