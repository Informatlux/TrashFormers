package com.informatlux.test

import android.os.Bundle
import android.view.View
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.informatlux.test.ScoreManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ArticleActivity : AppCompatActivity() {

    private lateinit var supabase: SupabaseClient
    private var userId: String = ""

    companion object{
        private const val SUPABASE_URL = "https://jedpwwxjrsejumyqyrgx.supabase.co"
        private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImplZHB3d3hqcnNlanVteXF5cmd4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTM4NzYzMzQsImV4cCI6MjA2OTQ1MjMzNH0.x9iFEmjd8ldd_llmc70ZfVqV3BBsUx1MSLnZbFCPlxI"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_article)
        UserManager.initialize(application)

        // 1️⃣ Initialize Supabase immediately
        supabase = createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
            install(Auth) { alwaysAutoRefresh = false; autoLoadFromStorage = false }
            install(Postgrest) { defaultSchema = "public" }
        }

        // 2️⃣ Fetch userId and do user‐dependent UI setup
        lifecycleScope.launch {
            try {
                userId = UserManager.getCurrentUserId()
                displayUserFullName()
                updateEcoPointsDisplay()
            } catch (e: Exception) {
                Log.e("ArticleActivity", "Failed to load userId", e)
            }
        }

        setupClickListeners()
        setupEntryAnimations()
    }


    private fun setupClickListeners() {
        // --- Header ---
        findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.profile_image).setOnClickListener {}


        // ... Add listeners for all cards and "See More" buttons as needed ...

        // --- Bottom Navigation ---
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_maps -> {
                    startActivity(Intent(this, MapsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_ai -> {
                    startActivity(Intent(this, AIActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_article -> {
                    true
                }
                R.id.nav_more -> {
                    startActivity(Intent(this, MoreActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
        bottomNav.selectedItemId = R.id.nav_article
    }

    private fun setupEntryAnimations() {
        // Find all the major cards in the layout
        val pointsCard = findViewById<MaterialCardView>(R.id.total_points_card)
        // ... find other cards by their ID ...

        val viewsToAnimate = listOfNotNull(pointsCard, /* ... other cards ... */)

        // Set the initial state for the animation
        viewsToAnimate.forEach { view ->
            view.alpha = 0f
            view.translationY = 50f
        }

        // Animate each view with a slight delay
        viewsToAnimate.forEachIndexed { index, view ->
            val animator = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                    ObjectAnimator.ofFloat(view, "translationY", 50f, 0f)
                )
                duration = 600
                startDelay = (index * 120).toLong()
                interpolator = DecelerateInterpolator()
            }
            animator.start()
        }
    }

    private fun displayUserFullName() {
        // Launch in a Coroutine (call this from onCreate or similar)

        lifecycleScope.launch {
            try {
                // 1. Get authenticated user's ID
                val session = supabase.auth.currentSessionOrNull()
                val userId = session?.user?.id
                if (userId == null) {
                    findViewById<TextView>(R.id.user_name_text).text = "User"
                    return@launch
                }

                // 2. Query Supabase "profiles" table for this user ID
                val result = supabase
                    .postgrest["profiles"]
                    .select {
                        filter { eq("id", userId) }
                        limit(1)
                    }
                    .decodeList<Map<String, Any>>() // use your data model if you have one

                if (result.isNotEmpty()) {
                    val user = result.first()
                    val firstName = user["first_name"] as? String ?: ""
                    val lastName = user["last_name"] as? String ?: ""
                    val fullName = when {
                        firstName.isNotEmpty() && lastName.isNotEmpty() -> "$firstName $lastName"
                        firstName.isNotEmpty() -> firstName
                        lastName.isNotEmpty() -> lastName
                        else -> "User"
                    }
                    findViewById<TextView>(R.id.user_name_text).text = fullName
                } else {
                    findViewById<TextView>(R.id.user_name_text).text = "User"
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching user profile", e)
                findViewById<TextView>(R.id.user_name_text).text = "User"
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Example: When user reads an article
    private fun onArticleRead() {
        ScoreManager.addPoints(userId, ScoreManager.POINTS_ARTICLE_READ)
        updateEcoPointsDisplay()
    }
    private fun updateEcoPointsDisplay() {
        val points = ScoreManager.getScore(userId)
        findViewById<TextView>(R.id.total_points_text).text = points.toString()
    }
}