package com.informatlux.test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(
    private val items: List<AiHistoryItem>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.history_item_icon)
        private val titleView: TextView = itemView.findViewById(R.id.history_item_title)
        private val subtitleView: TextView = itemView.findViewById(R.id.history_item_subtitle)

        fun bind(item: AiHistoryItem) {
            iconView.setImageResource(item.iconResId)
            iconView.setBackgroundResource(item.iconBackgroundResId)
            titleView.text = item.title
            subtitleView.text = item.subtitle
            // Here you would set a click listener to open this specific chat history
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}