package com.informatlux.test

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar

class CreateEventActivity : AppCompatActivity() {

    private var selectedBannerUri: Uri? = null
    private lateinit var imageBannerPreview: ShapeableImageView
    private lateinit var inputDate: TextInputEditText

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedBannerUri = it
            imageBannerPreview.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)

        // Initialize all views
        imageBannerPreview = findViewById(R.id.image_banner_preview)
        inputDate = findViewById(R.id.input_event_date)
        val inputTitle = findViewById<TextInputEditText>(R.id.input_event_title)
        val inputDesc = findViewById<TextInputEditText>(R.id.input_event_description)
        val inputLocation = findViewById<TextInputEditText>(R.id.input_event_location)

        // Setup Toolbar
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        // Setup Click Listeners
        imageBannerPreview.setOnClickListener {
            // Launch the image picker to get a banner
            pickImageLauncher.launch("image/*")
        }

        inputDate.setOnClickListener {
            // Show a calendar to pick a date
            showDatePickerDialog()
        }

        findViewById<MaterialButton>(R.id.btn_create_event).setOnClickListener {
            val title = inputTitle.text.toString().trim()
            val desc = inputDesc.text.toString().trim()
            val date = inputDate.text.toString().trim()
            val location = inputLocation.text.toString().trim()

            // Validate all input fields, including the banner image
            if (title.isBlank() || desc.isBlank() || date.isBlank() || location.isBlank() || selectedBannerUri == null) {
                Toast.makeText(this, "Please fill all fields and select a banner", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // FIX: Create the Event object using named arguments to match the data class.
            // The 'id' and 'isOngoing' fields have default values in the data class,
            // so we don't need to provide them here unless we want to override them.
            val newEvent = Event(
                title = title,
                description = desc,
                date = date,
                location = location,
                bannerUri = selectedBannerUri
            )

            // Create an intent to pass the new event data back to EventsActivity
            val resultIntent = Intent()
            resultIntent.putExtra("NEW_EVENT", newEvent)
            setResult(Activity.RESULT_OK, resultIntent)
            finish() // Close this activity and return to the list
        }
    }
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            inputDate.setText("$selectedDay/${selectedMonth + 1}/$selectedYear")
        }, year, month, day).show()
    }
}