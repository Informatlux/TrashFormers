package com.informatlux.test

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class AIActivity : AppCompatActivity() {

    private val viewModel: AIViewModel by viewModels()
    private lateinit var chatHistoryRecyclerView: RecyclerView
    private lateinit var chatHistoryAdapter: ChatHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ai_home)

        // Initialize UserManager
        UserManager.initialize(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        setupChatHistory()
        setupNewChatButton()
        setupBottomNavigation()
    }

    private fun setupChatHistory() {
        // Use a different ID that exists in your layout
        chatHistoryRecyclerView = findViewById(R.id.history_recycler_view) // Change this to match your layout
        chatHistoryRecyclerView.layoutManager = LinearLayoutManager(this)
        chatHistoryAdapter = ChatHistoryAdapter(
            emptyList(),
            onSessionClick = { sessionId ->
                // Open chat session
                val intent = Intent(this, AIChatActivity::class.java)
                intent.putExtra("session_id", sessionId)
                startActivity(intent)
            },
            onSessionLongClick = { sessionId ->
                // Delete session with confirmation
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Chat")
                    .setMessage("Are you sure you want to delete this chat session?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteSession(sessionId)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        chatHistoryRecyclerView.adapter = chatHistoryAdapter

        // Observe chat sessions from ViewModel and convert to ChatHistorySession
        viewModel.chatSessions.observe(this) { sessions ->
            val historySession = sessions.map { chatSession ->
                ChatHistorySession(
                    id = chatSession.id,
                    title = chatSession.title,
                    createdAt = chatSession.created_at ?: "",
                    messageCount = 0 // You'll need to implement message counting logic
                )
            }
            chatHistoryAdapter.updateSessions(historySession)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh chat sessions when returning to this activity
        viewModel.loadChatSessions()
    }

    private fun setupNewChatButton() {
        val btnNewChat = findViewById<Button>(R.id.btn_new_chat)
        btnNewChat.setOnClickListener {
            val intent = Intent(this, AIChatActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupBottomNavigation() {
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
                R.id.nav_ai -> true // Current screen
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
}