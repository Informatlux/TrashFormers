
package com.informatlux.test.services

import android.content.Context
import android.util.Log
import com.informatlux.test.models.Event
import com.informatlux.test.models.EventParticipant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class SupaBaseServiceEvents(private val context: Context) {

    // Replace with your actual Supabase URL and API Key
    companion object{
        private const val supabaseUrl = "https://jedpwwxjrsejumyqyrgx.supabase.co"
        private const val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImplZHB3d3hqcnNlanVteXF5cmd4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTM4NzYzMzQsImV4cCI6MjA2OTQ1MjMzNH0.x9iFEmjd8ldd_llmc70ZfVqV3BBsUx1MSLnZbFCPlxI"
    }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

    suspend fun createEvent(event: Event): Result<Event> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$supabaseUrl/rest/v1/events")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("apikey", supabaseKey)
            connection.setRequestProperty("Authorization", "Bearer $supabaseKey")
            connection.doOutput = true

            val eventJson = JSONObject().apply {
                put("title", event.title)
                put("description", event.description)
                put("image_url", event.imageUrl)
                put("created_by", event.createdBy)
                event.eventDate?.let { put("event_date", dateFormat.format(it)) }
                put("location", event.location)
                put("points_reward", event.pointsReward)
                put("max_participants", event.maxParticipants)
                put("category", event.category)
            }

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(eventJson.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_CREATED) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).readText()
                val responseArray = JSONArray(response)
                if (responseArray.length() > 0) {
                    val createdEvent = parseEventFromJson(responseArray.getJSONObject(0))
                    Result.success(createdEvent)
                } else {
                    Result.failure(Exception("No event created"))
                }
            } else {
                val error = BufferedReader(InputStreamReader(connection.errorStream)).readText()
                Result.failure(Exception("Failed to create event: $error"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error creating event", e)
            Result.failure(e)
        }
    }

    suspend fun getEvents(): Result<List<Event>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$supabaseUrl/rest/v1/events?select=*,participant_count:event_participants(count)&order=created_at.desc")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.setRequestProperty("apikey", supabaseKey)
            connection.setRequestProperty("Authorization", "Bearer $supabaseKey")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).readText()
                val eventsArray = JSONArray(response)
                val events = mutableListOf<Event>()

                for (i in 0 until eventsArray.length()) {
                    val eventJson = eventsArray.getJSONObject(i)
                    events.add(parseEventFromJson(eventJson))
                }

                Result.success(events)
            } else {
                val error = BufferedReader(InputStreamReader(connection.errorStream)).readText()
                Result.failure(Exception("Failed to fetch events: $error"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error fetching events", e)
            Result.failure(e)
        }
    }

    suspend fun joinEvent(eventId: String, userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$supabaseUrl/rest/v1/event_participants")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("apikey", supabaseKey)
            connection.setRequestProperty("Authorization", "Bearer $supabaseKey")
            connection.doOutput = true

            val participantJson = JSONObject().apply {
                put("event_id", eventId)
                put("user_id", userId)
            }

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(participantJson.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            Result.success(responseCode == HttpURLConnection.HTTP_CREATED)
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error joining event", e)
            Result.failure(e)
        }
    }

    suspend fun leaveEvent(eventId: String, userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$supabaseUrl/rest/v1/event_participants?event_id=eq.$eventId&user_id=eq.$userId")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "DELETE"
            connection.setRequestProperty("apikey", supabaseKey)
            connection.setRequestProperty("Authorization", "Bearer $supabaseKey")

            val responseCode = connection.responseCode
            Result.success(responseCode == HttpURLConnection.HTTP_NO_CONTENT)
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error leaving event", e)
            Result.failure(e)
        }
    }

    suspend fun getEventDetails(eventId: String, userId: String): Result<Event> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$supabaseUrl/rest/v1/events?id=eq.$eventId&select=*,participant_count:event_participants(count),is_joined:event_participants(user_id.eq.$userId)")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.setRequestProperty("apikey", supabaseKey)
            connection.setRequestProperty("Authorization", "Bearer $supabaseKey")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).readText()
                val eventsArray = JSONArray(response)

                if (eventsArray.length() > 0) {
                    val eventJson = eventsArray.getJSONObject(0)
                    val event = parseEventFromJson(eventJson)
                    Result.success(event)
                } else {
                    Result.failure(Exception("Event not found"))
                }
            } else {
                val error = BufferedReader(InputStreamReader(connection.errorStream)).readText()
                Result.failure(Exception("Failed to fetch event details: $error"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error fetching event details", e)
            Result.failure(e)
        }
    }

    private fun parseEventFromJson(json: JSONObject): Event {
        return Event(
            id = json.optString("id", ""),
            title = json.optString("title", ""),
            description = json.optString("description", ""),
            imageUrl = json.optString("image_url", ""),
            createdBy = json.optString("created_by", ""),
            createdAt = try {
                dateFormat.parse(json.optString("created_at", "")) ?: Date()
            } catch (e: Exception) { Date() },
            eventDate = try {
                json.optString("event_date").takeIf { it.isNotEmpty() }?.let {
                    dateFormat.parse(it)
                }
            } catch (e: Exception) { null },
            location = json.optString("location", ""),
            pointsReward = json.optInt("points_reward", 0),
            maxParticipants = json.optInt("max_participants", 0),
            status = json.optString("status", "active"),
            category = json.optString("category", "challenge"),
            participantCount = json.optJSONArray("participant_count")?.length() ?: 0,
            isJoined = json.optJSONArray("is_joined")?.length() ?: 0 > 0
        )
    }
}
