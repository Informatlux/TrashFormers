
package com.informatlux.test.models

import java.util.Date

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val createdBy: String = "",
    val createdAt: Date = Date(),
    val eventDate: Date? = null,
    val location: String = "",
    val pointsReward: Int = 0,
    val maxParticipants: Int = 0,
    val status: String = "active",
    val category: String = "challenge",
    val participantCount: Int = 0,
    val isJoined: Boolean = false
)

data class EventParticipant(
    val id: String = "",
    val eventId: String = "",
    val userId: String = "",
    val joinedAt: Date = Date()
)
