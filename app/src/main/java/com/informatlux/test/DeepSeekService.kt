package com.informatlux.test

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

object DeepSeekService {
    private const val API_URL = "https://openrouter.ai/api/v1/chat/completions"
    private const val API_KEY = "Bearer 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"

    private val client = OkHttpClient()

    suspend fun askDeepSeek(
        prompt: String,
        imageBitmap: Bitmap? = null
    ): String = withContext(Dispatchers.IO) {
        val messages = JSONArray()
        val userMsg = JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        }
        messages.put(userMsg)

        if (imageBitmap != null) {
            val base64Image = bitmapToBase64(imageBitmap)
            val imageMsg = JSONObject().apply {
                put("role", "system")
                put("content", "data:image/jpeg;base64,$base64Image")
            }
            messages.put(imageMsg)
        }

        val body = JSONObject().apply {
            put("model", "deepseek/deepseek-chat-v3-0324:free")
            put("messages", messages)
            put("max_tokens", 512)
        }

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", API_KEY)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), body.toString()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return@withContext "No response"
        val json = JSONObject(responseBody)
        val choices = json.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: "No answer"
        } else {
            "No answer"
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}