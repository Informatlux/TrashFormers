package com.informatlux.test

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class AIChatViewModel(application: Application) : AndroidViewModel(application) {
    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val messageList = mutableListOf<ChatMessage>()
    private val userId = "user1" // Replace with actual user ID logic

    fun sendMessage(userText: String) {
        addMessage(ChatMessage.UserMessage(userText))
        addMessage(ChatMessage.BotMessage("", isLoading = true))

        viewModelScope.launch {
            // Award points for asking AI
            ScoreManager.addPoints(userId, ScoreManager.POINTS_AI_QUESTION)

            // Detect intent for scoring
            if (userText.contains("decompose", true) || userText.contains("decomposition", true)) {
                ScoreManager.addPoints(userId, ScoreManager.POINTS_DECOMPOSITION_QUERY)
            }
            if (userText.contains("recycle", true) || userText.contains("recycling center", true)) {
                ScoreManager.addPoints(userId, ScoreManager.POINTS_SEARCH_RECYCLING_CENTER)
            }
            if (userText.contains("DIY", true) || userText.contains("best out of waste", true)) {
                ScoreManager.addPoints(userId, ScoreManager.POINTS_DIY_SUGGESTION)
            }
            if (userText.contains("classify", true) || userText.contains("waste type", true)) {
                ScoreManager.addPoints(userId, ScoreManager.POINTS_WASTE_CLASSIFICATION)
            }

            val response = DeepSeekService.askDeepSeek(userText)
            updateLastBotMessage(response)
        }
    }

    fun analyzeImage(imageUri: Uri, context: android.content.Context) {
        addMessage(ChatMessage.ImagePreviewMessage(imageUri))
        addMessage(ChatMessage.BotMessage("", isLoading = true))

        viewModelScope.launch {
            val bitmap = ImageUtils.loadBitmapFromUri(context, imageUri)
            if (bitmap != null) {
                ScoreManager.addPoints(userId, ScoreManager.POINTS_WASTE_CLASSIFICATION)
                val prompt = "Identify the type of waste in this image, estimate decomposition time, suggest a DIY reuse, and provide a caption for proper disposal."
                val response = DeepSeekService.askDeepSeek(prompt, bitmap)
                updateLastBotMessage(response)
            } else {
                updateLastBotMessage("Could not load image.")
            }
        }
    }

    private fun addMessage(msg: ChatMessage) {
        messageList.add(msg)
        _messages.value = messageList.toList()
    }

    private fun updateLastBotMessage(text: String) {
        val lastIndex = messageList.indexOfLast { it is ChatMessage.BotMessage && it.isLoading }
        if (lastIndex != -1) {
            val oldMsg = messageList[lastIndex] as ChatMessage.BotMessage
            messageList[lastIndex] = oldMsg.copy(text = text, isLoading = false)
            _messages.value = messageList.toList()
        }
    }
}