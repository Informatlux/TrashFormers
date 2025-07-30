package com.informatlux.test

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView


class MoreActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_more)

        setupToolbar()
        setupRecyclerView()
        runEntryAnimations()

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
                    startActivity(Intent(this, ArticleActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_more -> {

                    true
                }
                else -> false
            }
        }
        bottomNav.selectedItemId = R.id.nav_more
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.more_menu_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val menuItems = listOf(
            MoreMenuItem(MenuItemKey.SETTINGS, R.drawable.settings_icon, "Settings", "Manage your preferences"),
            MoreMenuItem(MenuItemKey.LEADERBOARD, R.drawable.leaderboard_icon, "Leaderboard", "See top community members"),
            MoreMenuItem(MenuItemKey.EVENTS, R.drawable.events_icon, "Events", "Join community events"),
            MoreMenuItem(MenuItemKey.ABOUT, R.drawable.info_icon, "About", "Learn more about the app"),
            MoreMenuItem(MenuItemKey.LOGOUT, R.drawable.logout_icon, "Logout", "Sign out of your account")
        )

        val adapter = MoreMenuAdapter(menuItems) { key ->
            handleMenuItemClick(key)
        }
        recyclerView.adapter = adapter
    }

    private fun handleMenuItemClick(key: MenuItemKey) {
        when (key) {
            MenuItemKey.SETTINGS -> startActivity(Intent(this, SettingsActivity::class.java))
            MenuItemKey.LEADERBOARD -> startActivity(Intent(this, LeaderboardActivity::class.java))
            MenuItemKey.EVENTS -> startActivity(Intent(this, EventsActivity::class.java))
            MenuItemKey.ABOUT -> showToast("About section coming soon!")
            MenuItemKey.LOGOUT -> showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                // This assumes you have a LoginActivity with these constants defined
                val sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    putBoolean(LoginActivity.KEY_IS_LOGGED_IN, false)
                    apply()
                }
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun runEntryAnimations() {
        val recyclerView = findViewById<RecyclerView>(R.id.more_menu_recycler_view)
        recyclerView.post {
            for (i in 0 until recyclerView.childCount) {
                val view: View = recyclerView.getChildAt(i)
                view.alpha = 0f
                view.translationY = 50f
                val animator = AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                        ObjectAnimator.ofFloat(view, "translationY", 50f, 0f)
                    )
                    duration = 400
                    startDelay = (i * 70).toLong()
                    interpolator = DecelerateInterpolator()
                }
                animator.start()
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}