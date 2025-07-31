package com.informatlux.test

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

object DeepSeekService {
    private const val TAG = "DeepSeekService"
    private const val API_URL = "https://openrouter.ai/api/v1/chat/completions"
    private const val API_KEY = "Bearer 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"
    private const val MAX_IMAGE_DIM = 256 // <= Resize images to a max of 256px (width or height)

    // Context and language hints
    private const val LANGUAGE_ENFORCEMENT = "\n\nIMPORTANT: Please respond ONLY in English language. Do not use Chinese, Japanese, Korean, or any other language. All responses must be in clear, proper English."
    private const val CONTEXT_APPEND = "\n\nContext: You are an AI assistant specialized in waste management, recycling, and environmental sustainability. Provide practical, actionable advice. Format your response with proper line breaks, bullet points, and clear structure."

    private val client = OkHttpClient()

    suspend fun askDeepSeek(
        prompt: String,
        imageBitmap: Bitmap? = null
    ): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting DeepSeek request for prompt: ${prompt.take(100)}...")

        val messages = JSONArray()

        // Build the actual prompt sent to API (with hidden appends)
        val enhancedPrompt = buildEnhancedPrompt(prompt, imageBitmap)

        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", enhancedPrompt)
        })

        val body = JSONObject().apply {
            put("model", "deepseek/deepseek-r1-0528:free")
            put("messages", messages)
            put("max_tokens", 2048)
            put("stream", false)
            put("temperature", 0.7)
        }

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", API_KEY)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), body.toString()))
            .build()

        try {
            Log.d(TAG, "Sending request to DeepSeek API...")
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext "No response"

            Log.d(TAG, "Received response from DeepSeek API")
            val formattedResponse = parseResponse(responseBody)
            Log.d(TAG, "Formatted response: ${formattedResponse.take(200)}...")

            return@withContext formattedResponse
        } catch (e: Exception) {
            Log.e(TAG, "Error in DeepSeek API call", e)
            return@withContext "Sorry, I encountered an error. Please try again."
        }
    }

    private fun buildEnhancedPrompt(userPrompt: String, imageBitmap: Bitmap?): String {
        val contentBuilder = StringBuilder()

        // Start with user's original prompt
        contentBuilder.append(userPrompt)

        // Add image data if present, resizing it first
        if (imageBitmap != null) {
            Log.d(TAG, "Resizing and adding image data to prompt")
            val resized = resizeBitmap(imageBitmap, MAX_IMAGE_DIM)
            val base64Image = bitmapToBase64(resized)
            contentBuilder.append("\n\nImage data: data:image/jpeg;base64,$base64Image")
        }

        // Add context and language guidance
        contentBuilder.append(CONTEXT_APPEND)
        contentBuilder.append(LANGUAGE_ENFORCEMENT)

        return contentBuilder.toString()
    }

    /** Resize a bitmap to fit within maxDimension (if needed). Keeps aspect ratio. */
    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = Math.min(
            maxDimension.toFloat() / width,
            maxDimension.toFloat() / height
        )
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        Log.d(TAG, "Resizing bitmap from ${width}x$height to ${newWidth}x${newHeight}")
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun parseResponse(responseBody: String): String {
        return try {
            val json = JSONObject(responseBody)
            val choices = json.optJSONArray("choices")

            if (choices != null && choices.length() > 0) {
                val rawContent = choices.getJSONObject(0)
                    .optJSONObject("message")
                    ?.optString("content") ?: "No answer"

                Log.d(TAG, "Raw AI response: ${rawContent.take(200)}...")

                // Clean up and format the response
                rawContent.stripMarkdown().cleanResponse().formatResponse()
            } else {
                // Handle error in response
                val error = json.optJSONObject("error")
                if (error != null) {
                    Log.e(TAG, "API Error: ${error.optString("message", "Unknown error")}")
                    "Sorry, I encountered an error: ${error.optString("message", "Unknown error")}"
                } else {
                    Log.e(TAG, "No choices in response")
                    "No answer received"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response", e)
            "Sorry, I couldn't process the response. Please try again."
        }
    }

    // Enhanced cleaning function
    private fun String.stripMarkdown(): String {
        return this
            .replace(Regex("[#*`_~]"), "") // Remove markdown formatting
            .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1") // Convert links to just text
            .replace(Regex("``````"), "") // Remove code blocks
            .replace(Regex("`([^`]+)`"), "$1") // Remove inline code formatting
            .trim()
    }

    // Additional response cleaning
    private fun String.cleanResponse(): String {
        return this
            .replace(Regex("^(Answer:|Response:|Reply:)\\s*", RegexOption.IGNORE_CASE), "") // Remove prefixes
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .replace(Regex("[\u4e00-\u9fff]+"), "") // Remove any Chinese characters that slip through
            .replace(Regex("[\\u3040-\\u309f\\u30a0-\\u30ff]+"), "") // Remove Japanese characters
            .replace(Regex("[\\uac00-\\ud7af]+"), "") // Remove Korean characters
            .trim()
            .takeIf { it.isNotBlank() } ?: "I apologize, but I couldn't generate a proper English response. Please try rephrasing your question."
    }

    // Response formatting function
    private fun String.formatResponse(): String {
        Log.d(TAG, "Formatting response: ${this.take(100)}...")

        return this
            .split("\n")
            .map { line ->
                line.trim().let { trimmedLine ->
                    when {
                        // Format bullet points
                        trimmedLine.matches(Regex("^[-•*]\\s*.*")) -> {
                            "• ${trimmedLine.replace(Regex("^[-•*]\\s*"), "")}"
                        }
                        // Format numbered lists
                        trimmedLine.matches(Regex("^\\d+\\.\\s*.*")) -> {
                            trimmedLine
                        }
                        // Format headers (lines that end with colon and are short)
                        trimmedLine.matches(Regex("^[A-Z][^:]*:$")) -> {
                            "\n${trimmedLine}\n"
                        }
                        // Format section breaks
                        trimmedLine.matches(Regex("^[A-Z\\s]+$")) && trimmedLine.length < 50 -> {
                            "\n${trimmedLine}\n"
                        }
                        // Add line breaks for long sentences
                        trimmedLine.length > 80 && !trimmedLine.contains(".") -> {
                            "$trimmedLine\n"
                        }
                        // Keep other lines as is
                        else -> trimmedLine
                    }
                }
            }
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n") // Remove excessive line breaks
            .trim()
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
