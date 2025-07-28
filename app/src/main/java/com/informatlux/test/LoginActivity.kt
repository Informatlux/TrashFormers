package com.informatlux.test

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Button
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    private var productSansTypeface: Typeface? = null

    // --- FIX: Add constants for SharedPreferences ---
    companion object {
        const val PREFS_NAME = "AuthPrefs"
        const val KEY_IS_LOGGED_IN = "isLoggedIn"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // --- FIX: Check login status before showing the UI ---
        if (isUserLoggedIn()) {
            navigateToMainActivity()
            // Return here to prevent the rest of onCreate from running
            return
        }

        setContentView(R.layout.activity_login)

        productSansTypeface = ResourcesCompat.getFont(this, R.font.product_sans)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupAnimations()
        setupToggleGroup()
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Important: This closes the LoginActivity so the user can't go back to it
    }

    private fun handleSignIn() {
        val emailOrUsername = findViewById<TextInputEditText>(R.id.input_email).text.toString()
        val password = findViewById<TextInputEditText>(R.id.input_password).text.toString()

        if (emailOrUsername.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email/username and password", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: Here you would put your actual authentication logic (e.g., check against an API)
        // For this example, we'll assume the login is always successful.

        Toast.makeText(this, "Authentication successful!", Toast.LENGTH_SHORT).show()

        // --- FIX: Save the logged-in state to SharedPreferences ---
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply() // Use apply() for asynchronous saving
        }

        navigateToMainActivity()
    }



    // --- All of your other functions are preserved below ---

    private fun setupAnimations() {
        val glassCard = findViewById<MaterialCardView>(R.id.glass_card)
        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.toggle_button_group)
        val title = findViewById<View>(R.id.text_title)
        val nameLayout = findViewById<View>(R.id.layout_name)
        val emailLayout = findViewById<View>(R.id.layout_email)
        val createButton = findViewById<View>(R.id.button_create_account)
        val socialLayout = findViewById<View>(R.id.layout_social)

        val views = listOf(glassCard, toggleGroup, title, nameLayout, emailLayout, createButton, socialLayout)
        views.forEach { view ->
            view.alpha = 0f
            view.translationY = 100f
        }
        animateViewsSequentially(views)
    }

    private fun animateViewsSequentially(views: List<View>) {
        views.forEachIndexed { index, view ->
            val animator = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                    ObjectAnimator.ofFloat(view, "translationY", 100f, 0f)
                )
                duration = 600
                startDelay = (index * 100).toLong()
                interpolator = OvershootInterpolator(0.5f)
            }
            animator.start()
        }
    }

    private fun setupToggleGroup() {
        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.toggle_button_group)
        val signUpButton = findViewById<MaterialButton>(R.id.button_sign_up)
        val signInButton = findViewById<MaterialButton>(R.id.button_sign_in)
        val title = findViewById<TextView>(R.id.text_title)
        val createButton = findViewById<MaterialButton>(R.id.button_create_account)
        val nameLayout = findViewById<View>(R.id.layout_name)
        val dividerText = findViewById<TextView>(R.id.text_divider)

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.button_sign_up -> {
                        signUpButton.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.apple_toggle_background))
                        signUpButton.setTextColor(getColor(R.color.apple_text_primary))
                        signUpButton.typeface = Typeface.create(productSansTypeface, Typeface.BOLD)
                        signInButton.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
                        signInButton.setTextColor(getColor(R.color.apple_text_secondary))
                        signInButton.typeface = Typeface.create(productSansTypeface, Typeface.NORMAL)
                        animateVisibility(nameLayout, true)
                        animateTextChange(title, "Create an account")
                        animateTextChange(createButton, "Create an account")
                        animateTextChange(dividerText, "OR SIGN UP WITH")
                        createButton.setOnClickListener { handleSignUp() }
                    }
                    R.id.button_sign_in -> {
                        signInButton.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.apple_toggle_background))
                        signInButton.setTextColor(getColor(R.color.apple_text_primary))
                        signInButton.typeface = Typeface.create(productSansTypeface, Typeface.BOLD)
                        signUpButton.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
                        signUpButton.setTextColor(getColor(R.color.apple_text_secondary))
                        signUpButton.typeface = Typeface.create(productSansTypeface, Typeface.NORMAL)
                        animateVisibility(nameLayout, false)
                        animateTextChange(title, "Welcome back")
                        animateTextChange(createButton, "Sign in")
                        animateTextChange(dividerText, "OR SIGN IN WITH")
                        createButton.setOnClickListener { handleSignIn() }
                    }
                }
            }
        }
        toggleGroup.check(R.id.button_sign_up)
    }

    private fun animateTextChange(view: TextView, newText: String) {
        val fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).apply { duration = 150; interpolator = AccelerateDecelerateInterpolator() }
        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply { duration = 150; interpolator = AccelerateDecelerateInterpolator() }
        fadeOut.start()
        fadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.text = newText
                fadeIn.start()
            }
        })
    }

    private fun animateVisibility(view: View, show: Boolean) {
        if (show && view.visibility == View.VISIBLE || !show && view.visibility == View.GONE) return
        if (show) {
            view.visibility = View.VISIBLE
            view.alpha = 0f
            view.translationY = -30f
            val animator = AnimatorSet().apply {
                playTogether(ObjectAnimator.ofFloat(view, "alpha", 0f, 1f), ObjectAnimator.ofFloat(view, "translationY", -30f, 0f))
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
            }
            animator.start()
        } else {
            val animator = AnimatorSet().apply {
                playTogether(ObjectAnimator.ofFloat(view, "alpha", 1f, 0f), ObjectAnimator.ofFloat(view, "translationY", 0f, -30f))
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
            }
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                }
            })
            animator.start()
        }
    }

    private fun handleSignUp() {
        // ... (This function remains unchanged)
        val firstName = findViewById<TextInputEditText>(R.id.input_first_name).text.toString()
        val lastName = findViewById<TextInputEditText>(R.id.input_last_name).text.toString()
        val email = findViewById<TextInputEditText>(R.id.input_email).text.toString()
        val password = findViewById<TextInputEditText>(R.id.input_password).text.toString()

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "Sign up functionality to be implemented", Toast.LENGTH_SHORT).show()
    }
}