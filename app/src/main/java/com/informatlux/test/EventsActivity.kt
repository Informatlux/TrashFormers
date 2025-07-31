package com.informatlux.test

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.informatlux.test.models.Event

class EventsActivity : AppCompatActivity() {

    private val viewModel: EventsViewModel by viewModels()
    private lateinit var eventsAdapter: EventsAdapter
    private val userId = "user1" // Replace with actual user ID logic

    private val createEventLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val newEvent = result.data?.getParcelableExtra<Event>("NEW_EVENT")
            newEvent?.let {
                viewModel.addEvent(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)

        val recyclerView = findViewById<RecyclerView>(R.id.events_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.eventsList.observe(this) { list ->
            eventsAdapter = EventsAdapter(list as List<Event>) { event -> joinEvent(event) }
            recyclerView.adapter = eventsAdapter
        }

        findViewById<ExtendedFloatingActionButton>(R.id.fab_create_event)
            .setOnClickListener {
                val intent = Intent(this, CreateEventActivity::class.java)
                createEventLauncher.launch(intent)
            }
    }

    private fun joinEvent(event: Event) {
        if (!event.participants.contains(userId)) {
            event.participants.add(userId) // Ensure 'participants' is mutable and correctly typed
            viewModel.updateEvent(event)
            ScoreManager.addPoints(userId, ScoreManager.POINTS_EVENT_PARTICIPATION)
            updateEcoPointsDisplay()
        }
    }

    private fun updateEcoPointsDisplay() {
        val points = ScoreManager.getScore(userId)
    }
}
