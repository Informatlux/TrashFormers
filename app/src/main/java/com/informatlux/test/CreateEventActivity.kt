package com.informatlux.test

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.informatlux.test.models.Event
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

class CreateEventActivity : AppCompatActivity() {

    private val SUPABASE_URL     = "https://jedpwwxjrsejumyqyrgx.supabase.co"
    private val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImplZHB3d3hqcnNlanVteXF5cmd4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTM4NzYzMzQsImV4cCI6MjA2OTQ1MjMzNH0.x9iFEmjd8ldd_llmc70ZfVqV3BBsUx1MSLnZbFCPlxI"
    private val TAG = "CreateEventAct"

    private lateinit var supabase: io.github.jan.supabase.SupabaseClient

    private lateinit var titleInput: EditText
    private lateinit var descInput: EditText
    private lateinit var dateInput: EditText
    private lateinit var locationInput: EditText
    private lateinit var pointsInput: EditText
    private lateinit var maxPartInput: EditText
    private lateinit var createBtn: MaterialButton

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var selectedDate: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)

        // 1. init supabase
        supabase = createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
            install(Auth)      { alwaysAutoRefresh = false; autoLoadFromStorage = false }
            install(Postgrest) { defaultSchema = "public" }
        }

        // 2. bind views
        titleInput    = findViewById(R.id.edit_event_title)
        descInput     = findViewById(R.id.edit_event_description)
        dateInput     = findViewById(R.id.btn_select_date)
        locationInput = findViewById(R.id.edit_event_location)
        pointsInput   = findViewById(R.id.edit_event_points)
        maxPartInput  = findViewById(R.id.edit_max_participants)
        createBtn     = findViewById<MaterialButton>(R.id.btn_create_event)

        // 3. date picker
        dateInput.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    cal.set(y, m, d, 0, 0, 0)
                    selectedDate = cal.time
                    dateInput.setText(dateFormat.format(cal.time))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // 4. submit
        createBtn.setOnClickListener { submitForm() }
    }

    private fun submitForm() {
        val title    = titleInput.text.toString().trim()
        val desc     = descInput.text.toString().trim()
        val loc      = locationInput.text.toString().trim()
        val ptsText  = pointsInput.text.toString().trim()
        val maxText  = maxPartInput.text.toString().trim()

        if (title.isEmpty() || desc.isEmpty() || loc.isEmpty() ||
            ptsText.isEmpty() || maxText.isEmpty() || selectedDate == null
        ) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val pts  = ptsText.toIntOrNull()
        val maxP = maxText.toIntOrNull()
        if (pts == null || maxP == null) {
            Toast.makeText(this, "Points & Participants must be numbers", Toast.LENGTH_SHORT).show()
            return
        }

        // 5. do work in coroutine
        lifecycleScope.launch {
            try {
                // a) get userId inside coroutine
                val userId = UserManager.getCurrentUserId()

                // b) build Event
                val event = Event(
                    id              = UUID.randomUUID().toString(),
                    title           = title,
                    description     = desc,
                    imageUrl        = "",
                    createdBy       = userId,
                    createdAt       = Date(),
                    eventDate       = selectedDate,
                    location        = loc,
                    pointsReward    = pts,
                    maxParticipants = maxP,
                    participants    = mutableListOf()
                )

                // c) insert; insert() is a suspend that runs the request
                supabase.from("events").insert(listOf(event))

                // d) return to caller
                val intent = Intent().putExtra("NEW_EVENT", event)
                setResult(RESULT_OK, intent)
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Create event failed", e)
                Toast.makeText(
                    this@CreateEventActivity,
                    "Failed to create event: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
