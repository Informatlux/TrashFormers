package com.informatlux.test

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.informatlux.test.models.Event

class EventsViewModel : ViewModel() {

    // This LiveData will hold the final list sent to the adapter (including String headers).
    private val _eventsList = MutableLiveData<List<Any>>()
    val eventsList: LiveData<List<Any>> = _eventsList

    // This is the "source of truth" - a simple list holding only the raw Event objects.
    private val allEvents = mutableListOf<Event>()

    init {
        // When the ViewModel is first created, load the initial (empty) state.
        loadEvents()
    }

    private val store = mutableListOf<Event>()

    fun addEvent(e: Event) {
        store += e
        _eventsList.value = store.toList()
    }

    fun updateEvent(updated: Event) {
        val idx = store.indexOfFirst { it.id == updated.id }
        if (idx >= 0) {
            store[idx] = updated
            _eventsList.value = store.toList()
        }
    }

    private fun loadEvents() {

        updateGroupedList()
    }


    private fun updateGroupedList() {
        // Create a new list that can hold both Strings (headers) and Event objects.
        val groupedList = mutableListOf<Any>()

        // Filter the raw events into two separate lists.
        val ongoing = allEvents.filter { it.isOngoing }
        val upcoming = allEvents.filter { !it.isOngoing }

        // If there are any ongoing events, add the header and then the events.
        if (ongoing.isNotEmpty()) {
            groupedList.add("Ongoing Events")
            groupedList.addAll(ongoing)
        }

        // If there are any upcoming events, add the header and then the events.
        if (upcoming.isNotEmpty()) {
            groupedList.add("Upcoming Events")
            groupedList.addAll(upcoming)
        }

        _eventsList.value = groupedList
    }
}