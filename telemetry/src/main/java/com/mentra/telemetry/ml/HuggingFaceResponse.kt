package com.mentra.telemetry.ml

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a parsed response from the Hugging Face API
 */
data class HuggingFaceResponse(
    val generatedText: String,
    val rawJson: JSONObject
) {
    companion object {
        private const val TAG = "HuggingFaceResponse"
        
        /**
         * Parses the raw JSON response from Hugging Face API
         * Handles different response formats:
         * - Single object with "generated_text"
         * - Array of objects with "generated_text"
         * - Array of strings
         */
        fun parse(jsonString: String): HuggingFaceResponse {
            try {
                val json = JSONObject(jsonString)
                
                // Try direct "generated_text" field
                if (json.has("generated_text")) {
                    return HuggingFaceResponse(
                        generatedText = json.getString("generated_text"),
                        rawJson = json
                    )
                }
                
                // Try array response
                val jsonArray = when {
                    json.has("generated_texts") -> json.getJSONArray("generated_texts")
                    jsonString.startsWith("[") -> JSONArray(jsonString)
                    else -> throw IllegalArgumentException("Unexpected response format")
                }
                
                // Get first result from array
                val firstResult = jsonArray.get(0)
                val text = when (firstResult) {
                    is JSONObject -> firstResult.getString("generated_text")
                    is String -> firstResult
                    else -> throw IllegalArgumentException("Unexpected array element type")
                }
                
                return HuggingFaceResponse(
                    generatedText = text,
                    rawJson = JSONObject().put("generated_texts", jsonArray)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse Hugging Face response", e)
                throw e
            }
        }
    }
    
    /**
     * Extracts behavioral insights from the generated text
     */
    fun extractInsights(): BehaviorInsights {
        // TODO: Add more sophisticated parsing based on your model's output format
        return BehaviorInsights(
            message = generatedText,
            suggestsBreak = generatedText.contains("break", ignoreCase = true),
            indicatesHighUsage = generatedText.contains("high usage", ignoreCase = true) ||
                               generatedText.contains("excessive", ignoreCase = true)
        )
    }
}

/**
 * Structured insights extracted from the model's response
 */
data class BehaviorInsights(
    val message: String,
    val suggestsBreak: Boolean,
    val indicatesHighUsage: Boolean
) 