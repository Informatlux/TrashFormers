package com.informatlux.test

import android.os.Bundle
import android.view.View
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.informatlux.test.ScoreManager

class ArticleActivity : AppCompatActivity() {

    private val userId = "user1" // Replace with actual user ID logic

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_article)

        // Handles the status bar padding for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        setupClickListeners()
        displayUserFullName()
        setupEntryAnimations()
        updateEcoPointsDisplay()
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
        val prefs = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val firstName = prefs.getString("user_first_name", "") ?: ""
        val lastName = prefs.getString("user_last_name", "") ?: ""

        val fullName = when {
            firstName.isNotEmpty() && lastName.isNotEmpty() -> "$firstName $lastName"
            firstName.isNotEmpty() -> firstName
            lastName.isNotEmpty() -> lastName
            else -> "User"
        }

        findViewById<TextView>(R.id.user_name_text).text = fullName
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