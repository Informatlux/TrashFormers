
package com.informatlux.test

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.informatlux.test.models.Event
import com.informatlux.test.services.SupaBaseServiceEvents
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EventDetailsActivity : AppCompatActivity() {

    private lateinit var supabaseService: SupaBaseServiceEvents
    private var currentEvent: Event? = null
    private val currentUserId = "current_user_id" // Replace with actual user ID from auth

    private lateinit var eventImage: ImageView
    private lateinit var eventTitle: TextView
    private lateinit var eventDescription: TextView
    private lateinit var eventPoints: TextView
    private lateinit var eventParticipants: TextView
    private lateinit var eventDate: TextView
    private lateinit var eventLocation: TextView
    private lateinit var joinButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_event_details)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        supabaseService = SupaBaseServiceEvents(this)
        initializeViews()
        setupClickListeners()

        val eventId = intent.getStringExtra("event_id")
        if (eventId != null) {
            loadEventDetails(eventId)
        } else {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeViews() {
        eventImage = findViewById(R.id.event_image)
        eventTitle = findViewById(R.id.event_title)
        eventDescription = findViewById(R.id.event_description)
        eventPoints = findViewById(R.id.event_points)
        eventParticipants = findViewById(R.id.event_participants)
        eventDate = findViewById(R.id.event_date)
        eventLocation = findViewById(R.id.event_location)
        joinButton = findViewById(R.id.btn_join_event)
    }

    private fun setupClickListeners() {
        findViewById<MaterialButton>(R.id.btn_back)?.setOnClickListener {
            finish()
        }

        joinButton.setOnClickListener {
            currentEvent?.let { event ->
                if (event.isJoined) {
                    leaveEvent(event.id)
                } else {
                    joinEvent(event.id)
                }
            }
        }
    }

    private fun loadEventDetails(eventId: String) {
        lifecycleScope.launch {
            supabaseService.getEventDetails(eventId, currentUserId).fold(
                onSuccess = { event ->
                    currentEvent = event
                    displayEventDetails(event)
                },
                onFailure = { error ->
                    Toast.makeText(this@EventDetailsActivity, "Failed to load event: ${error.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            )
        }
    }

    private fun displayEventDetails(event: Event) {
        eventTitle.text = event.title
        eventDescription.text = event.description
        eventPoints.text = "${event.pointsReward} Points"
        eventParticipants.text = "${event.participantCount} Participants"

        if (event.location.isNotEmpty()) {
            eventLocation.text = event.location
            eventLocation.visibility = android.view.View.VISIBLE
        } else {
            eventLocation.visibility = android.view.View.GONE
        }

        event.eventDate?.let { date ->
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            eventDate.text = dateFormat.format(date)
            eventDate.visibility = android.view.View.VISIBLE
        } ?: run {
            eventDate.visibility = android.view.View.GONE
        }

        updateJoinButton(event.isJoined)

        // Load image (you can implement image loading library like Glide or Picasso)
        // For now, set a placeholder or default image
        eventImage.setImageResource(R.drawable.green_background)
    }

    private fun updateJoinButton(isJoined: Boolean) {
        if (isJoined) {
            joinButton.text = "Leave Event"
            joinButton.setBackgroundColor(resources.getColor(R.color.apple_text_secondary, null))
        } else {
            joinButton.text = "Join Event"
            joinButton.setBackgroundColor(resources.getColor(R.color.sustainable_green, null))
        }
    }

    private fun joinEvent(eventId: String) {
        joinButton.isEnabled = false
        joinButton.text = "Joining..."

        lifecycleScope.launch {
            supabaseService.joinEvent(eventId, currentUserId).fold(
                onSuccess = { success ->
                    if (success) {
                        currentEvent = currentEvent?.copy(
                            isJoined = true,
                            participantCount = currentEvent!!.participantCount + 1
                        )
                        currentEvent?.let { displayEventDetails(it) }
                        Toast.makeText(this@EventDetailsActivity, "Successfully joined event!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@EventDetailsActivity, "Failed to join event", Toast.LENGTH_SHORT).show()
                    }
                    joinButton.isEnabled = true
                },
                onFailure = { error ->
                    Toast.makeText(this@EventDetailsActivity, "Error joining event: ${error.message}", Toast.LENGTH_LONG).show()
                    joinButton.isEnabled = true
                    joinButton.text = "Join Event"
                }
            )
        }
    }

    private fun leaveEvent(eventId: String) {
        joinButton.isEnabled = false
        joinButton.text = "Leaving..."

        lifecycleScope.launch {
            supabaseService.leaveEvent(eventId, currentUserId).fold(
                onSuccess = { success ->
                    if (success) {
                        currentEvent = currentEvent?.copy(
                            isJoined = false,
                            participantCount = maxOf(0, currentEvent!!.participantCount - 1)
                        )
                        currentEvent?.let { displayEventDetails(it) }
                        Toast.makeText(this@EventDetailsActivity, "Left event successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@EventDetailsActivity, "Failed to leave event", Toast.LENGTH_SHORT).show()
                    }
                    joinButton.isEnabled = true
                },
                onFailure = { error ->
                    Toast.makeText(this@EventDetailsActivity, "Error leaving event: ${error.message}", Toast.LENGTH_LONG).show()
                    joinButton.isEnabled = true
                    joinButton.text = "Leave Event"
                }
            )
        }
    }
}
