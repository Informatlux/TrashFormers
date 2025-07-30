package com.informatlux.test

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.informatlux.test.ScoreManager

class AIActivity : AppCompatActivity() {

    // Use the 'by viewModels()' delegate to get a lifecycle-aware ViewModel instance
    private val viewModel: AIViewModel by viewModels()

    private lateinit var carouselRecyclerView: RecyclerView
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var emptyHistoryText: TextView
    private lateinit var carouselAdapter: CarouselAdapter
    private lateinit var historyAdapter: HistoryAdapter

    private val userId = "user1" // Replace with actual user ID logic

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ai_home)

        // Handles status bar padding for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        initializeViews()
        setupRecyclerViews()
        setupObservers()
        setupClickListeners()
        setupEntryAnimations()
        updateEcoPointsDisplay()

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
                    true
                }
                R.id.nav_article -> {
                    startActivity(Intent(this, ArticleActivity::class.java))
                    finish()
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
        bottomNav.selectedItemId = R.id.nav_ai
    }

    private fun initializeViews() {
        carouselRecyclerView = findViewById(R.id.carousel_recycler_view)
        historyRecyclerView = findViewById(R.id.history_recycler_view)
        emptyHistoryText = findViewById(R.id.empty_history_text)
    }

    private fun setupRecyclerViews() {
        // Carousel Setup
        carouselRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // History Setup
        historyRecyclerView.layoutManager = LinearLayoutManager(this)
        historyRecyclerView.isNestedScrollingEnabled = false // Important for smooth scrolling inside a ScrollView
    }

    private fun setupObservers() {
        // Observe the carousel items LiveData
        viewModel.carouselItems.observe(this) { items ->
            carouselAdapter = CarouselAdapter(items)
            carouselRecyclerView.adapter = carouselAdapter
        }

        // Observe the history items LiveData
        viewModel.historyItems.observe(this) { items ->
            historyAdapter = HistoryAdapter(items)
            historyRecyclerView.adapter = historyAdapter
        }

        // Observe the empty state LiveData
        viewModel.isHistoryEmpty.observe(this) { isEmpty ->
            emptyHistoryText.visibility = if (isEmpty) View.VISIBLE else View.GONE
            historyRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        findViewById<MaterialButton>(R.id.btn_new_chat).setOnClickListener {
            startActivity(Intent(this, AIChatActivity::class.java))
        }

        findViewById<TextView>(R.id.btn_see_all).setOnClickListener {
            Toast.makeText(this, "See all history clicked", Toast.LENGTH_SHORT).show()
        }

        // Setup Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        // bottomNav.selectedItemId = R.id.nav_scan // Example: Set the correct item as selected
        bottomNav.setOnItemSelectedListener {
            // Handle navigation here
            true
        }
    }

    private fun setupEntryAnimations() {
        val viewsToAnimate = listOf(
            findViewById<View>(R.id.btn_new_chat),
            findViewById<View>(R.id.carousel_recycler_view),
            findViewById<View>(R.id.history_recycler_view)
        )

        viewsToAnimate.forEach { view ->
            view.alpha = 0f
            view.translationY = 50f
        }

        viewsToAnimate.forEachIndexed { index, view ->
            val animator = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                    ObjectAnimator.ofFloat(view, "translationY", 50f, 0f)
                )
                duration = 600
                startDelay = (index * 100).toLong()
                interpolator = DecelerateInterpolator()
            }
            animator.start()
        }
    }

    private fun updateEcoPointsDisplay() {
        val points = ScoreManager.getScore(userId)
    }

    // Example: When user interacts with carousel (e.g., reads an article)
    private fun onCarouselAction() {
        ScoreManager.addPoints(userId, ScoreManager.POINTS_ARTICLE_READ)
        updateEcoPointsDisplay()
    }

    // Example: When user interacts with history (e.g., participates in an event)
    private fun onHistoryAction() {
        ScoreManager.addPoints(userId, ScoreManager.POINTS_EVENT_PARTICIPATION)
        updateEcoPointsDisplay()
    }
}