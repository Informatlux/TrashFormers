package com.informatlux.test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.informatlux.test.models.Event
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventsAdapter(
    private val items: List<Any>,               // mixed Strings (headers) and Event
    private val onJoin: (Event) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_EVENT  = 1
    }

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is String) TYPE_HEADER else TYPE_EVENT
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_event_header, parent, false)
            HeaderViewHolder(v)
        } else {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_event_card, parent, false)
            EventViewHolder(v, dateFormat, onJoin)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.headerTitle.text = items[position] as String
        } else if (holder is EventViewHolder) {
            holder.bind(items[position] as Event)
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val headerTitle: TextView = itemView.findViewById(R.id.header_title)
    }

    class EventViewHolder(
        itemView: View,
        private val dateFormat: SimpleDateFormat,
        private val onJoin: (Event) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val banner: ImageView = itemView.findViewById(R.id.event_image)
        private val title: TextView = itemView.findViewById(R.id.event_title)
        private val description: TextView = itemView.findViewById(R.id.event_description)
        private val date: TextView = itemView.findViewById(R.id.event_date)
        private val location: TextView = itemView.findViewById(R.id.event_location)
        private val btnJoin: Button = itemView.findViewById(R.id.btn_join_event)

        fun bind(event: Event) {
            title.text = event.title
            description.text = event.description

            // format date
            date.text = event.eventDate?.let { dateFormat.format(it) } ?: "TBD"
            location.text = event.location

            Glide.with(itemView.context)
                .load(event.imageUrl)
                .placeholder(R.drawable.recycle_grass_image)
                .into(banner)

            val uid = runBlocking { UserManager.getCurrentUserId() }
            val joined = event.participants.contains(uid)
            btnJoin.text = if (joined) "Joined" else "Join"
            btnJoin.isEnabled = !joined && event.participants.size < event.maxParticipants
            btnJoin.setOnClickListener { onJoin(event) }
        }
    }
}
