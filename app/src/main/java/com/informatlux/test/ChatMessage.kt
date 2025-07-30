package com.informatlux.test

import android.net.Uri

sealed class ChatMessage {
    data class UserMessage(val text: String) : ChatMessage()
    data class BotMessage(val text: String, val isLoading: Boolean = false) : ChatMessage()
    data class ImagePreviewMessage(val uri: Uri) : ChatMessage()
}