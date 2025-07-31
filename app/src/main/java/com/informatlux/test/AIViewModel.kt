package com.informatlux.test

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

class AIViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AIViewModel"

    private val _chatSessions = MutableLiveData<List<ChatSession>>(emptyList())
    val chatSessions: LiveData<List<ChatSession>> = _chatSessions

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val prefs = application.getSharedPreferences("chat_history", Context.MODE_PRIVATE)
    private lateinit var supabase: SupabaseClient
    private var supabaseAvailable = false

    private var userId: String = "default_user" // Default fallback userId

    init {
        viewModelScope.launch {
            // Initialize UserManager and await getting userId asynchronously
            UserManager.initialize(getApplication())
            userId = UserManager.getCurrentUserId()

            initializeSupabase()
            loadChatSessions()
        }
    }

    /**
     * Initialize Supabase client with your credentials
     */
    private fun initializeSupabase() {
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
                    defaultSchema = "api" // Change to "api" only if your tables are there
                }

                install(Storage)
            }
            supabaseAvailable = true
            Log.d(TAG, "Supabase client initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Supabase client", e)
            supabaseAvailable = false
            _errorMessage.value = "Failed to initialize database connection"
        }
    }

    /**
     * Load chat sessions either from Supabase or local shared prefs as fallback.
     */
    fun loadChatSessions() {
        _isLoading.value = true
        _errorMessage.value = null

        if (supabaseAvailable) {
            loadSessionsFromSupabase()
        } else {
            loadSessionsFromLocal()
        }
    }

    /**
     * Load sessions from Supabase asynchronously.
     */
    private fun loadSessionsFromSupabase() {
        viewModelScope.launch {
            try {
                val sessions = supabase.postgrest["chat_sessions"]
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<ChatSession>()

                _chatSessions.value = sessions
                _isLoading.value = true
                Log.d(TAG, "Loaded ${sessions.size} sessions from Supabase")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load sessions from Supabase, falling back to local", e)
                _errorMessage.value = "Failed to load sessions from server, using offline data."
                loadSessionsFromLocal()
            }
        }
    }

    /**
     * Load sessions from local SharedPreferences cache.
     */
    private fun loadSessionsFromLocal() {
        viewModelScope.launch(Dispatchers.IO) {
            val sessions = mutableListOf<ChatSession>()
            val allPrefs = prefs.all

            allPrefs.keys.forEach { key ->
                if (key.startsWith("title_")) {
                    val sessionId = key.removePrefix("title_")
                    val title = prefs.getString(key, "Chat Session") ?: "Chat Session"
                    val historyKey = "history_$sessionId"

                    if (prefs.contains(historyKey)) {
                        sessions.add(
                            ChatSession(
                                id = sessionId,
                                user_id = userId,
                                title = title,
                                created_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()).format(Date())
                            )
                        )
                    }
                }
            }

            // Sort by creation date descending, or fallback by title descending if dates not available
            sessions.sortByDescending { it.title }

            withContext(Dispatchers.Main) {
                _chatSessions.value = sessions
                _isLoading.value = false
                Log.d(TAG, "Loaded ${sessions.size} sessions from local storage")
            }
        }
    }

    /**
     * Delete a chat session and its messages.
     */
    fun deleteSession(sessionId: String) {
        _errorMessage.value = null
        if (supabaseAvailable) {
            deleteSessionFromSupabase(sessionId)
        }
        deleteSessionFromLocal(sessionId)
        loadChatSessions() // Refresh after deletion
    }

    private fun deleteSessionFromSupabase(sessionId: String) {
        viewModelScope.launch {
            try {
                supabase.postgrest["chat_messages"].delete {
                    filter { eq("session_id", sessionId) }
                }
                supabase.postgrest["chat_sessions"].delete {
                    filter {
                        eq("id", sessionId)
                        eq("user_id", userId)
                    }
                }
                Log.d(TAG, "Session deleted from Supabase")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete session from Supabase", e)
                _errorMessage.value = "Failed to delete session from server."
            }
        }
    }

    private fun deleteSessionFromLocal(sessionId: String) {
        prefs.edit()
            .remove("history_$sessionId")
            .remove("title_$sessionId")
            .apply()
        Log.d(TAG, "Session deleted from local storage")
    }

    /**
     * Clear any error message posted.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
