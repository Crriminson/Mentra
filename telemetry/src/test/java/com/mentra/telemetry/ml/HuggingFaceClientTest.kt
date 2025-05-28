package com.mentra.telemetry.ml

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HuggingFaceClientTest {
    private lateinit var client: HuggingFaceClient
    
    @Before
    fun setup() {
        // Replace with your API token for testing
        client = HuggingFaceClient(
            apiToken = "YOUR_API_TOKEN_HERE",
            modelId = "distilbert-base-uncased"
        )
    }
    
    @Test
    fun testBehaviorAnalysis() = runBlocking {
        val input = """
            User behavior summary:
            - Unlocked phone 10 times in 5 minutes
            - Average session duration: 45 seconds
            - Opened social media apps 8 times
            - Total screen time: 15 minutes
        """.trimIndent()
        
        val insights = client.analyzeBehavior(input)
        
        // Verify we got valid insights
        assertNotNull(insights)
        assertNotNull(insights.message)
        assertFalse(insights.message.isEmpty())
        
        println("""
            Behavior Analysis Results:
            Message: ${insights.message}
            Suggests Break: ${insights.suggestsBreak}
            Indicates High Usage: ${insights.indicatesHighUsage}
        """.trimIndent())
    }
} 