package com.informatlux.test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class EventsAdapter(private val items: List<Any>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_EVENT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is String) VIEW_TYPE_HEADER else VIEW_TYPE_EVENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event_card, parent, false)
            EventViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.headerTitle.text = items[position] as String
        } else if (holder is EventViewHolder) {
            holder.bind(items[position] as Event)
        }
    }

    override fun getItemCount(): Int = items.size

    // ViewHolder for the "Ongoing"/"Upcoming" titles
    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val headerTitle: TextView = itemView.findViewById(R.id.header_title)
    }

    // ViewHolder for the event cards
    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val banner: ImageView = itemView.findViewById(R.id.event_banner_image)
        private val title: TextView = itemView.findViewById(R.id.event_title)
        private val description: TextView = itemView.findViewById(R.id.event_description)
        private val date: TextView = itemView.findViewById(R.id.event_date)
        private val location: TextView = itemView.findViewById(R.id.event_location)

        fun bind(event: Event) {
            title.text = event.title
            description.text = event.description
            date.text = event.date
            location.text = event.location

            // Use Glide to load the banner image
            Glide.with(itemView.context)
                .load(event.bannerUri)
                .placeholder(R.drawable.recycle_grass_image)
                .into(banner)
        }
    }
}