package com.informatlux.test

object ScoreManager {
    // Scoring scheme
    const val POINTS_WASTE_CLASSIFICATION = 10
    const val POINTS_SEARCH_RECYCLING_CENTER = 10
    const val POINTS_DECOMPOSITION_QUERY = 10
    const val POINTS_AI_QUESTION = 1
    const val POINTS_DIY_SUGGESTION = 1
    const val CREATE_EVENT = 25
    // Add more actions as needed
    const val POINTS_EVENT_PARTICIPATION = 15
    const val POINTS_ARTICLE_READ = 5

    // In-memory user scores (replace with persistent storage in production)
    private val userScores = mutableMapOf<String, Int>()

    // Add points to a user
    fun addPoints(userId: String, points: Int) {
        userScores[userId] = (userScores[userId] ?: 0) + points
    }

    // Get a user's score
    fun getScore(userId: String): Int {
        return userScores[userId] ?: 0
    }

    // Set a user's score (for admin/testing)
    fun setScore(userId: String, score: Int) {
        userScores[userId] = score
    }

    // Get all users' scores for leaderboard
    fun getAllScores(): Map<String, Int> {
        return userScores.toMap()
    }

    // Reset all scores (for testing/demo)
    fun resetAllScores() {
        userScores.clear()
    }
} 