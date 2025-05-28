package com.mentra.telemetry.analysis

import android.content.Context
import android.util.Log
import com.mentra.telemetry.WindowMetrics
import com.mentra.telemetry.ml.BehaviorModel

/**
 * Analyzes window metrics to detect behavioral patterns
 */
class BehaviorAnalyzer(context: Context) {
    private val model = BehaviorModel(context)
    
    fun analyzeWindow(metrics: WindowMetrics): BehaviorPattern {
        val reasons = mutableListOf<String>()
        
        // Get model predictions
        val predictions = model.predict(metrics)
        val isDistracted = predictions[0] > PREDICTION_THRESHOLD
        val isCompulsiveChecking = predictions[1] > PREDICTION_THRESHOLD
        val isHighTypingActivity = predictions[2] > PREDICTION_THRESHOLD
        
        // Add reasons based on predictions and thresholds
        if (isDistracted) {
            reasons.add(buildDistractionReason(metrics))
        }
        if (isCompulsiveChecking) {
            reasons.add(buildCompulsiveCheckingReason(metrics))
        }
        if (isHighTypingActivity) {
            reasons.add(buildTypingActivityReason(metrics))
        }
        
        val pattern = BehaviorPattern(
            isDistracted = isDistracted,
            isCompulsiveChecking = isCompulsiveChecking,
            isHighTypingActivity = isHighTypingActivity,
            reasons = reasons
        )
        
        // Log detected patterns
        if (pattern.reasons.isNotEmpty()) {
            Log.i(TAG, """
                Detected patterns in window from ${metrics.windowStartTime}:
                ${pattern.reasons.joinToString("\n")}
                Model predictions: ${predictions.joinToString()}
            """.trimIndent())
        }
        
        return pattern
    }
    
    private fun buildDistractionReason(metrics: WindowMetrics): String {
        return "Distraction detected: " + when {
            metrics.unlockCount > BehaviorPattern.HIGH_UNLOCK_COUNT -> 
                "High unlock count (${metrics.unlockCount} times)"
            metrics.screenOnDuration > BehaviorPattern.HIGH_SCREEN_TIME_MS ->
                "Extended screen time (${metrics.screenOnDuration / 1000} seconds)"
            else -> "Multiple factors"
        }
    }
    
    private fun buildCompulsiveCheckingReason(metrics: WindowMetrics): String {
        return "Compulsive checking: " + when {
            metrics.unlockCount > BehaviorPattern.HIGH_UNLOCK_COUNT ->
                "Frequent unlocks (${metrics.unlockCount} times)"
            metrics.activeApps.size > BehaviorPattern.RAPID_APP_SWITCHES ->
                "Rapid app switching (${metrics.activeApps.size} apps)"
            else -> "Multiple factors"
        }
    }
    
    private fun buildTypingActivityReason(metrics: WindowMetrics): String {
        return "High typing activity: ${metrics.keysTyped} characters typed"
    }
    
    companion object {
        private const val TAG = "BehaviorAnalyzer"
        private const val PREDICTION_THRESHOLD = 0.7f
    }
} 