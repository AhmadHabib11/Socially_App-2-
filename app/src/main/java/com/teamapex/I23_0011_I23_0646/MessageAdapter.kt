package com.teamapex.I23_0011_I23_0646

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import android.widget.MediaController
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
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
        private const val VIEW_TYPE_SENT_SHARED_POST = 3
        private const val VIEW_TYPE_RECEIVED_SHARED_POST = 4
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        val isSent = message.senderId == currentUserId

        return when (message.messageType) {
            "shared_post" -> if (isSent) VIEW_TYPE_SENT_SHARED_POST else VIEW_TYPE_RECEIVED_SHARED_POST
            else -> if (isSent) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_sent, parent, false)
                SentMessageViewHolder(view)
            }
            VIEW_TYPE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_received, parent, false)
                ReceivedMessageViewHolder(view)
            }
            VIEW_TYPE_SENT_SHARED_POST -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_post_send, parent, false)
                SentSharedPostViewHolder(view)
            }
            VIEW_TYPE_RECEIVED_SHARED_POST -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_post_received, parent, false)
                ReceivedSharedPostViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_sent, parent, false)
                SentMessageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        when (holder) {
            is SentMessageViewHolder -> holder.bind(message, onLongClick)
            is ReceivedMessageViewHolder -> holder.bind(message)
            is SentSharedPostViewHolder -> holder.bind(message, onLongClick)
            is ReceivedSharedPostViewHolder -> holder.bind(message)
        }
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: Message) {
        val existingIndex = messages.indexOfFirst { it.id == message.id }

        if (existingIndex == -1) {
            messages.add(message)
            notifyItemInserted(messages.size - 1)
        }
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

    // Shared Post ViewHolders
    class SentSharedPostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val postImage: ImageView = itemView.findViewById(R.id.ivPostImage)
        private val postUsername: TextView = itemView.findViewById(R.id.tvPostUsername)
        private val postCaption: TextView = itemView.findViewById(R.id.tvPostCaption)
        private val timeText: TextView = itemView.findViewById(R.id.tvPostTime)
        private val shareLabel: TextView = itemView.findViewById(R.id.tvShareLabel)

        fun bind(message: Message, onLongClick: (Message) -> Unit) {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            timeText.text = sdf.format(Date(message.timestamp * 1000))

            message.sharedPostId?.let { postId ->
                loadSharedPostData(postId, postImage, postUsername, postCaption)
            } ?: run {
                postUsername.text = "Post unavailable"
                postCaption.text = ""
            }

            itemView.setOnLongClickListener {
                onLongClick(message)
                true
            }
        }

        private fun loadSharedPostData(
            postId: Int,
            imageView: ImageView,
            usernameView: TextView,
            captionView: TextView
        ) {
            val url = "http://192.168.18.35/socially_app/get_post_details.php?post_id=$postId"

            val request = StringRequest(
                Request.Method.GET, url,
                { response ->
                    try {
                        val json = JSONObject(response)
                        if (json.getInt("statuscode") == 200) {
                            val post = json.getJSONObject("post")

                            usernameView.text = post.getString("username")
                            captionView.text = post.getString("caption")

                            val imageBase64 = post.getString("image_base64")
                            if (imageBase64.isNotEmpty()) {
                                val decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                imageView.setImageBitmap(bitmap)
                            }
                        } else {
                            usernameView.text = "Post unavailable"
                            captionView.text = json.optString("message", "")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MessageAdapter", "Error loading shared post: ${e.message}")
                        usernameView.text = "Error loading post"
                        captionView.text = ""
                    }
                },
                { error ->
                    android.util.Log.e("MessageAdapter", "Network error: ${error.message}")
                    usernameView.text = "Failed to load post"
                    captionView.text = ""
                }
            )

            Volley.newRequestQueue(imageView.context).add(request)
        }
    }

    class ReceivedSharedPostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val postImage: ImageView = itemView.findViewById(R.id.ivPostImage)
        private val postUsername: TextView = itemView.findViewById(R.id.tvPostUsername)
        private val postCaption: TextView = itemView.findViewById(R.id.tvPostCaption)
        private val timeText: TextView = itemView.findViewById(R.id.tvPostTime)
        private val shareLabel: TextView = itemView.findViewById(R.id.tvShareLabel)

        fun bind(message: Message) {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            timeText.text = sdf.format(Date(message.timestamp * 1000))

            message.sharedPostId?.let { postId ->
                loadSharedPostData(postId, postImage, postUsername, postCaption)
            } ?: run {
                postUsername.text = "Post unavailable"
                postCaption.text = ""
            }
        }

        private fun loadSharedPostData(
            postId: Int,
            imageView: ImageView,
            usernameView: TextView,
            captionView: TextView
        ) {
            val url = "http://192.168.18.35/socially_app/get_post_details.php?post_id=$postId"

            val request = StringRequest(
                Request.Method.GET, url,
                { response ->
                    try {
                        val json = JSONObject(response)
                        if (json.getInt("statuscode") == 200) {
                            val post = json.getJSONObject("post")

                            usernameView.text = post.getString("username")
                            captionView.text = post.getString("caption")

                            val imageBase64 = post.getString("image_base64")
                            if (imageBase64.isNotEmpty()) {
                                val decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                imageView.setImageBitmap(bitmap)
                            }
                        } else {
                            usernameView.text = "Post unavailable"
                            captionView.text = json.optString("message", "")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MessageAdapter", "Error loading shared post: ${e.message}")
                        usernameView.text = "Error loading post"
                        captionView.text = ""
                    }
                },
                { error ->
                    android.util.Log.e("MessageAdapter", "Network error: ${error.message}")
                    usernameView.text = "Failed to load post"
                    captionView.text = ""
                }
            )

            Volley.newRequestQueue(imageView.context).add(request)
        }
    }

    // Original ViewHolders (kept unchanged)
    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvSentMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvSentTime)
        private val ivImage: ImageView = itemView.findViewById(R.id.ivSentImage)
        private val flVideo: FrameLayout = itemView.findViewById(R.id.flSentVideo)
        private val vvVideo: VideoView = itemView.findViewById(R.id.vvSentVideo)
        private val ivPlayButton: ImageView = itemView.findViewById(R.id.ivSentPlayButton)

        private var currentVideoFile: java.io.File? = null

        fun bind(message: Message, onLongClick: (Message) -> Unit) {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            val time = sdf.format(Date(message.timestamp * 1000))
            tvTime.text = if (message.isEdited) "$time (edited)" else time

            // Clean up previous video
            currentVideoFile?.let { if (it.exists()) it.delete() }
            vvVideo.stopPlayback()
            vvVideo.suspend()

            when (message.messageType) {
                "text" -> {
                    tvMessage.visibility = View.VISIBLE
                    ivImage.visibility = View.GONE
                    flVideo.visibility = View.GONE
                    tvMessage.text = message.content
                }
                "image" -> {
                    tvMessage.visibility = View.GONE
                    ivImage.visibility = View.VISIBLE
                    flVideo.visibility = View.GONE
                    loadImage(ivImage, message.mediaPath)
                }
                "video" -> {
                    tvMessage.visibility = View.GONE
                    ivImage.visibility = View.GONE
                    flVideo.visibility = View.VISIBLE
                    loadVideo(vvVideo, ivPlayButton, message.mediaPath, message.id)
                }
                else -> {
                    tvMessage.visibility = View.VISIBLE
                    ivImage.visibility = View.GONE
                    flVideo.visibility = View.GONE
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

        private fun loadVideo(videoView: VideoView, playButton: ImageView, path: String, messageId: Int) {
            if (path.startsWith("data:video")) {
                try {
                    val base64 = path.substringAfter("base64,")
                    val decodedBytes = Base64.decode(base64, Base64.DEFAULT)

                    val tempFile = java.io.File(videoView.context.cacheDir, "video_msg_${messageId}.mp4")

                    if (!tempFile.exists() || tempFile.length() == 0L) {
                        tempFile.writeBytes(decodedBytes)
                        android.util.Log.d("MessageAdapter", "Video file created: ${tempFile.absolutePath}, size: ${tempFile.length()}")
                    } else {
                        android.util.Log.d("MessageAdapter", "Using existing video file: ${tempFile.absolutePath}")
                    }

                    currentVideoFile = tempFile

                    videoView.setVideoURI(Uri.fromFile(tempFile))

                    val mediaController = MediaController(videoView.context)
                    mediaController.setAnchorView(videoView)
                    videoView.setMediaController(mediaController)

                    playButton.visibility = View.VISIBLE

                    videoView.setOnPreparedListener { mp ->
                        android.util.Log.d("MessageAdapter", "Video prepared successfully")
                    }

                    playButton.setOnClickListener {
                        playButton.visibility = View.GONE
                        videoView.start()
                    }

                    videoView.setOnCompletionListener {
                        playButton.visibility = View.VISIBLE
                        videoView.seekTo(0)
                    }

                    videoView.setOnErrorListener { mp, what, extra ->
                        android.util.Log.e("MessageAdapter", "Video error: what=$what, extra=$extra, path=${tempFile.absolutePath}, exists=${tempFile.exists()}")
                        playButton.visibility = View.VISIBLE
                        true
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MessageAdapter", "Error loading video: ${e.message}", e)
                    playButton.visibility = View.VISIBLE
                }
            } else if (path.startsWith("http")) {
                videoView.setVideoURI(Uri.parse(path))

                val mediaController = MediaController(videoView.context)
                mediaController.setAnchorView(videoView)
                videoView.setMediaController(mediaController)

                playButton.visibility = View.VISIBLE

                videoView.setOnPreparedListener { mp ->
                    android.util.Log.d("MessageAdapter", "Video from URL prepared")
                }

                playButton.setOnClickListener {
                    playButton.visibility = View.GONE
                    videoView.start()
                }

                videoView.setOnCompletionListener {
                    playButton.visibility = View.VISIBLE
                    videoView.seekTo(0)
                }

                videoView.setOnErrorListener { mp, what, extra ->
                    android.util.Log.e("MessageAdapter", "Video URL error: what=$what, extra=$extra")
                    playButton.visibility = View.VISIBLE
                    true
                }
            }
        }
    }

    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvReceivedMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvReceivedTime)
        private val ivImage: ImageView = itemView.findViewById(R.id.ivReceivedImage)
        private val flVideo: FrameLayout = itemView.findViewById(R.id.flReceivedVideo)
        private val vvVideo: VideoView = itemView.findViewById(R.id.vvReceivedVideo)
        private val ivPlayButton: ImageView = itemView.findViewById(R.id.ivReceivedPlayButton)

        fun bind(message: Message) {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            val time = sdf.format(Date(message.timestamp * 1000))
            tvTime.text = if (message.isEdited) "$time (edited)" else time

            when (message.messageType) {
                "text" -> {
                    tvMessage.visibility = View.VISIBLE
                    ivImage.visibility = View.GONE
                    flVideo.visibility = View.GONE
                    tvMessage.text = message.content
                }
                "image" -> {
                    tvMessage.visibility = View.GONE
                    ivImage.visibility = View.VISIBLE
                    flVideo.visibility = View.GONE
                    loadImage(ivImage, message.mediaPath)
                }
                "video" -> {
                    tvMessage.visibility = View.GONE
                    ivImage.visibility = View.GONE
                    flVideo.visibility = View.VISIBLE
                    loadVideo(vvVideo, ivPlayButton, message.mediaPath)
                }
                else -> {
                    tvMessage.visibility = View.VISIBLE
                    ivImage.visibility = View.GONE
                    flVideo.visibility = View.GONE
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

        private fun loadVideo(videoView: VideoView, playButton: ImageView, path: String) {
            if (path.startsWith("data:video")) {
                try {
                    val base64 = path.substringAfter("base64,")
                    val decodedBytes = Base64.decode(base64, Base64.DEFAULT)

                    val tempFile = java.io.File(videoView.context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
                    tempFile.writeBytes(decodedBytes)

                    videoView.setVideoURI(Uri.fromFile(tempFile))

                    val mediaController = MediaController(videoView.context)
                    mediaController.setAnchorView(videoView)
                    videoView.setMediaController(mediaController)

                    playButton.visibility = View.VISIBLE
                    playButton.setOnClickListener {
                        playButton.visibility = View.GONE
                        videoView.start()
                    }

                    videoView.setOnCompletionListener {
                        playButton.visibility = View.VISIBLE
                        videoView.seekTo(0)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MessageAdapter", "Error loading video: ${e.message}")
                }
            } else if (path.startsWith("http")) {
                videoView.setVideoURI(Uri.parse(path))

                val mediaController = MediaController(videoView.context)
                mediaController.setAnchorView(videoView)
                videoView.setMediaController(mediaController)

                playButton.visibility = View.VISIBLE
                playButton.setOnClickListener {
                    playButton.visibility = View.GONE
                    videoView.start()
                }

                videoView.setOnCompletionListener {
                    playButton.visibility = View.VISIBLE
                    videoView.seekTo(0)
                }
            }
        }
    }
}