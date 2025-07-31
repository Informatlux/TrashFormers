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
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.*

// Data classes for Chat Sessions and Messages
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

    // LiveData for messages and UI state
    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _sessionTitle = MutableLiveData<String?>("New Chat")
    val sessionTitle: LiveData<String> = _sessionTitle as LiveData<String>

    private val _isOfflineMode = MutableLiveData(false)
    val isOfflineMode: LiveData<Boolean> = _isOfflineMode

    private val messageList = mutableListOf<ChatMessage>()

    private val prefs = application.getSharedPreferences("chat_history", Context.MODE_PRIVATE)

    private val _userId = MutableLiveData<String>()
    val userId: LiveData<String> = _userId

    private lateinit var supabase: SupabaseClient
    private var currentSessionId: String? = null
    private var supabaseAvailable = false

    init {
        Log.d(TAG, "Initializing AIChatViewModel")
        UserManager.initialize(application)
        viewModelScope.launch {
            val id = UserManager.getCurrentUserId()
            _userId.value = id
            initializeSupabase()
            loadChatSessions()
        }
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
                    // Use your actual schema below; commonly "public"
                    defaultSchema = "api"
                }

                install(Storage)
            }
            supabaseAvailable = true
            _isOfflineMode.postValue(false)
            Log.d(TAG, "Supabase client initialized successfully with user ID: ${_userId.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Supabase client", e)
            supabaseAvailable = false
            _isOfflineMode.postValue(true)
        }
    }

    public fun saveCurrentSession() {
        currentSessionId?.let { id ->
            prefs.edit().putString("current_session", id).putString("title_$id", _sessionTitle.value).apply()
            Log.d(TAG, "Saved session $id")
        }
    }

    // 2️⃣: Load session list (we just load last session here)
    private fun loadChatSessions() {
        val saved = prefs.getString("current_session", null)
        if (saved != null) {
            currentSessionId = saved
            _sessionTitle.value = prefs.getString("title_$saved", "Chat")
            loadHistory()
        } else {
            createNewSession()
        }
    }

    // 3️⃣: Load in-memory history for currentSessionId
    private fun loadHistory() {
        val id = currentSessionId ?: return createNewSession()
        val key = "history_$id"
        prefs.getString(key, null)?.let { json ->
            val arr = JSONArray(json)
            messageList.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                when (obj.getString("type")) {
                    "user" -> messageList.add(ChatMessage.UserMessage(obj.getString("text")))
                    "bot"  -> messageList.add(ChatMessage.BotMessage(obj.getString("text"), obj.optBoolean("isLoading", false)))
                    "image"-> messageList.add(ChatMessage.ImagePreviewMessage(Uri.parse(obj.getString("uri"))))
                }
            }
            _messages.value = messageList.toList()
            Log.d(TAG, "Loaded ${messageList.size} local messages for session $id")
        }
    }


    fun createNewSession() {
        Log.d(TAG, "Creating new chat session for user: ${_userId.value}")
        currentSessionId = UUID.randomUUID().toString()
        messageList.clear()
        _messages.value = emptyList()
        _sessionTitle.value = "New Chat"

        // Save new session immediately locally
        saveCurrentSession()

        if (supabaseAvailable) {
            viewModelScope.launch {
                try {
                    val sessionData = ChatSession(
                        id = currentSessionId!!,
                        user_id = _userId.value ?: "",
                        title = "New Chat",
                        created_at = null
                    )

                    Log.d(TAG, "Inserting session data: $sessionData")
                    supabase.postgrest["chat_sessions"]
                        .insert(sessionData)

                    _isOfflineMode.postValue(false)
                    Log.d(TAG, "Created session with ID: $currentSessionId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create session in Supabase", e)
                    _isOfflineMode.postValue(true)
                    handleSupabaseError(e)
                }
            }
        } else {
            _isOfflineMode.value = true
            Log.d(TAG, "Offline mode: session created locally")
        }

        // Clear current messages regardless of connectivity
        messageList.clear()
        _messages.value = emptyList()
        _sessionTitle.value = "New Chat"
    }

    fun loadSession(sessionId: String) {
        Log.d(TAG, "Loading session: $sessionId")
        currentSessionId = sessionId

        if (supabaseAvailable) {
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

                    val messagesData = response.decodeList<ChatMessageData>()
                    Log.d(TAG, "Loaded ${messagesData.size} messages from Supabase")

                    loadMessagesFromData(messagesData)
                    loadSessionTitle(sessionId)
                    _isOfflineMode.postValue(false)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load session from Supabase", e)
                    _isOfflineMode.postValue(true)
                    handleSupabaseError(e)
                    loadHistory() // fallback to local storage
                }
            }
        } else {
            _isOfflineMode.postValue(true)
            loadHistory()
        }
    }

    private fun loadMessagesFromData(messages: List<ChatMessageData>) {
        messageList.clear()
        for (msg in messages) {
            when (msg.message_type) {
                "user" -> msg.content?.let { messageList.add(ChatMessage.UserMessage(it)) }
                "bot" -> msg.content?.let { messageList.add(ChatMessage.BotMessage(it, false)) }
                "image" -> msg.image_url?.let { messageList.add(ChatMessage.ImagePreviewMessage(Uri.parse(it))) }
            }
        }
        _messages.value = messageList.toList()
    }

    private suspend fun loadSessionTitle(sessionId: String) {
        try {
            val response = supabase.postgrest["chat_sessions"]
                .select(columns = Columns.list("title")) {
                    filter {
                        eq("id", sessionId)
                    }
                }
            val sessions = response.decodeList<ChatSession>()
            sessions.firstOrNull()?.let {
                _sessionTitle.postValue(it.title)
                Log.d(TAG, "Loaded session title: ${it.title}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load session title", e)
        }
    }

    fun sendMessage(userText: String) {
        Log.d(TAG, "Sending message: ${userText.take(50)}...")

        if (currentSessionId == null) {
            createNewSession()
        }

        addMessage(ChatMessage.UserMessage(userText))
        addMessage(ChatMessage.BotMessage("", isLoading = true))

        // Generate short session title for first message pair
        if (messageList.size == 2) {
            generateSessionTitle(userText)
        }

        viewModelScope.launch {
            try {
                ScoreManager.addPoints(_userId.value ?: "", ScoreManager.POINTS_AI_QUESTION)
                detectAndScoreIntent(userText)

                Log.d(TAG, "Calling DeepSeek ..")
                val response = DeepSeekService.askDeepSeek(userText)
                Log.d(TAG, "Received response from DeepSeek: ${response.take(100)}...")

                updateLastBotMessage(response)

                if (supabaseAvailable) {
                    saveToSupabase()
                } else {
                    saveHistory()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in sendMessage", e)
                val errorMessage = when {
                    e.message?.contains("auth", true) == true ->
                        "Authentication error. Please check your API credentials."
                    e.message?.contains("network", true) == true ->
                        "Network error. Please check your internet connection."
                    else -> "Sorry, I encountered an error. Please try again."
                }
                updateLastBotMessage(errorMessage)
                saveHistory()
            }
        }
    }

    private fun detectAndScoreIntent(userText: String) {
        val lowerText = userText.lowercase(Locale.getDefault())

        when {
            lowerText.contains("decompose") || lowerText.contains("decomposition") -> {
                ScoreManager.addPoints(_userId.value ?: "", ScoreManager.POINTS_DECOMPOSITION_QUERY)
            }
            lowerText.contains("recycle") || lowerText.contains("recycling center") -> {
                ScoreManager.addPoints(_userId.value ?: "", ScoreManager.POINTS_SEARCH_RECYCLING_CENTER)
            }
            lowerText.contains("diy") || lowerText.contains("best out of waste") -> {
                ScoreManager.addPoints(_userId.value ?: "", ScoreManager.POINTS_DIY_SUGGESTION)
            }
            lowerText.contains("classify") || lowerText.contains("waste type") -> {
                ScoreManager.addPoints(_userId.value ?: "", ScoreManager.POINTS_WASTE_CLASSIFICATION)
            }
        }
    }

    private fun handleSupabaseError(e: Exception) {
        when {
            e.message?.contains("permission denied", true) == true -> {
                Log.w(TAG, "Database permissions not set up properly. Working in offline mode.")
                addMessage(ChatMessage.BotMessage("📱 Working in offline mode. Your chats are saved locally.", false))
            }
            e.message?.contains("network", true) == true -> {
                Log.w(TAG, "Network error. Working in offline mode.")
                addMessage(ChatMessage.BotMessage("🌐 No internet connection. Working in offline mode.", false))
            }
            else -> {
                Log.w(TAG, "Supabase error: ${e.message}. Working in offline mode.")
                addMessage(ChatMessage.BotMessage("💾 Database unavailable. Your chats are saved locally.", false))
            }
        }
    }

    fun analyzeImage(imageUri: Uri, context: Context) {
        Log.d(TAG, "Analyzing image: $imageUri")

        if (currentSessionId == null) {
            createNewSession()
        }

        addMessage(ChatMessage.ImagePreviewMessage(imageUri))
        addMessage(ChatMessage.BotMessage("", isLoading = true))

        viewModelScope.launch {
            try {
                val bitmap = ImageUtils.loadBitmapFromUri(context, imageUri)
                if (bitmap != null) {
                    ScoreManager.addPoints(_userId.value ?: "", ScoreManager.POINTS_WASTE_CLASSIFICATION)

                    var imageUrl = ""
                    if (supabaseAvailable) {
                        Log.d(TAG, "Uploading image to Supabase Storage...")
                        imageUrl = uploadImageToStorage(bitmap)
                        Log.d(TAG, "Image uploaded, URL: $imageUrl")
                    }

                    val prompt = "Identify the type of waste in this image, estimate decomposition time, suggest a DIY reuse, and provide a caption for proper disposal."
                    Log.d(TAG, "Calling DeepSeek API for image analysis...")
                    val response = DeepSeekService.askDeepSeek(prompt, bitmap)
                    Log.d(TAG, "Received image analysis response: ${response.take(100)}...")

                    updateLastBotMessage(response)

                    if (supabaseAvailable && imageUrl.isNotEmpty()) {
                        saveImageMessageToSupabase(imageUrl)
                        saveToSupabase()
                    } else {
                        saveHistory()
                    }
                } else {
                    Log.e(TAG, "Failed to load bitmap from URI")
                    updateLastBotMessage("Could not load image. Please try again with a different image.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in analyzeImage", e)
                updateLastBotMessage("Failed to analyze image. Please try again.")
                saveHistory()
            }
        }
    }

    private suspend fun uploadImageToStorage(bitmap: Bitmap): String {
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            val imageBytes = outputStream.toByteArray()

            val fileName = "image_bkt/${UUID.randomUUID()}.jpg"
            Log.d(TAG, "Uploading image with filename: $fileName")

            supabase.storage.from("image-bkt").upload(
                path = fileName,
                data = imageBytes,
                upsert = false
            )

            val publicUrl = supabase.storage.from("image-bkt").publicUrl(fileName)
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
        if (currentSessionId == null || !supabaseAvailable) {
            Log.w(TAG, "Cannot save to Supabase: no session ID or Supabase unavailable")
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
                Log.d(TAG, "Saving user message to Supabase...")
                supabase.postgrest["chat_messages"].insert(userMessageData)
            }

            if (lastBotMessage != null) {
                val botMessageData = ChatMessageData(
                    session_id = currentSessionId!!,
                    message_type = "bot",
                    content = (lastBotMessage as ChatMessage.BotMessage).text
                )
                Log.d(TAG, "Saving bot message to Supabase...")
                supabase.postgrest["chat_messages"].insert(botMessageData)
            }
            Log.d(TAG, "Messages saved to Supabase successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save messages to Supabase", e)
            _isOfflineMode.value = true
            handleSupabaseError(e)
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
                val cleanTitle = generatedTitle.take(50).trim()

                Log.d(TAG, "Generated title: $cleanTitle")
                _sessionTitle.value = cleanTitle

                if (supabaseAvailable) {
                    updateSessionTitle(cleanTitle)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate session title", e)
                val fallbackTitle = firstMessage.take(30).trim() + if (firstMessage.length > 30) "..." else ""
                _sessionTitle.value = fallbackTitle
                if (supabaseAvailable) {
                    updateSessionTitle(fallbackTitle)
                }
            }
        }
    }

    private suspend fun updateSessionTitle(title: String) {
        if (currentSessionId != null && supabaseAvailable) {
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
                _isOfflineMode.value = true
            }
        }
    }

    private fun saveHistory() {
        val id = currentSessionId ?: return
        val arr = JSONArray().also {
            messageList.forEach { msg ->
                val o = JSONObject()
                when (msg) {
                    is ChatMessage.UserMessage -> { o.put("type","user"); o.put("text",msg.text) }
                    is ChatMessage.BotMessage  -> { o.put("type","bot");  o.put("text",msg.text); o.put("isLoading",msg.isLoading) }
                    is ChatMessage.ImagePreviewMessage -> { o.put("type","image"); o.put("uri", msg.uri.toString()) }
                }
                it.put(o)
            }
        }
        prefs.edit().putString("history_$id", arr.toString()).apply()
        Log.d(TAG, "Saved ${messageList.size} messages locally for session $id")
    }
}
