package com.informatlux.test

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LeaderboardAdapter(
    private val entries: List<LeaderboardEntry>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_TOP_3 = 1
        private const val VIEW_TYPE_REGULAR = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (entries[position].rank <= 3) VIEW_TYPE_TOP_3 else VIEW_TYPE_REGULAR
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_TOP_3) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_leaderboard_top3, parent, false)
            Top3ViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_leaderboard_regular, parent, false)
            RegularViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val entry = entries[position]
        if (holder is Top3ViewHolder) {
            holder.bind(entry)
        } else if (holder is RegularViewHolder) {
            holder.bind(entry)
        }
    }

    override fun getItemCount(): Int = entries.size

    // ViewHolder for Ranks 1-3
    class Top3ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rankText: TextView = itemView.findViewById(R.id.rank_text)
        private val avatarImage: ImageView = itemView.findViewById(R.id.avatar_image)
        private val nameText: TextView = itemView.findViewById(R.id.name_text)
        private val scoreText: TextView = itemView.findViewById(R.id.score_text)
        private val crownIcon: ImageView = itemView.findViewById(R.id.crown_icon)

        fun bind(entry: LeaderboardEntry) {
            rankText.text = entry.rank.toString()
            avatarImage.setImageResource(entry.avatarResId)
            nameText.text = entry.name
            scoreText.text = String.format("%,d", entry.score)

            // Set crown color based on rank
            when (entry.rank) {
                1 -> crownIcon.setColorFilter(Color.parseColor("#FFD700")) // Gold
                2 -> crownIcon.setColorFilter(Color.parseColor("#C0C0C0")) // Silver
                3 -> crownIcon.setColorFilter(Color.parseColor("#CD7F32")) // Bronze
            }
        }
    }

    // ViewHolder for Ranks 4+
    class RegularViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rankText: TextView = itemView.findViewById(R.id.rank_text)
        private val avatarImage: ImageView = itemView.findViewById(R.id.avatar_image)
        private val nameText: TextView = itemView.findViewById(R.id.name_text)
        private val scoreText: TextView = itemView.findViewById(R.id.score_text)

        fun bind(entry: LeaderboardEntry) {
            rankText.text = entry.rank.toString()
            avatarImage.setImageResource(entry.avatarResId)
            nameText.text = entry.name
            scoreText.text = "${String.format("%,d", entry.score)} pts"
        }
    }
}