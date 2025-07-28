package com.informatlux.test

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // Your existing AI and state variables are preserved.
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var aiService: AIService // Assuming you have this class in your project
    private var isVoiceModeEnabled = false
    private val conversationHistory = mutableListOf<String>()
    private var currentEcoPoints = 12341

    // Your existing ActivityResultLaunchers are preserved.
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            handleImageCapture(result.data)
        }
    }

    private val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            handleSpeechResult(result.data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize services
        textToSpeech = TextToSpeech(this, this)
        aiService = AIService(this) // Make sure you have AIService.kt in your project

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        findViewById<Button>(R.id.btn_logout).setOnClickListener {
            logoutUser()
        }

        setupUIAndClickListeners()
        setupEntryAnimations()
        updateGreeting()
        requestPermissions()
    }

    private fun logoutUser() {
        // Clear the saved login state
        val sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean(LoginActivity.KEY_IS_LOGGED_IN, false)
            apply()
        }

        // Navigate back to the LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Close MainActivity
    }

    private fun setupUIAndClickListeners() {
        // --- Header ---
        findViewById<ShapeableImageView>(R.id.profile_image).setOnClickListener { showToast("Profile clicked") }
        findViewById<ImageView>(R.id.notification_button).setOnClickListener { showToast("Notifications clicked") }

        // --- Cards ---
        findViewById<MaterialCardView>(R.id.total_points_card).setOnClickListener { showEcoScoreDetails() }
        findViewById<MaterialCardView>(R.id.saved_co2_card).setOnClickListener { showCO2Details() }
        findViewById<MaterialCardView>(R.id.trash_cycle_card).setOnClickListener { showTrashCycleDetails() }

        // Updated this the ecobud_card click to launch MapsActivity
        findViewById<MaterialCardView>(R.id.ecobud_card).setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }

        findViewById<MaterialCardView>(R.id.challenge_card).setOnClickListener { showChallengeDetails("Recycle Challenge: Beat Heat!") }

        // --- "See More" Links ---
        findViewById<TextView>(R.id.see_more_challenges).setOnClickListener { showAllChallenges() }
        findViewById<TextView>(R.id.see_more_activity).setOnClickListener { showAllActivity() }

        // --- Bottom Navigation ---
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showToast("Home selected")
                    true
                }
                R.id.nav_maps -> {
                    startActivity(Intent(this, MapsActivity::class.java))
                    true
                }
                R.id.nav_scan -> {
                    startActivity(Intent(this, AIActivity::class.java))
                    true
                }
                R.id.nav_article -> {
                    startActivity(Intent(this, ArticleActivity::class.java))
                    finish()  // Optional: close current activity if needed
                    true
                }
                R.id.nav_profile -> {
                    showToast("Profile feature coming soon")
                    true
                }
                else -> false
            }
        }
        bottomNav.selectedItemId = R.id.nav_home
    }


    private fun setupEntryAnimations() {
        try {
            val totalPointsCard = findViewById<MaterialCardView>(R.id.total_points_card)
            val ecobudCard = findViewById<MaterialCardView>(R.id.ecobud_card)
            val challengeCard = findViewById<MaterialCardView>(R.id.challenge_card)
            val cards = listOfNotNull(totalPointsCard, ecobudCard, challengeCard)

            cards.forEach { card ->
                card.alpha = 0f
                card.translationY = 60f
            }

            cards.forEachIndexed { index, card ->
                val animator = AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(card, "alpha", 0f, 1f),
                        ObjectAnimator.ofFloat(card, "translationY", 60f, 0f)
                    )
                    duration = 600
                    startDelay = (index * 150).toLong()
                    interpolator = DecelerateInterpolator()
                }
                animator.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateGreeting() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
        findViewById<TextView>(R.id.greeting_text).text = "$greeting 👋"
    }

    // --- All of your original AI and helper functions are preserved below ---

    private fun showClassificationOptions() {
        val options = arrayOf("📷 Take Photo", "📝 Describe Item")
        AlertDialog.Builder(this)
            .setTitle("🌿 Waste Classification")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> captureImageForClassification()
                    1 -> showTextClassificationDialog()
                }
            }
            .show()
    }

    private fun startEcoBot() {
        val options = arrayOf("Voice Chat", "Text Chat", "Toggle Voice Mode")
        AlertDialog.Builder(this)
            .setTitle("🌿 AI EcoBot")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startVoiceChat()
                    1 -> startTextChat()
                    2 -> toggleVoiceMode()
                }
            }
            .show()
    }

    private fun showEcoScoreDetails() {
        val details = """
            🏆 Your EcoScore: ${String.format("%,d", currentEcoPoints)} points
            
            Recent Achievements:
            🏆 Recycling Champion (100 pts)
            🌱 Compost Expert (75 pts)
            ♻️ Zero Waste Day (50 pts)
            
            Next Goal: Reach 15,000 points
            Complete 3 more challenges to unlock new badges!
        """.trimIndent()
        showResponseDialog("EcoScore Details", details)
    }

    private fun captureImageForClassification() {
        if (checkPermission(Manifest.permission.CAMERA)) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(intent)
        }
    }

    private fun showTextClassificationDialog() {
        showInputDialog("Describe Item", "Describe the item you want to classify...") { description ->
            classifyWasteItem(description)
        }
    }

    private fun classifyWasteItem(description: String) {
        lifecycleScope.launch {
            try {
                showProgressDialog("Analyzing waste item...")
                val classification = aiService.classifyWaste(description)
                hideProgressDialog()
                showResponseDialog("AI Classification Result", classification)

                val pointsRegex = "EcoPoints: (\\d+)".toRegex()
                val points = pointsRegex.find(classification)?.groupValues?.get(1)?.toIntOrNull() ?: 10
                addEcoPoints(points)
            } catch (e: Exception) {
                hideProgressDialog()
                showErrorDialog("Classification Error", "Failed to classify waste item. Please try again.")
            }
        }
    }

    private fun startVoiceChat() {
        if (checkPermission(Manifest.permission.RECORD_AUDIO)) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask me about waste management...")
            }
            speechLauncher.launch(intent)
        }
    }

    private fun startTextChat() {
        showInputDialog("AI EcoBot", "Ask me about waste management...") { input ->
            processEcoBotQuery(input)
        }
    }

    private fun toggleVoiceMode() {
        isVoiceModeEnabled = !isVoiceModeEnabled
        showToast("Voice mode ${if (isVoiceModeEnabled) "enabled" else "disabled"}")
    }

    private fun processEcoBotQuery(query: String) {
        lifecycleScope.launch {
            try {
                showProgressDialog("EcoBot is thinking...")
                conversationHistory.add("User: $query")

                val response = aiService.chatWithEcoBot(query, conversationHistory)
                conversationHistory.add("EcoBot: $response")

                if (conversationHistory.size > 8) {
                    conversationHistory.subList(0, 2).clear()
                }

                hideProgressDialog()
                showResponseDialog("🤖 EcoBot Response", response)

                if (isVoiceModeEnabled && ::textToSpeech.isInitialized) {
                    textToSpeech.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            } catch (e: Exception) {
                hideProgressDialog()
                showErrorDialog("EcoBot Error", "Failed to get response from EcoBot. Please try again.")
            }
        }
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 100)
        }
    }

    private fun handleImageCapture(data: Intent?) {
        showToast("Image captured - Processing for classification...")
    }

    private fun handleSpeechResult(data: Intent?) {
        data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.let { results ->
            if (results.isNotEmpty()) {
                processEcoBotQuery(results[0])
            }
        }
    }

    private fun checkPermission(permission: String): Boolean {
        return if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 100)
            false
        } else {
            true
        }
    }

    private fun showInputDialog(title: String, hint: String, callback: (String) -> Unit) {
        val editText = TextInputEditText(this).apply { this.hint = hint }
        val inputLayout = TextInputLayout(this).apply {
            addView(editText)
            setPadding(64, 32, 64, 0)
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(inputLayout)
            .setPositiveButton("Submit") { _, _ ->
                val input = editText.text.toString().trim()
                if (input.isNotEmpty()) {
                    callback(input)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showResponseDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showErrorDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.getDefault()
        }
    }

    private fun showChallengeDetails(challengeName: String) {
        val details = """
            🏆 Recycle Challenge: Beat Heat!
            
            Goal: Collect and recycle materials to reduce environmental heat
            Duration: 1 week remaining
            Participants: 21 members
            Reward: 220 EcoPoints + Special Badge
        """.trimIndent()
        showResponseDialog("Challenge Details", details)
    }

    private fun showCO2Details() {
        showResponseDialog("CO² Savings", "Your CO² impact this month is 1,500 kg equivalent.")
    }

    private fun showTrashCycleDetails() {
        showResponseDialog("Trash Cycle Analytics", "You have processed 12 kg of waste this month.")
    }

    private fun showAllChallenges() {
        // startActivity(Intent(this, ChallengesActivity::class.java))
        showToast("Showing all challenges...")
    }

    private fun showAllActivity() {
        showToast("Showing all activity...")
    }

    private fun addEcoPoints(points: Int) {
        currentEcoPoints += points
        findViewById<TextView>(R.id.total_points_text)?.text = "${String.format("%,d", currentEcoPoints)} Points"
        showToast("✅ +$points EcoPoints earned!")
    }

    private var progressDialog: AlertDialog? = null

    private fun showProgressDialog(message: String) {
        progressDialog?.dismiss()
        progressDialog = AlertDialog.Builder(this)
            .setMessage(message)
            .setCancelable(false)
            .create()
        progressDialog?.show()
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    // FIX: Added the missing helper function.
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        if (::aiService.isInitialized) {
            aiService.cleanup()
        }
        progressDialog?.dismiss()
    }
}