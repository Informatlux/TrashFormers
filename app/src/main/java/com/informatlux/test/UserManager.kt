package com.informatlux.test

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object UserManager {
    private const val TAG = "UserManager"
    private const val PREFS_NAME = "user_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"

    private lateinit var prefs: SharedPreferences
    private lateinit var supabase: SupabaseClient

    /** Call once from Application or first launched Activity */
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
        }
        Log.d(TAG, "Initialized UserManager and Supabase client")
    }

    /** Get current userId asynchronously, returns stored fallback if needed */
    suspend fun getCurrentUserId(): String = withContext(Dispatchers.IO) {
        try {
            val session = supabase.auth.currentSessionOrNull()
            session?.user?.id ?: getStoredUserId()
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching session, using stored user ID", e)
            getStoredUserId()
        }
    }

    /** Synchronously get stored cached user ID */
    fun getStoredUserId(): String = prefs.getString(KEY_USER_ID, "default_user") ?: "default_user"

    /** Update stored user ID and optional email */
    fun setCurrentUser(userId: String, email: String? = null) {
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, email)
            .apply()
        Log.d(TAG, "User set in prefs: $userId")
    }

    /** Get cached user email */
    fun getCurrentUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)

    /** Check if user is logged in asynchronously */
    suspend fun isUserLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        try {
            supabase.auth.currentSessionOrNull() != null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check login status", e)
            false
        }
    }

    /** Clear cached user data */
    fun clearUserData() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Cleared user data from SharedPreferences")
    }
}
