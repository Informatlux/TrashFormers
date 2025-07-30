package com.informatlux.test

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LeaderboardViewModel : ViewModel() {

    private val _leaderboardEntries = MutableLiveData<List<LeaderboardEntry>>()
    val leaderboardEntries: LiveData<List<LeaderboardEntry>> = _leaderboardEntries

    init {
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        // In a real app, this data would come from your server or a local database
        val sampleData = listOf(
            LeaderboardEntry(1, "Devrizal Maryuandi", 12341, R.drawable.profile_placeholder_icon),
            LeaderboardEntry(2, "Mika Alulba", 11982, R.drawable.profile_placeholder_icon),
            LeaderboardEntry(3, "Alejandro Luis", 10543, R.drawable.profile_placeholder_icon),
            LeaderboardEntry(4, "Jane Doe", 9876, R.drawable.profile_placeholder_icon),
            LeaderboardEntry(5, "John Smith", 8765, R.drawable.profile_placeholder_icon),
            LeaderboardEntry(6, "Emily Jones", 7654, R.drawable.profile_placeholder_icon),
            LeaderboardEntry(7, "Michael Clark", 6543, R.drawable.profile_placeholder_icon),
            LeaderboardEntry(8, "Sarah Wilson", 5432, R.drawable.profile_placeholder_icon)
        )
        _leaderboardEntries.value = sampleData
    }
}