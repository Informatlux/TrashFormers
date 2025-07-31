package com.informatlux.test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

data class ChatHistorySession(
    val id: String,
    val title: String,
    val createdAt: String,
    val messageCount: Int
)

class ChatHistoryAdapter(
    private var sessions: List<ChatHistorySession>,
    private val onSessionClick: (String) -> Unit,
    private val onSessionLongClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<ChatHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.chat_session_title)
        val date: TextView = view.findViewById(R.id.chat_session_date)
        val messageCount: TextView = view.findViewById(R.id.chat_session_message_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = sessions[position]
        holder.title.text = session.title
        holder.date.text = formatDate(session.createdAt)
        holder.messageCount.text = "${session.messageCount} messages"

        holder.itemView.setOnClickListener {
            onSessionClick(session.id)
        }

        holder.itemView.setOnLongClickListener {
            onSessionLongClick?.invoke(session.id)
            true
        }
    }

    override fun getItemCount() = sessions.size

    fun updateSessions(newSessions: List<ChatHistorySession>) {
        sessions = newSessions
        notifyDataSetChanged()
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }
}