package com.informatlux.test

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AIViewModel : ViewModel() {

    // LiveData for the carousel items
    private val _carouselItems = MutableLiveData<List<AiCarouselItem>>()
    val carouselItems: LiveData<List<AiCarouselItem>> = _carouselItems

    // LiveData for the history items
    private val _historyItems = MutableLiveData<List<AiHistoryItem>>()
    val historyItems: LiveData<List<AiHistoryItem>> = _historyItems

    // LiveData to control the visibility of the "No history" text
    private val _isHistoryEmpty = MutableLiveData<Boolean>()
    val isHistoryEmpty: LiveData<Boolean> = _isHistoryEmpty

    init {
        loadAiFeatures()
        loadChatHistory()
    }

    private fun loadAiFeatures() {
        // In a real app, this data would come from a remote config or a database
        val features = listOf(
            AiCarouselItem(R.drawable.help_icon, "AI Helper", "General AI assistant for eco tips and questions.") ,
            AiCarouselItem(R.drawable.scan_icon, "Scan Images", "Capture images to classify waste automatically."),
            AiCarouselItem(R.drawable.waste_separation_icon, "Classify Waste Items", "AI-powered classification for better sorting."),
            AiCarouselItem(R.drawable.voice_mode_icon, "Voice Mode", "Interact with the AI assistant using voice commands."),
            AiCarouselItem(R.drawable.decompostion_time, "Decomposition Time", "Estimate how long waste takes to decompose."),
            AiCarouselItem(R.drawable.recycle_icon, "Smart Recycling Suggestions", "Recommendation of recycling centers and methods."),
            AiCarouselItem(R.drawable.trash_icon, "Waste Sorting Assistance", "Help sorting waste into correct bins."),
            AiCarouselItem(R.drawable.electronics_icon, "E-Waste Identification", "Recognize electronic waste & safe disposal tips."),
            AiCarouselItem(R.drawable.eco_points_icon, "Eco Points & Gamification", "Earn points and rewards for eco-friendly actions."),
            AiCarouselItem(R.drawable.info_icon, "AI Help & FAQs", "Chat with AI assistant for instant help.")

        )
        _carouselItems.value = features
    }

    private fun loadChatHistory() {
        // In a real app, you would fetch this from a local database (like Room)
        val history = listOf(
            AiHistoryItem(R.drawable.ai_chatbot, "Code tutor", "How to use Visual Studio", R.drawable.ai_history_bg),
        )
        _historyItems.value = history
        _isHistoryEmpty.value = history.isEmpty()
    }
}