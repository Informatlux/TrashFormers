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
import kotlinx.coroutines.launch

class AIViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AIViewModel"

    private val _chatSessions = MutableLiveData<List<ChatSession>>(emptyList())
    val chatSessions: LiveData<List<ChatSession>> = _chatSessions

    private lateinit var supabase: SupabaseClient
    private val userId = "user1" // Replace with actual user ID

    init {
        Log.d(TAG, "Initializing AIViewModel")
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
                install(Auth)
                install(Postgrest) {
                    defaultSchema = "api" // CHANGED FROM "public" TO "api"
                }
            }
            Log.d(TAG, "Supabase client initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Supabase client", e)
        }
    }

    private fun loadChatSessions() {
        Log.d(TAG, "Loading chat sessions for user: $userId")
        viewModelScope.launch {
            try {
                Log.d(TAG, "Fetching chat sessions from Supabase...")
                val response = supabase.postgrest["chat_sessions"]
                    .select(columns = Columns.list("*")) {
                        filter {
                            eq("user_id", userId)
                        }
                        order("created_at", order = Order.DESCENDING)
                    }

                val sessions = response.decodeList<ChatSession>()
                Log.d(TAG, "Loaded ${sessions.size} chat sessions from Supabase")

                _chatSessions.value = sessions
                Log.d(TAG, "Chat sessions updated in LiveData")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load chat sessions from Supabase", e)
                _chatSessions.value = emptyList()
            }
        }
    }
}