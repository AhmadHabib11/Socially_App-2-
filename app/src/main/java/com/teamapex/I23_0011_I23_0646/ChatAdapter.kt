package com.teamapex.I23_0011_I23_0646

import android.graphics.BitmapFactory
import android.graphics.Outline
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val chats: MutableList<Chat>,
    private val currentUserId: String,
    private val onClick: (Chat) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    companion object {
        private const val BASE_URL = "http://192.168.18.35/socially_app/"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position], currentUserId, onClick)
    }

    override fun getItemCount() = chats.size

    fun addChat(chat: Chat) {
        val existingIndex = chats.indexOfFirst { it.chatId == chat.chatId }
        if (existingIndex != -1) {
            chats[existingIndex] = chat
            notifyItemChanged(existingIndex)
            Log.d("ChatAdapter", "Updated chat: ${chat.username} (Online: ${chat.isOnline})")
        } else {
            chats.add(0, chat)
            notifyItemInserted(0)
            Log.d("ChatAdapter", "Added new chat: ${chat.username} (Online: ${chat.isOnline})")
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
        private val statusIndicator: View = itemView.findViewById(R.id.statusIndicator)
        private val statusText: TextView = itemView.findViewById(R.id.tvStatusText)

        init {
            // Make profile image circular using clipToOutline
            profileImage.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            profileImage.clipToOutline = true
        }

        fun bind(chat: Chat, currentUserId: String, onClick: (Chat) -> Unit) {
            username.text = chat.username

            // Load profile picture
            loadProfilePicture(chat.profileImage)

            // Always show status indicator - just change color based on online/offline
            statusIndicator.visibility = View.VISIBLE

            if (chat.isOnline) {
                // User is online - show green indicator
                statusIndicator.setBackgroundResource(R.drawable.status_indicator_online)

                statusText.visibility = View.VISIBLE
                statusText.text = "Online"
                statusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))

                Log.d("ChatAdapter", "${chat.username} is ONLINE")
            } else {
                // User is offline - show grey indicator
                statusIndicator.setBackgroundResource(R.drawable.status_indicator_offline)

                // Show last seen if available
                if (chat.lastSeen > 0) {
                    statusText.visibility = View.VISIBLE
                    statusText.text = formatLastSeen(chat.lastSeen)
                    statusText.setTextColor(android.graphics.Color.parseColor("#999999"))

                    Log.d("ChatAdapter", "${chat.username} is OFFLINE - last seen: ${chat.lastSeen}")
                } else {
                    statusText.visibility = View.VISIBLE
                    statusText.text = "Offline"
                    statusText.setTextColor(android.graphics.Color.parseColor("#999999"))

                    Log.d("ChatAdapter", "${chat.username} is OFFLINE - no last seen data")
                }
            }

            // Format the last message with status indicator
            val messageText = when {
                chat.lastMessage.isEmpty() -> ""
                chat.lastMessageSenderId == currentUserId -> {
                    // Message sent by current user - show delivery status
                    val statusPrefix = when (chat.deliveryStatus) {
                        "seen" -> "Seen"
                        "delivered" -> "Delivered"
                        else -> "Sent"
                    }

                    val preview = when {
                        chat.lastMessage.startsWith("Image") || chat.lastMessage == "image" -> "📷 Photo"
                        chat.lastMessage.startsWith("Video") || chat.lastMessage == "video" -> "🎥 Video"
                        chat.lastMessage.length > 30 -> chat.lastMessage.substring(0, 30) + "..."
                        else -> chat.lastMessage
                    }

                    "$statusPrefix · $preview"
                }
                else -> {
                    // Message received from other user
                    when {
                        chat.lastMessage.startsWith("Image") || chat.lastMessage == "image" -> "📷 Photo"
                        chat.lastMessage.startsWith("Video") || chat.lastMessage == "video" -> "🎥 Video"
                        chat.lastMessage.length > 35 -> chat.lastMessage.substring(0, 35) + "..."
                        else -> chat.lastMessage
                    }
                }
            }

            lastMessage.text = messageText
            lastMessage.alpha = if (chat.lastMessage.isEmpty()) 0.5f else 1.0f

            // Format timestamp
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            timestamp.text = sdf.format(Date(chat.timestamp * 1000))

            itemView.setOnClickListener {
                onClick(chat)
            }
        }

        private fun formatLastSeen(lastSeenTimestamp: Long): String {
            val currentTime = System.currentTimeMillis() / 1000
            val diff = currentTime - lastSeenTimestamp

            return when {
                diff < 60 -> "Active just now"
                diff < 3600 -> "Active ${diff / 60}m ago"
                diff < 86400 -> "Active ${diff / 3600}h ago"
                diff < 604800 -> "Active ${diff / 86400}d ago"
                else -> {
                    val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                    "Active ${sdf.format(Date(lastSeenTimestamp * 1000))}"
                }
            }
        }

        private fun loadProfilePicture(profilePicPath: String) {
            if (profilePicPath.isEmpty()) {
                profileImage.setImageResource(R.drawable.story1)
                return
            }

            val url = "${BASE_URL}get_profile_pic.php?path=$profilePicPath"

            Log.d("ChatAdapter", "Loading profile pic from: $url")

            val request = StringRequest(
                Request.Method.GET, url,
                { response ->
                    try {
                        val obj = JSONObject(response)
                        if (obj.getInt("statuscode") == 200) {
                            val imageBase64 = obj.getString("image")
                            val decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            profileImage.setImageBitmap(bitmap)
                            Log.d("ChatAdapter", "Profile pic loaded successfully")
                        } else {
                            Log.e("ChatAdapter", "Error: ${obj.optString("message")}")
                            profileImage.setImageResource(R.drawable.story1)
                        }
                    } catch (e: Exception) {
                        Log.e("ChatAdapter", "Error loading profile pic: ${e.message}")
                        profileImage.setImageResource(R.drawable.story1)
                    }
                },
                { error ->
                    Log.e("ChatAdapter", "Network error: ${error.message}")
                    profileImage.setImageResource(R.drawable.story1)
                }
            )

            Volley.newRequestQueue(itemView.context).add(request)
        }
    }
}