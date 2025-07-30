package com.informatlux.test

import okhttp3.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType

class ChatService(
    private val apiKey: String,
    private val model: String = "google/gemma-7b", // HuggingFace model name
    private val apiUrl: String = "https://api-inference.huggingface.co/models/google/gemma-7b"
) {
    private val client = OkHttpClient()
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    data class ChatMessage(val role: String, val content: String)
    data class ChoiceDelta(val delta: Map<String, String>?, val finish_reason: String?)
    data class StreamChunk(val choices: List<ChoiceDelta>)

    /**
     * Streams a chat completion.  As partial chunks arrive,
     * onPartial(text) is invoked on the Main thread.
     * onComplete() is called when done, onError(e) on failure.
     */
    fun streamChat(
        history: List<ChatMessage>,
        onPartial: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        // Build the prompt from history
        val prompt = history.joinToString("\n") { m ->
            if (m.role == "user") "User: ${m.content}" else "AI: ${m.content}"
        } + "\nAI: "
        val bodyJson = mapOf("inputs" to prompt)
        val body = RequestBody.create(
            "application/json".toMediaType(),
            com.google.gson.Gson().toJson(bodyJson)
        )
        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                onError(e)
            }
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    onError(Exception("HTTP ${response.code} ${response.message}"))
                    return
                }
                val bodyStr = response.body?.string()
                try {
                    val jsonResp = com.google.gson.Gson().fromJson(bodyStr, List::class.java)
                    val result = (jsonResp.getOrNull(0) as? Map<*, *>)?.get("generated_text") as? String
                    if (!result.isNullOrBlank()) {
                        // Simulate streaming by sending the whole result as one chunk
                        kotlinx.coroutines.MainScope().launch {
                            onPartial(result.trim())
                            onComplete()
                        }
                    } else {
                        onError(Exception("No result from AI"))
                    }
                } catch (e: Exception) {
                    onError(e)
                } finally {
                    response.close()
                }
            }
        })
    }
}

