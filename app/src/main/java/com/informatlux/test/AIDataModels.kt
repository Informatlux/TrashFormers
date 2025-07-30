package com.informatlux.test

import androidx.annotation.DrawableRes

// Represents a single card in the top feature carousel
data class AiCarouselItem(
    @DrawableRes val iconResId: Int,
    val title: String,
    val description: String
)

// Represents a single row in the chat history list
data class AiHistoryItem(
    @DrawableRes val iconResId: Int,
    val title: String,
    val subtitle: String,
    val iconBackgroundResId: Int // To allow for different background colors
)