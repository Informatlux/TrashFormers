package com.informatlux.test

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.*

@Serializable
data class ChatSession(
    val id: String,
    val user_id: String,
    val title: String,
    val created_at: String? = null
)

@Serializable
data class ChatMessageData(
    val id: String? = null,
    val session_id: String,
    val message_type: String,
    val content: String? = null,
    val image_url: String? = null,
    val created_at: String? = null
)

class AIChatViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AIChatViewModel"

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _sessionTitle = MutableLiveData<String>("New Chat")
    val sessionTitle: LiveData<String> = _sessionTitle

    private val messageList = mutableListOf<ChatMessage>()
    private val prefs = application.getSharedPreferences("chat_history", Context.MODE_PRIVATE)
    private val userId = "user1" // Replace with actual user ID logic

    private lateinit var supabase: SupabaseClient
    private var currentSessionId: String? = null

    init {
        Log.d(TAG, "Initializing AIChatViewModel")
        initializeSupabase()
        loadHistory()
    }

    private fun initializeSupabase() {
        Log.d(TAG, "Initializing Supabase client")
        try {
            supabase = createSupabaseClient(
                supabaseUrl = "https://jedpwwxjrsejumyqyrgx.supabase.co",
                supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImplZHB3d3hqcnNlanVteXF5cmd4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTM4NzYzMzQsImV4cCI6MjA2OTQ1MjMzNH0.x9iFEmjd8ldd_llmc70ZfVqV3BBsUx1MSLnZbFCPlxI"
            ) {
                defaultSerializer = KotlinXSerializer(Json {
                    ignoreUnknownKeys = true
                })

                install(Auth) {
                    alwaysAutoRefresh = false
                    autoLoadFromStorage = false
                }

                install(Postgrest) {
                    defaultSchema = "public"
                }

                install(Storage)
            }
            Log.d(TAG, "Supabase client initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Supabase client", e)
        }
    }

    fun createNewSession() {
        Log.d(TAG, "Creating new chat session")
        viewModelScope.launch {
            try {
                val sessionData = ChatSession(
                    id = UUID.randomUUID().toString(),
                    user_id = userId,
                    title = "New Chat ${Date().time}",
                    created_at = null
                )

                Log.d(TAG, "Inserting session data: $sessionData")
                val response = supabase.postgrest["chat_sessions"]
                    .insert(sessionData) {
                        select()
                    }

                val sessions = response.decodeList<ChatSession>()
                currentSessionId = sessions.firstOrNull()?.id
                Log.d(TAG, "Created session with ID: $currentSessionId")

                // Clear current messages
                messageList.clear()
                _messages.value = emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create session in Supabase", e)
                // Handle error - create local session
                currentSessionId = UUID.randomUUID().toString()
                messageList.clear()
                _messages.value = emptyList()
            }
        }
    }

    fun loadSession(sessionId: String) {
        Log.d(TAG, "Loading session: $sessionId")
        currentSessionId = sessionId
        viewModelScope.launch {
            try {
                Log.d(TAG, "Fetching messages for session: $sessionId")
                val response = supabase.postgrest["chat_messages"]
                    .select(columns = Columns.list("*")) {
                        filter {
                            eq("session_id", sessionId)
                        }
                        order("created_at", order = Order.ASCENDING)
                    }

                val messages = response.decodeList<ChatMessageData>()
                Log.d(TAG, "Loaded ${messages.size} messages from Supabase")

                messageList.clear()

                for (msg in messages) {
                    Log.d(TAG, "Processing message: ${msg.message_type} - ${msg.content?.take(50)}")
                    when (msg.message_type) {
                        "user" -> {
                            msg.content?.let { content ->
                                messageList.add(ChatMessage.UserMessage(content))
                            }
                        }
                        "bot" -> {
                            msg.content?.let { content ->
                                messageList.add(ChatMessage.BotMessage(content, false))
                            }
                        }
                        "image" -> {
                            msg.image_url?.let { imageUrl ->
                                messageList.add(ChatMessage.ImagePreviewMessage(Uri.parse(imageUrl)))
                            }
                        }
                    }
                }
                _messages.value = messageList.toList()
                Log.d(TAG, "Session loaded successfully with ${messageList.size} messages")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load session from Supabase", e)
                // Handle error - load from local storage
                loadHistory()
            }
        }
    }

    fun sendMessage(userText: String) {
        Log.d(TAG, "Sending message: ${userText.take(50)}...")
        addMessage(ChatMessage.UserMessage(userText))
        addMessage(ChatMessage.BotMessage("", isLoading = true))

        viewModelScope.launch {
            try {
                // Award points for asking AI
                ScoreManager.addPoints(userId, ScoreManager.POINTS_AI_QUESTION)

                // Detect intent for scoring
                if (userText.contains("decompose", true) || userText.contains("decomposition", true)) {
                    ScoreManager.addPoints(userId, ScoreManager.POINTS_DECOMPOSITION_QUERY)
                }
                if (userText.contains("recycle", true) || userText.contains("recycling center", true)) {
                    ScoreManager.addPoints(userId, ScoreManager.POINTS_SEARCH_RECYCLING_CENTER)
                }
                if (userText.contains("DIY", true) || userText.contains("best out of waste", true)) {
                    ScoreManager.addPoints(userId, ScoreManager.POINTS_DIY_SUGGESTION)
                }
                if (userText.contains("classify", true) || userText.contains("waste type", true)) {
                    ScoreManager.addPoints(userId, ScoreManager.POINTS_WASTE_CLASSIFICATION)
                }

                Log.d(TAG, "Calling DeepSeek API...")
                val response = DeepSeekService.askDeepSeek(userText)
                Log.d(TAG, "Received response from DeepSeek: ${response.take(100)}...")

                updateLastBotMessage(response)
                saveToSupabase()
            } catch (e: Exception) {
                Log.e(TAG, "Error in sendMessage", e)
                updateLastBotMessage("Sorry, I encountered an error. Please try again.")
                saveHistory() // Fallback to local storage
            }
        }
    }

    fun analyzeImage(imageUri: Uri, context: Context) {
        Log.d(TAG, "Analyzing image: $imageUri")
        addMessage(ChatMessage.ImagePreviewMessage(imageUri))
        addMessage(ChatMessage.BotMessage("", isLoading = true))

        viewModelScope.launch {
            try {
                val bitmap = ImageUtils.loadBitmapFromUri(context, imageUri)
                if (bitmap != null) {
                    ScoreManager.addPoints(userId, ScoreManager.POINTS_WASTE_CLASSIFICATION)

                    // Upload image to Supabase Storage
                    Log.d(TAG, "Uploading image to Supabase Storage...")
                    val imageUrl = uploadImageToStorage(bitmap)
                    Log.d(TAG, "Image uploaded, URL: $imageUrl")

                    val prompt = "Identify the type of waste in this image, estimate decomposition time, suggest a DIY reuse, and provide a caption for proper disposal."
                    Log.d(TAG, "Calling DeepSeek API for image analysis...")
                    val response = DeepSeekService.askDeepSeek(prompt, bitmap)
                    Log.d(TAG, "Received image analysis response: ${response.take(100)}...")

                    updateLastBotMessage(response)

                    // Save image message to Supabase
                    saveImageMessageToSupabase(imageUrl)
                } else {
                    Log.e(TAG, "Failed to load bitmap from URI")
                    updateLastBotMessage("Could not load image.")
                }
                saveToSupabase()
            } catch (e: Exception) {
                Log.e(TAG, "Error in analyzeImage", e)
                updateLastBotMessage("Failed to analyze image. Please try again.")
                saveHistory() // Fallback to local storage
            }
        }
    }

    private suspend fun uploadImageToStorage(bitmap: Bitmap): String {
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            val imageBytes = outputStream.toByteArray()

            val fileName = "chat_images/${UUID.randomUUID()}.jpg"
            Log.d(TAG, "Uploading image with filename: $fileName")

            supabase.storage.from("chat-images").upload(
                path = fileName,
                data = imageBytes,
                upsert = false
            )

            val publicUrl = supabase.storage.from("chat-images").publicUrl(fileName)
            Log.d(TAG, "Image uploaded successfully, public URL: $publicUrl")
            publicUrl
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload image to storage", e)
            ""
        }
    }

    private suspend fun saveImageMessageToSupabase(imageUrl: String) {
        if (currentSessionId != null && imageUrl.isNotEmpty()) {
            try {
                val messageData = ChatMessageData(
                    session_id = currentSessionId!!,
                    message_type = "image",
                    image_url = imageUrl
                )
                Log.d(TAG, "Saving image message to Supabase: $messageData")
                supabase.postgrest["chat_messages"].insert(messageData)
                Log.d(TAG, "Image message saved successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save image message to Supabase", e)
            }
        }
    }

    private fun addMessage(msg: ChatMessage) {
        Log.d(TAG, "Adding message to list: ${msg.javaClass.simpleName}")
        messageList.add(msg)
        _messages.value = messageList.toList()
    }

    private fun updateLastBotMessage(text: String) {
        val lastIndex = messageList.indexOfLast { it is ChatMessage.BotMessage && it.isLoading }
        if (lastIndex != -1) {
            val oldMsg = messageList[lastIndex] as ChatMessage.BotMessage
            messageList[lastIndex] = oldMsg.copy(text = text, isLoading = false)
            _messages.value = messageList.toList()
            Log.d(TAG, "Updated bot message at index $lastIndex")
        }
    }

    private suspend fun saveToSupabase() {
        if (currentSessionId == null) {
            Log.w(TAG, "Cannot save to Supabase: no current session ID")
            return
        }

        try {
            // Save the last user and bot messages
            val lastUserMessage = messageList.findLast { it is ChatMessage.UserMessage }
            val lastBotMessage = messageList.findLast { it is ChatMessage.BotMessage && !it.isLoading }

            if (lastUserMessage != null) {
                val userMessageData = ChatMessageData(
                    session_id = currentSessionId!!,
                    message_type = "user",
                    content = (lastUserMessage as ChatMessage.UserMessage).text
                )
                Log.d(TAG, "Saving user message to Supabase: ${userMessageData.content?.take(50)}...")
                supabase.postgrest["chat_messages"].insert(userMessageData)
            }

            if (lastBotMessage != null) {
                val botMessageData = ChatMessageData(
                    session_id = currentSessionId!!,
                    message_type = "bot",
                    content = (lastBotMessage as ChatMessage.BotMessage).text
                )
                Log.d(TAG, "Saving bot message to Supabase: ${botMessageData.content?.take(50)}...")
                supabase.postgrest["chat_messages"].insert(botMessageData)
            }
            Log.d(TAG, "Messages saved to Supabase successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save messages to Supabase", e)
            // Fallback to local storage
            saveHistory()
        }
    }

    fun generateSessionTitle(firstMessage: String) {
        Log.d(TAG, "Generating session title for: ${firstMessage.take(50)}...")
        viewModelScope.launch {
            try {
                val titlePrompt = "Generate a short, descriptive title (3-5 words max) for a chat conversation that starts with: \"$firstMessage\". Only respond with the title, nothing else."
                val generatedTitle = DeepSeekService.askDeepSeek(titlePrompt)
                val cleanTitle = generatedTitle.take(50).trim() // Limit length

                Log.d(TAG, "Generated title: $cleanTitle")
                _sessionTitle.value = cleanTitle

                // Update session title in database
                updateSessionTitle(cleanTitle)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate session title", e)
                // Fallback to a simple title
                val fallbackTitle = firstMessage.take(30).trim() + if (firstMessage.length > 30) "..." else ""
                _sessionTitle.value = fallbackTitle
                updateSessionTitle(fallbackTitle)
            }
        }
    }

    private suspend fun updateSessionTitle(title: String) {
        if (currentSessionId != null) {
            try {
                Log.d(TAG, "Updating session title to: $title")
                supabase.postgrest["chat_sessions"]
                    .update({
                        set("title", title)
                    }) {
                        filter {
                            eq("id", currentSessionId!!)
                        }
                    }
                Log.d(TAG, "Session title updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update session title", e)
            }
        }
    }

    private fun saveHistory() {
        Log.d(TAG, "Saving chat history to local storage")
        val arr = JSONArray()
        for (msg in messageList) {
            val obj = JSONObject()
            when (msg) {
                is ChatMessage.UserMessage -> {
                    obj.put("type", "user")
                    obj.put("text", msg.text)
                }
                is ChatMessage.BotMessage -> {
                    obj.put("type", "bot")
                    obj.put("text", msg.text)
                    obj.put("isLoading", msg.isLoading)
                }
                is ChatMessage.ImagePreviewMessage -> {
                    obj.put("type", "image")
                    obj.put("uri", msg.uri.toString())
                }
            }
            arr.put(obj)
        }
        prefs.edit().putString("history", arr.toString()).apply()
        Log.d(TAG, "Chat history saved to local storage")
    }

    private fun loadHistory() {
        Log.d(TAG, "Loading chat history from local storage")
        val arrStr = prefs.getString("history", null) ?: return
        try {
            val arr = JSONArray(arrStr)
            messageList.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                when (obj.getString("type")) {
                    "user" -> messageList.add(ChatMessage.UserMessage(obj.getString("text")))
                    "bot" -> messageList.add(ChatMessage.BotMessage(obj.getString("text"), obj.optBoolean("isLoading", false)))
                    "image" -> messageList.add(ChatMessage.ImagePreviewMessage(Uri.parse(obj.getString("uri"))))
                }
            }
            _messages.value = messageList.toList()
            Log.d(TAG, "Loaded ${messageList.size} messages from local storage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load chat history from local storage", e)
            // Handle JSON parsing error
            messageList.clear()
            _messages.value = emptyList()
        }
    }
}