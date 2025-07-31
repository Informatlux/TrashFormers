
package com.informatlux.test

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.informatlux.test.models.Event
import com.informatlux.test.services.SupaBaseServiceEvents
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CreateEventActivity : AppCompatActivity() {

    private lateinit var supabaseService: SupaBaseServiceEvents
    private var selectedDate: Calendar? = null

    private lateinit var titleEditText: TextInputEditText
    private lateinit var descriptionEditText: TextInputEditText
    private lateinit var locationEditText: TextInputEditText
    private lateinit var pointsEditText: TextInputEditText
    private lateinit var maxParticipantsEditText: TextInputEditText
    private lateinit var imageUrlEditText: TextInputEditText
    private lateinit var dateButton: MaterialButton
    private lateinit var createButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_event)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        supabaseService = SupaBaseServiceEvents(this)
        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        titleEditText = findViewById(R.id.edit_event_title)
        descriptionEditText = findViewById(R.id.edit_event_description)
        locationEditText = findViewById(R.id.edit_event_location)
        pointsEditText = findViewById(R.id.edit_event_points)
        maxParticipantsEditText = findViewById(R.id.edit_max_participants)
        imageUrlEditText = findViewById(R.id.edit_image_url)
        dateButton = findViewById(R.id.btn_select_date)
        createButton = findViewById(R.id.btn_create_event)
    }

    private fun setupClickListeners() {
        dateButton.setOnClickListener {
            showDateTimePicker()
        }

        createButton.setOnClickListener {
            createEvent()
        }

        findViewById<MaterialButton>(R.id.btn_back)?.setOnClickListener {
            finish()
        }
    }

    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            TimePickerDialog(this, { _, hourOfDay, minute ->
                selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, hourOfDay, minute)
                }
                updateDateButton()
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateDateButton() {
        selectedDate?.let { date ->
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            dateButton.text = dateFormat.format(date.time)
        }
    }

    private fun createEvent() {
        val title = titleEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val location = locationEditText.text.toString().trim()
        val pointsText = pointsEditText.text.toString().trim()
        val maxParticipantsText = maxParticipantsEditText.text.toString().trim()
        val imageUrl = imageUrlEditText.text.toString().trim()

        if (title.isEmpty()) {
            titleEditText.error = "Title is required"
            return
        }

        if (description.isEmpty()) {
            descriptionEditText.error = "Description is required"
            return
        }

        val points = pointsText.toIntOrNull() ?: 0
        val maxParticipants = maxParticipantsText.toIntOrNull() ?: 0

        val event = Event(
            title = title,
            description = description,
            location = location,
            eventDate = selectedDate?.time,
            pointsReward = points,
            maxParticipants = maxParticipants,
            imageUrl = imageUrl.ifEmpty { "https://images.unsplash.com/photo-1542601906990-b4d3fb778b09?w=800" },
            createdBy = "current_user_id", // Replace with actual user ID from auth
            category = "challenge"
        )

        createButton.isEnabled = false
        createButton.text = "Creating..."

        lifecycleScope.launch {
            supabaseService.createEvent(event).fold(
                onSuccess = { createdEvent ->
                    Toast.makeText(this@CreateEventActivity, "Event created successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                },
                onFailure = { error ->
                    Toast.makeText(this@CreateEventActivity, "Failed to create event: ${error.message}", Toast.LENGTH_LONG).show()
                    createButton.isEnabled = true
                    createButton.text = "Create Event"
                }
            )
        }
    }
}
