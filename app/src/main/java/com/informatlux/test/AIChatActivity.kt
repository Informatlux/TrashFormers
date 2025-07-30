package com.informatlux.test

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class AIChatActivity : AppCompatActivity() {
    private val viewModel: AIChatViewModel by viewModels()
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var editTextMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnAttach: ImageButton
    private var tempImageUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempImageUri != null) {
            viewModel.analyzeImage(tempImageUri!!, this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aichat)

        chatRecyclerView = findViewById(R.id.chat_recycler_view)
        chatAdapter = ChatAdapter(mutableListOf())
        chatRecyclerView.adapter = chatAdapter
        chatRecyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }

        editTextMessage = findViewById(R.id.edit_text_message)
        btnSend = findViewById(R.id.btn_send)
        btnAttach = findViewById(R.id.btn_attach)

        btnSend.setOnClickListener { sendMessage() }
        btnAttach.setOnClickListener { checkCameraPermissionAndLaunch() }

        viewModel.messages.observe(this) { messages ->
            chatAdapter.updateMessages(messages)
            chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    private fun sendMessage() {
        val userText = editTextMessage.text.toString().trim()
        if (userText.isNotEmpty()) {
            viewModel.sendMessage(userText)
            editTextMessage.text.clear()
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
        // Create a temporary image file in the cache directory
        val imageFile = File.createTempFile("picture_", ".jpg", cacheDir).apply {
            deleteOnExit() // Mark file for deletion on app exit to avoid clutter
        }

        // Get a content URI for the File using FileProvider
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", imageFile)

        // Assign to the nullable tempImageUri variable
        tempImageUri = uri

        // Launch camera if URI is not null (it should never be, but safe call for Kotlin null safety)
        tempImageUri?.let { takePictureLauncher.launch(it) }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        }
    }
}