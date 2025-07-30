package com.informatlux.test

import com.google.mlkit.nl.smartreply.SmartReply
import com.google.mlkit.nl.smartreply.TextMessage
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult
import kotlinx.coroutines.tasks.await

object MLKitSmartReplyService {
    suspend fun getSmartReplies(messages: List<TextMessage>): List<String> {
        val smartReply = SmartReply.getClient()
        val result = smartReply.suggestReplies(messages).await()
        return when (result.status) {
            SmartReplySuggestionResult.STATUS_SUCCESS -> result.suggestions.map { it.text }
            else -> listOf("No smart reply available.")
        }
    }
}
