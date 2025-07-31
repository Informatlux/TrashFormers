package com.informatlux.test

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.util.*

class AIChatActivity : AppCompatActivity() {
    private val viewModel: AIChatViewModel by viewModels()
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var editTextMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnAttach: ImageButton
    private lateinit var btnMic: ImageButton
    private lateinit var toolbarTitle: TextView
    private var tempImageUri: Uri? = null
    private var currentSessionId: String? = null

    // TTS (Speech Recognition) variables
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempImageUri != null) {
            viewModel.analyzeImage(tempImageUri!!, this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aichat)

        // Initialize UserManager
        UserManager.initialize(this)

        initializeViews()
        setupRecyclerView()
        setupSpeechRecognizer()
        handleSessionIntent()
        setupClickListeners()
        observeViewModel()
    }

    private fun initializeViews() {
        chatRecyclerView = findViewById(R.id.chat_recycler_view)
        editTextMessage = findViewById(R.id.edit_text_message)
        btnSend = findViewById(R.id.btn_send)
        btnAttach = findViewById(R.id.btn_attach)
        btnMic = findViewById(R.id.btn_mic)
        toolbarTitle = findViewById(R.id.toolbar_title)
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(mutableListOf())
        chatRecyclerView.adapter = chatAdapter
        chatRecyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
    }

    private fun setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    btnMic.setImageResource(R.drawable.mic_icon) // You'll need this drawable
                    Toast.makeText(this@AIChatActivity, "Listening...", Toast.LENGTH_SHORT).show()
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    btnMic.setImageResource(R.drawable.mic_icon)
                    isListening = false
                }

                override fun onError(error: Int) {
                    btnMic.setImageResource(R.drawable.mic_icon)
                    isListening = false
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech input matched"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Speech recognition error"
                    }
                    Toast.makeText(this@AIChatActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }

                override fun onResults(results: Bundle?) {
                    btnMic.setImageResource(R.drawable.mic_icon)
                    isListening = false

                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val spokenText = matches[0]
                        editTextMessage.setText(spokenText)
                        editTextMessage.setSelection(spokenText.length) // Move cursor to end
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun handleSessionIntent() {
        val sessionId = intent.getStringExtra("session_id")
        currentSessionId = sessionId

        if (sessionId != null) {
            viewModel.loadSession(sessionId)
            toolbarTitle.text = "Loading..."
        } else {
            viewModel.createNewSession()
            toolbarTitle.text = "New Chat"
        }
    }

    private fun setupClickListeners() {
        btnSend.setOnClickListener { sendMessage() }
        btnAttach.setOnClickListener { checkCameraPermissionAndLaunch() }
        btnMic.setOnClickListener { toggleSpeechRecognition() }

        val btnback = findViewById<ImageButton>(R.id.btn_back)
        btnback.setOnClickListener {
            val intent = Intent(this, AIActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        // Observe messages from ViewModel
        viewModel.messages.observe(this) { messages ->
            chatAdapter.updateMessages(messages)
            chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)

            // Generate title after first user message
            if (messages.isNotEmpty() && toolbarTitle.text == "New Chat") {
                generateChatTitle(messages)
            }
        }

        // Observe session title updates
        viewModel.sessionTitle.observe(this) { title ->
            toolbarTitle.text = title
        }
    }

    private fun sendMessage() {
        val userText = editTextMessage.text.toString().trim()
        if (userText.isNotEmpty()) {
            viewModel.sendMessage(userText)
            editTextMessage.text.clear()
        }
    }

    private fun toggleSpeechRecognition() {
        if (isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    private fun startListening() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                isListening = true
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                }
                speechRecognizer?.startListening(intent)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                Toast.makeText(this, "Microphone permission is required for speech recognition.", Toast.LENGTH_SHORT).show()
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 101)
            }
            else -> {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 101)
            }
        }
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        btnMic.setImageResource(R.drawable.mic_icon)
    }

    private fun generateChatTitle(messages: List<ChatMessage>) {
        val firstUserMessage = messages.firstOrNull { it is ChatMessage.UserMessage }
        if (firstUserMessage is ChatMessage.UserMessage) {
            viewModel.generateSessionTitle(firstUserMessage.text)
        }
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(this, "Camera permission is required to take photos.", Toast.LENGTH_SHORT).show()
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 100)
            }
            else -> {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 100)
            }
        }
    }

    private fun launchCamera() {
        try {
            val imageFile = File.createTempFile("picture_", ".jpg", cacheDir).apply { deleteOnExit() }
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", imageFile)
            tempImageUri = uri
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to launch camera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            100 -> { // Camera permission
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchCamera()
                }
            }
            101 -> { // Microphone permission
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startListening()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        // Save current session
        viewModel.saveCurrentSession()
    }

    override fun onPause() {
        super.onPause()
        if (isListening) {
            stopListening()
        }
        // Save current session when pausing
        viewModel.saveCurrentSession()
    }

    override fun onStop() {
        super.onStop()
        // Save current session when stopping
        viewModel.saveCurrentSession()
    }
}