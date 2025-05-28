package com.mentra.telemetry.ml

import android.content.Context
import android.util.Log
import com.mentra.telemetry.WindowMetrics
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Handles TensorFlow Lite model operations for behavior prediction
 */
class BehaviorModel(context: Context) : Closeable {
    private val interpreter: Interpreter
    
    init {
        try {
            val modelFile = FileUtil.loadMappedFile(context, "model.tflite")
            interpreter = Interpreter(modelFile)
            logModelInfo()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading behavior model", e)
            throw e
        }
    }
    
    /**
     * Predicts behavior patterns from window metrics and triggers nudges if needed
     * @return Float array of predictions [isDistracted, isCompulsiveChecking, isHighTypingActivity]
     */
    fun predict(metrics: WindowMetrics, onDistracted: () -> Unit = {}): FloatArray {
        // Prepare input tensor
        val inputBuffer = ByteBuffer.allocateDirect(INPUT_TENSOR_SIZE).apply {
            order(ByteOrder.nativeOrder())
            
            // Normalize and add features
            putFloat(metrics.screenOnDuration.toFloat() / MAX_SCREEN_TIME)
            putFloat(metrics.unlockCount.toFloat() / MAX_UNLOCK_COUNT)
            putFloat(metrics.keysTyped.toFloat() / MAX_TYPING_COUNT)
            putFloat(metrics.activeApps.size.toFloat() / MAX_ACTIVE_APPS)
            rewind()
        }
        
        // Prepare output tensor
        val outputBuffer = Array(1) { FloatArray(NUM_OUTPUTS) }
        
        // Run inference
        interpreter.run(inputBuffer, outputBuffer)
        
        // Get predictions and check for distraction
        val predictions = outputBuffer[0]
        checkForDistraction(predictions, onDistracted)
        
        // Log predictions
        Log.d(TAG, """
            Model predictions:
            - Distracted: ${predictions[DISTRACTED_IDX]}
            - Compulsive checking: ${predictions[COMPULSIVE_IDX]}
            - High typing activity: ${predictions[TYPING_IDX]}
            
            Raw features:
            - Screen on duration: ${metrics.screenOnDuration}ms
            - Unlock count: ${metrics.unlockCount}
            - Keys typed: ${metrics.keysTyped}
            - Active apps: ${metrics.activeApps.size}
        """.trimIndent())
        
        return predictions
    }
    
    /**
     * Checks if the user is distracted based on model predictions and triggers a nudge if needed
     */
    private fun checkForDistraction(predictions: FloatArray, onDistracted: () -> Unit) {
        val predictedClass = predictions.indices.maxByOrNull { predictions[it] }
        if (predictedClass == DISTRACTED_IDX && predictions[DISTRACTED_IDX] >= DISTRACTION_THRESHOLD) {
            Log.i(TAG, "Distraction detected with confidence: ${predictions[DISTRACTED_IDX]}")
            onDistracted()
        }
    }
    
    private fun logModelInfo() {
        val inputTensor = interpreter.getInputTensor(0)
        val outputTensor = interpreter.getOutputTensor(0)
        
        Log.i(TAG, """
            Model loaded successfully:
            - Input shape: ${inputTensor.shape().contentToString()}
            - Input type: ${inputTensor.dataType()}
            - Output shape: ${outputTensor.shape().contentToString()}
            - Output type: ${outputTensor.dataType()}
        """.trimIndent())
    }
    
    override fun close() {
        interpreter.close()
    }
    
    companion object {
        private const val TAG = "BehaviorModel"
        
        // Input tensor configuration
        private const val INPUT_TENSOR_SIZE = 4 * 4 // 4 features * 4 bytes per float
        private const val NUM_OUTPUTS = 3 // isDistracted, isCompulsiveChecking, isHighTypingActivity
        
        // Feature normalization constants
        private const val MAX_SCREEN_TIME = 3600000f // 1 hour in ms
        private const val MAX_UNLOCK_COUNT = 20f
        private const val MAX_TYPING_COUNT = 1000f
        private const val MAX_ACTIVE_APPS = 20f
        
        // Behavior indices in model output
        const val DISTRACTED_IDX = 0
        const val COMPULSIVE_IDX = 1
        const val TYPING_IDX = 2
        
        // Threshold for triggering nudges
        private const val DISTRACTION_THRESHOLD = 0.7f // Confidence threshold for distraction
    }
} 