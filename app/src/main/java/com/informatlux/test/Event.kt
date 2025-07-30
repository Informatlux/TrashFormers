package com.informatlux.test

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Event(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val date: String,
    val location: String,
    // FIX: Changed this from String to Uri? to correctly handle image paths
    val bannerUri: Uri?,
    var isOngoing: Boolean = false
) : Parcelable