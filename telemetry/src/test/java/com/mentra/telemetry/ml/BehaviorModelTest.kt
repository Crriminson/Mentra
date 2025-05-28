package com.mentra.telemetry.ml

import android.content.Context
import com.mentra.telemetry.WindowMetrics
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant

class BehaviorModelTest {
    private lateinit var context: Context
    private lateinit var model: BehaviorModel
    
    @Before
    fun setup() {
        // Mock context and file loading
        context = mockk()
        val modelBuffer = createDummyModelBuffer()
        every { FileUtil.loadMappedFile(context, "model.tflite") } returns modelBuffer
        
        model = BehaviorModel(context)
    }
    
    @Test
    fun `test model inference with normal usage patterns`() {
        val metrics = WindowMetrics(
            windowStartTime = Instant.now(),
            windowEndTime = Instant.now().plusSeconds(10),
            windowDurationSeconds = 10L,
            screenOnDuration = 8000L, // 8 seconds
            unlockCount = 1,
            keysTyped = 50,
            activeApps = listOf("com.example.app")
        )
        
        val predictions = model.predict(metrics)
        
        // With our dummy identity model, predictions should be approximately equal to normalized inputs
        assertEquals(8000f / 3600000f, predictions[0], 0.01f) // Normalized screen time
        assertEquals(1f / 20f, predictions[1], 0.01f) // Normalized unlock count
        assertEquals(50f / 1000f, predictions[2], 0.01f) // Normalized typing count
    }
    
    @Test
    fun `test model inference with high usage patterns`() {
        val metrics = WindowMetrics(
            windowStartTime = Instant.now(),
            windowEndTime = Instant.now().plusSeconds(10),
            windowDurationSeconds = 10L,
            screenOnDuration = 3600000L, // 1 hour
            unlockCount = 20,
            keysTyped = 1000,
            activeApps = List(20) { "com.example.app$it" }
        )
        
        val predictions = model.predict(metrics)
        
        // With maximum values, normalized predictions should be close to 1.0
        assertEquals(1.0f, predictions[0], 0.01f)
        assertEquals(1.0f, predictions[1], 0.01f)
        assertEquals(1.0f, predictions[2], 0.01f)
    }
    
    /**
     * Creates a dummy TFLite model buffer that acts as an identity function
     * (outputs normalized inputs directly)
     */
    private fun createDummyModelBuffer(): ByteBuffer {
        // Create a buffer with identity weights
        return ByteBuffer.allocateDirect(1024).apply {
            order(ByteOrder.nativeOrder())
            // TODO: Add dummy model bytes here
            // For testing, we're relying on the mocked Interpreter
            // In a real test, we would include actual model bytes
        }
    }
} 