package com.informatlux.test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ChatAdapter(private val messages: MutableList<ChatMessage>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_BOT = 2
        private const val VIEW_TYPE_IMAGE_PREVIEW = 3
    }

    inner class UserMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.user_message_text)
    }

    inner class BotMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.bot_message_text)
        val loadingIndicator: View = view.findViewById(R.id.bot_loading_indicator)
    }

    inner class ImagePreviewViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.image_preview)
    }

    override fun getItemViewType(position: Int): Int {
        return when (messages[position]) {
            is ChatMessage.UserMessage -> VIEW_TYPE_USER
            is ChatMessage.BotMessage -> VIEW_TYPE_BOT
            is ChatMessage.ImagePreviewMessage -> VIEW_TYPE_IMAGE_PREVIEW
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> UserMessageViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_chat_user, parent, false)
            )
            VIEW_TYPE_BOT -> BotMessageViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_ai_chatbot, parent, false)
            )
            VIEW_TYPE_IMAGE_PREVIEW -> ImagePreviewViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_chat_image_preview, parent, false)
            )
            else -> throw IllegalArgumentException("Invalid view type $viewType")
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val message = messages[position]) {
            is ChatMessage.UserMessage -> {
                (holder as UserMessageViewHolder).messageText.text = message.text
            }
            is ChatMessage.BotMessage -> {
                val botHolder = holder as BotMessageViewHolder
                if (message.isLoading) {
                    botHolder.loadingIndicator.visibility = View.VISIBLE
                    botHolder.messageText.visibility = View.GONE
                } else {
                    botHolder.loadingIndicator.visibility = View.GONE
                    botHolder.messageText.visibility = View.VISIBLE
                    botHolder.messageText.text = message.text
                }
            }
            is ChatMessage.ImagePreviewMessage -> {
                Glide.with(holder.itemView.context)
                    .load(message.uri)
                    .into((holder as ImagePreviewViewHolder).imageView)
            }
        }
    }

    fun updateMessages(newMessages: List<ChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }
}