package com.informatlux.test

import androidx.annotation.DrawableRes

// A unique key for each menu item to identify it in click listeners
enum class MenuItemKey {
    SETTINGS, LEADERBOARD, EVENTS, ABOUT, LOGOUT
}

// The data class representing a single row in our menu
data class MoreMenuItem(
    val key: MenuItemKey,
    @DrawableRes val iconResId: Int,
    val title: String,
    val subtitle: String
)