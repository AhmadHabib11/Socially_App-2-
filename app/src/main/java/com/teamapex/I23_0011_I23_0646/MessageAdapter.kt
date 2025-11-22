package com.teamapex.I23_0011_I23_0646

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val messages: MutableList<Message>,
    private val currentUserId: String,
    private val onLongClick: (Message) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        if (holder is SentMessageViewHolder) {
            holder.bind(message, onLongClick)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message)
        }
    }

    override fun getItemCount() = messages.size

    // SIMPLE VERSION - Just add if doesn't exist, never update
    fun addMessage(message: Message) {
        val existingIndex = messages.indexOfFirst { it.id == message.id }

        if (existingIndex == -1) {
            // Message doesn't exist - add it
            messages.add(message)
            notifyItemInserted(messages.size - 1)
        }
        // If it exists - DO NOTHING, don't update it
    }

    fun updateMessage(messageId: Int, newContent: String) {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            messages[index] = messages[index].copy(content = newContent, isEdited = true)
            notifyItemChanged(index)
        }
    }

    fun deleteMessage(messageId: Int) {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            messages.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvSentMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvSentTime)
        private val ivImage: ImageView = itemView.findViewById(R.id.ivSentImage)

        fun bind(message: Message, onLongClick: (Message) -> Unit) {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            val time = sdf.format(Date(message.timestamp * 1000))
            tvTime.text = if (message.isEdited) "$time (edited)" else time

            when (message.messageType) {
                "text" -> {
                    tvMessage.visibility = View.VISIBLE
                    ivImage.visibility = View.GONE
                    tvMessage.text = message.content
                }
                "image" -> {
                    tvMessage.visibility = View.GONE
                    ivImage.visibility = View.VISIBLE
                    loadImage(ivImage, message.mediaPath)
                }
                else -> {
                    tvMessage.visibility = View.VISIBLE
                    ivImage.visibility = View.GONE
                    tvMessage.text = "[${message.messageType.uppercase()}]"
                }
            }

            itemView.setOnLongClickListener {
                onLongClick(message)
                true
            }
        }

        private fun loadImage(imageView: ImageView, path: String) {
            if (path.startsWith("data:image")) {
                val base64 = path.substringAfter("base64,")
                val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                imageView.setImageBitmap(bitmap)
            }
        }
    }

    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvReceivedMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvReceivedTime)
        private val ivImage: ImageView = itemView.findViewById(R.id.ivReceivedImage)

        fun bind(message: Message) {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            val time = sdf.format(Date(message.timestamp * 1000))
            tvTime.text = if (message.isEdited) "$time (edited)" else time

            when (message.messageType) {
                "text" -> {
                    tvMessage.visibility = View.VISIBLE
                    ivImage.visibility = View.GONE
                    tvMessage.text = message.content
                }
                "image" -> {
                    tvMessage.visibility = View.GONE
                    ivImage.visibility = View.VISIBLE
                    loadImage(ivImage, message.mediaPath)
                }
                else -> {
                    tvMessage.visibility = View.VISIBLE
                    ivImage.visibility = View.GONE
                    tvMessage.text = "[${message.messageType.uppercase()}]"
                }
            }
        }

        private fun loadImage(imageView: ImageView, path: String) {
            if (path.startsWith("data:image")) {
                val base64 = path.substringAfter("base64,")
                val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                imageView.setImageBitmap(bitmap)
            }
        }
    }
}