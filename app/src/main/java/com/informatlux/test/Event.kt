package com.informatlux.test.models

import android.os.Parcel
import android.os.Parcelable
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
    val participants: MutableList<String> = mutableListOf(),
    val isJoined: Boolean = false
) : Parcelable {
    // computed property
    val isOngoing: Boolean
        get() = eventDate?.let { it.before(Date()) } ?: false

    private constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        Date(parcel.readLong()),
        parcel.readLong().let { if (it > 0L) Date(it) else null },
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: "active",
        parcel.readString() ?: "challenge",
        parcel.readInt(),
        mutableListOf<String>().apply { parcel.readStringList(this) },
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(imageUrl)
        parcel.writeString(createdBy)
        parcel.writeLong(createdAt.time)
        parcel.writeLong(eventDate?.time ?: -1L)
        parcel.writeString(location)
        parcel.writeInt(pointsReward)
        parcel.writeInt(maxParticipants)
        parcel.writeString(status)
        parcel.writeString(category)
        parcel.writeInt(participantCount)
        parcel.writeStringList(participants)
        parcel.writeByte(if (isJoined) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Event> {
        override fun createFromParcel(parcel: Parcel): Event = Event(parcel)
        override fun newArray(size: Int): Array<Event?> = arrayOfNulls(size)
    }
}
