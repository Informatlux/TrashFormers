package com.informatlux.test

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class AIViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AIViewModel"

    private val _chatSessions = MutableLiveData<List<ChatSession>>(emptyList())
    val chatSessions: LiveData<List<ChatSession>> = _chatSessions

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private lateinit var supabase: SupabaseClient

    // Use UserManager for proper user ID management
    private val userId: String by lazy {
        UserManager.getCurrentUserId()
    }

    init {
        Log.d(TAG, "Initializing AIViewModel")
        // Initialize UserManager
        UserManager.initialize(application)
        initializeSupabase()
        loadChatSessions()
    }

    private fun initializeSupabase() {
        Log.d(TAG, "Initializing Supabase client for AIViewModel")
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
                    // FIXED: Use "api" schema instead of "public"
                    defaultSchema = "api"
                }
            }
            Log.d(TAG, "Supabase client initialized successfully with user ID: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Supabase client", e)
            _errorMessage.value = "Failed to initialize database connection"
        }
    }

    fun loadChatSessions() {
        Log.d(TAG, "Loading chat sessions for user: $userId")
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "Fetching chat sessions from Supabase...")
                val response = supabase.postgrest["chat_sessions"]
                    .select(columns = Columns.list("*")) {
                        filter {
                            eq("user_id", userId) // Now using proper UUID
                        }
                        order("created_at", order = Order.DESCENDING)
                    }

                val sessions = response.decodeList<ChatSession>()
                Log.d(TAG, "Loaded ${sessions.size} chat sessions from Supabase")

                _chatSessions.value = sessions
                _isLoading.value = false
                Log.d(TAG, "Chat sessions updated in LiveData")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load chat sessions from Supabase", e)
                _chatSessions.value = emptyList()
                _isLoading.value = false
                _errorMessage.value = "Failed to load chat sessions. Working in offline mode."
            }
        }
    }

    fun deleteSession(sessionId: String) {
        Log.d(TAG, "Deleting session: $sessionId")
        viewModelScope.launch {
            try {
                // Delete messages first (if your database has foreign key constraints)
                supabase.postgrest["chat_messages"]
                    .delete {
                        filter {
                            eq("session_id", sessionId)
                        }
                    }

                // Delete the session
                supabase.postgrest["chat_sessions"]
                    .delete {
                        filter {
                            eq("id", sessionId)
                        }
                    }

                Log.d(TAG, "Session deleted successfully")

                // Refresh the list
                loadChatSessions()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete session", e)
                _errorMessage.value = "Failed to delete session"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}