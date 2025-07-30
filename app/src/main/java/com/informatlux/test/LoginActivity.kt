package com.informatlux.test

import android.animation.*
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.regex.Pattern

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginActivity"
        const val PREFS_NAME = "AuthPrefs"
        const val KEY_IS_LOGGED_IN = "isLoggedIn"

        // Replace with your Supabase credentials
        private const val SUPABASE_URL = "https://jedpwwxjrsejumyqyrgx.supabase.co"
        private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImplZHB3d3hqcnNlanVteXF5cmd4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTM4NzYzMzQsImV4cCI6MjA2OTQ1MjMzNH0.x9iFEmjd8ldd_llmc70ZfVqV3BBsUx1MSLnZbFCPlxI"
    }

    private var productSansTypeface: Typeface? = null
    private lateinit var supabase: SupabaseClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val passwordInputLayout = findViewById<TextInputLayout>(R.id.layout_password)
        val passwordInput = findViewById<TextInputEditText>(R.id.input_password)

        passwordInputLayout.setEndIconOnClickListener {
            val isPasswordVisible =
                passwordInput.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)

            if (isPasswordVisible) {
                // Hide password
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                // Optionally update icon manually (if you have custom icons)
                passwordInputLayout.endIconDrawable = ContextCompat.getDrawable(this, R.drawable.invisible_icon)
            } else {
                // Show password
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            }
            // Move cursor to the end after input type change
            passwordInput.setSelection(passwordInput.text?.length ?: 0)
        }



        // Initialize Supabase client with proper configuration
        supabase = createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
            })

            install(Auth) {
                alwaysAutoRefresh = false
                autoLoadFromStorage = false
            }

            install(Postgrest) {
                defaultSchema = "public"
            }
        }

        // Check if already logged in
        if (getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_IS_LOGGED_IN, false)
        ) {
            navigateToMain()
            return
        }

        productSansTypeface = ResourcesCompat.getFont(this, R.font.product_sans)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        setupAnimations()
        setupToggleGroup()
    }

    // ————— SIGN UP —————
    private fun handleSignUp() {
        val firstName = findViewById<TextInputEditText>(R.id.input_first_name).text.toString().trim()
        val lastName  = findViewById<TextInputEditText>(R.id.input_last_name).text.toString().trim()
        val email     = findViewById<TextInputEditText>(R.id.input_email).text.toString().trim()
        val password  = findViewById<TextInputEditText>(R.id.input_password).text.toString()

        if (firstName.isEmpty() || lastName.isEmpty() ||
            email.isEmpty() || password.isEmpty()
        ) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Password policy: ≥6 chars, upper, lower, digit, special
        val regex = Pattern.compile(
            """^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])\S{6,}$"""
        )
        if (!regex.matcher(password).matches()) {
            Toast.makeText(
                this,
                "Password must be ≥6 chars, include upper, lower, digit & special",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Show loading
        val createBtn = findViewById<MaterialButton>(R.id.button_create_account)
        createBtn.isEnabled = false
        createBtn.text = "Creating Account..."

        lifecycleScope.launch {
            try {
                // Sign up with Supabase using the correct API
                val response = supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    // Add user metadata
                    data = buildJsonObject {
                        put("first_name", firstName)
                        put("last_name", lastName)
                    }
                }

                // Check if user was created successfully
                response.let { authResult ->
                    val user = authResult
                    if (user != null) {
                        // Insert user profile into profiles table
                        try {
                            supabase.postgrest["profiles"].insert(
                                listOf(
                                    mapOf(
                                        "id" to user.id,
                                        "first_name" to firstName,
                                        "last_name" to lastName,
                                        "email" to email
                                    )
                                )
                            )

                            Toast.makeText(
                                this@LoginActivity,
                                "Account Successfully Created!",
                                Toast.LENGTH_LONG
                            ).show()

                            saveUserName(firstName, lastName)
                            saveLoginState()
                            navigateToMain()
                        } catch (e: Exception) {
                            Log.e(TAG, "Profile insert error", e)
                            Toast.makeText(
                                this@LoginActivity,
                                "Account created but profile setup failed: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Failed to create account. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sign up error", e)
                val errorMessage = when {
                    e.message?.contains("User already registered") == true -> "Email already exists. Please sign in instead."
                    e.message?.contains("Invalid email") == true -> "Please enter a valid email address."
                    e.message?.contains("Password") == true -> "Password requirements not met."
                    else -> "Sign up failed: ${e.message}"
                }
                Toast.makeText(
                    this@LoginActivity,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                // Reset button
                createBtn.isEnabled = true
                val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.toggle_button_group)
                if (toggleGroup.checkedButtonId == R.id.button_sign_up) {
                    createBtn.text = "Create an account"
                } else {
                    createBtn.text = "Sign in"
                }
            }
        }
    }

    // ————— SIGN IN —————
    private fun handleSignIn() {
        val email    = findViewById<TextInputEditText>(R.id.input_email).text.toString().trim()
        val password = findViewById<TextInputEditText>(R.id.input_password).text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email & password", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        val createBtn = findViewById<MaterialButton>(R.id.button_create_account)
        createBtn.isEnabled = false
        createBtn.text = "Signing In..."

        lifecycleScope.launch {
            try {
                // Sign in with Supabase using the correct API
                val response = supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                // Check if sign in was successful
                response.let { authResult ->
                    val user = authResult
                    if (user != null) {
                        Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                        saveLoginState()
                        navigateToMain()
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Invalid email or password",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sign in error", e)
                val errorMessage = when {
                    e.message?.contains("Invalid login credentials") == true -> "Invalid email or password."
                    e.message?.contains("Email not confirmed") == true -> "Please check your email and confirm your account."
                    e.message?.contains("Too many requests") == true -> "Too many attempts. Please try again later."
                    else -> "Sign in failed: ${e.message}"
                }
                Toast.makeText(
                    this@LoginActivity,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                // Reset button
                createBtn.isEnabled = true
                val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.toggle_button_group)
                if (toggleGroup.checkedButtonId == R.id.button_sign_up) {
                    createBtn.text = "Create an account"
                } else {
                    createBtn.text = "Sign in"
                }
            }
        }
    }

    private fun saveUserName(firstName: String, lastName: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("user_first_name", firstName)
            .putString("user_last_name", lastName)
            .apply()
    }

    private fun saveLoginState() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_IS_LOGGED_IN, true).apply()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    // ————— UI ANIMATIONS & TOGGLE —————

    private fun setupAnimations() {
        val views = listOf(
            findViewById<MaterialCardView>(R.id.glass_card),
            findViewById<MaterialButtonToggleGroup>(R.id.toggle_button_group),
            findViewById<View>(R.id.text_title),
            findViewById<View>(R.id.layout_name),
            findViewById<View>(R.id.layout_email),
            findViewById<View>(R.id.button_create_account)
        )
        views.forEach { it.alpha = 0f; it.translationY = 100f }
        views.forEachIndexed { i, v ->
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(v, "alpha", 0f, 1f),
                    ObjectAnimator.ofFloat(v, "translationY", 100f, 0f)
                )
                duration = 600
                startDelay = (i * 100).toLong()
                interpolator = OvershootInterpolator(0.5f)
                start()
            }
        }
    }

    private fun setupToggleGroup() {
        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.toggle_button_group)
            .apply { isSingleSelection = true }

        val signUpBtn  = findViewById<MaterialButton>(R.id.button_sign_up)
        val signInBtn  = findViewById<MaterialButton>(R.id.button_sign_in)
        val titleView  = findViewById<TextView>(R.id.text_title)
        val nameLayout = findViewById<View>(R.id.layout_name)
        val createBtn  = findViewById<MaterialButton>(R.id.button_create_account)

        // Always update UI and click listeners on toggle change
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            // Reset both buttons to transparent background
            signUpBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            signInBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)

            if (checkedId == R.id.button_sign_up) {
                // Sign-up active styling
                signUpBtn.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getColor(R.color.apple_toggle_background))
                )
                signUpBtn.setTextColor(getColor(R.color.apple_text_primary))
                signInBtn.setTextColor(getColor(R.color.apple_text_secondary))

                titleView.text = "Create an account"
                nameLayout.visibility = View.VISIBLE
                createBtn.text = "Create an account"
                createBtn.setOnClickListener { handleSignUp() }
            } else if (checkedId == R.id.button_sign_in) {
                // Sign-in active styling
                signInBtn.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getColor(R.color.apple_toggle_background))
                )
                signInBtn.setTextColor(getColor(R.color.apple_text_primary))
                signUpBtn.setTextColor(getColor(R.color.apple_text_secondary))

                titleView.text = "Welcome back"
                nameLayout.visibility = View.GONE
                createBtn.text = "Sign in"
                createBtn.setOnClickListener { handleSignIn() }
            }
        }

        // Set default checked (this triggers listener)
        toggleGroup.check(R.id.button_sign_up)
    }
}