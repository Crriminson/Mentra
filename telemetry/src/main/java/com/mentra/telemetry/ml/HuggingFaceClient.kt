package com.mentra.telemetry.ml

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Client for making inference requests to Hugging Face API
 */
class HuggingFaceClient(
    private val apiToken: String,
    private val modelId: String = "distilbert-base-uncased"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://api-inference.huggingface.co/models"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Makes an inference request to Hugging Face API and returns behavioral insights
     * @param input The text input to analyze
     * @return Structured behavioral insights from the model's response
     * @throws IOException if the request fails
     */
    suspend fun analyzeBehavior(input: String): BehaviorInsights = withContext(Dispatchers.IO) {
        try {
            val jsonInput = JSONObject().put("inputs", input).toString()
            
            val request = Request.Builder()
                .url("$baseUrl/$modelId")
                .addHeader("Authorization", "Bearer $apiToken")
                .post(jsonInput.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Unexpected response ${response.code}")
                }
                
                val resultJson = response.body?.string() ?: throw IOException("Empty response body")
                Log.d(TAG, "Raw inference result: $resultJson")
                
                val parsedResponse = HuggingFaceResponse.parse(resultJson)
                val insights = parsedResponse.extractInsights()
                
                Log.i(TAG, """
                    Behavior analysis results:
                    - Message: ${insights.message}
                    - Suggests break: ${insights.suggestsBreak}
                    - Indicates high usage: ${insights.indicatesHighUsage}
                """.trimIndent())
                
                return@withContext insights
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing behavior", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "HuggingFaceClient"
    }
} 