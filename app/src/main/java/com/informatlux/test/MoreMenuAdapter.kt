package com.informatlux.test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MoreMenuAdapter(
    private val menuItems: List<MoreMenuItem>,
    private val onItemClick: (MenuItemKey) -> Unit
) : RecyclerView.Adapter<MoreMenuAdapter.MoreMenuItemViewHolder>() {

    inner class MoreMenuItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.item_icon)
        private val title: TextView = itemView.findViewById(R.id.item_title)
        private val subtitle: TextView = itemView.findViewById(R.id.item_subtitle)

        fun bind(item: MoreMenuItem) {
            icon.setImageResource(item.iconResId)
            title.text = item.title
            subtitle.text = item.subtitle
            itemView.setOnClickListener { onItemClick(item.key) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoreMenuItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_more_menu, parent, false)
        return MoreMenuItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoreMenuItemViewHolder, position: Int) {
        holder.bind(menuItems[position])
    }

    override fun getItemCount(): Int = menuItems.size
}