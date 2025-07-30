package com.informatlux.test

import androidx.annotation.DrawableRes

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val score: Int,
    @DrawableRes val avatarResId: Int // Using a drawable resource for sample avatars
)