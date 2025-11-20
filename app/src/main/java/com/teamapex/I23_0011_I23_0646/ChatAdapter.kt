package com.teamapex.I23_0011_I23_0646

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val chats: MutableList<Chat>,
    private val onClick: (Chat) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position], onClick)
    }

    override fun getItemCount() = chats.size

    fun addChat(chat: Chat) {
        val existingIndex = chats.indexOfFirst { it.chatId == chat.chatId }
        if (existingIndex != -1) {
            chats[existingIndex] = chat
            notifyItemChanged(existingIndex)
        } else {
            chats.add(0, chat)
            notifyItemInserted(0)
        }
    }

    fun clearChats() {
        chats.clear()
        notifyDataSetChanged()
    }

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        private val username: TextView = itemView.findViewById(R.id.tvUsername)
        private val lastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        private val timestamp: TextView = itemView.findViewById(R.id.tvTimestamp)

        fun bind(chat: Chat, onClick: (Chat) -> Unit) {
            username.text = chat.username
            lastMessage.text = chat.lastMessage

            // Format timestamp
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            timestamp.text = sdf.format(Date(chat.timestamp * 1000))

            // Load profile image (you'd use Glide or Picasso for real implementation)
            // For now just a placeholder

            itemView.setOnClickListener {
                onClick(chat)
            }
        }
    }
}