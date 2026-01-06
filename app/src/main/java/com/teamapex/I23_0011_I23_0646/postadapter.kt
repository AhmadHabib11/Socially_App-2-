package com.teamapex.I23_0011_I23_0646

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class PostAdapter(
    private val context: Context,
    private val posts: List<Post>,
    private val currentUsername: String,
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onShareClick: (Post) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val postProfilePic: ImageView = view.findViewById(R.id.postProfilePic)
        val postUsername: TextView = view.findViewById(R.id.postUsername)
        val postImage: ImageView = view.findViewById(R.id.postImage)
        val likeIcon: ImageView = view.findViewById(R.id.likeIcon)
        val likeCount: TextView = view.findViewById(R.id.likeCount)
        val commentIcon: ImageView = view.findViewById(R.id.commentIcon)
        val commentCount: TextView = view.findViewById(R.id.commentCount)
        val shareIcon: ImageView = view.findViewById(R.id.shareIcon)
        val saveIcon: ImageView = view.findViewById(R.id.saveIcon)
        val likedByText: TextView = view.findViewById(R.id.likedByText)
        val captionUser: TextView = view.findViewById(R.id.captionUser)
        val postCaption: TextView = view.findViewById(R.id.postCaption)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        holder.postUsername.text = post.username
        holder.captionUser.text = post.username
        holder.postCaption.text = post.caption

        loadProfilePicture(post.userProfilePic, holder.postProfilePic)

        if (post.imageBase64.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(post.imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.postImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.postImage.setImageResource(R.drawable.selimg)
            }
        }

        holder.likeCount.text = "${post.likesCount}"
        if (post.isLikedByCurrentUser) {
            holder.likeIcon.setImageResource(R.drawable.redheart)
        } else {
            holder.likeIcon.setImageResource(R.drawable.like)
        }

        holder.commentCount.text = "${post.commentsCount}"

        if (post.likesCount > 0) {
            if (post.likesCount == 1) {
                if (post.isLikedByCurrentUser) {
                    holder.likedByText.text = "Liked by $currentUsername"
                } else {
                    holder.likedByText.text = "Liked by ${post.username}"
                }
            } else {
                if (post.isLikedByCurrentUser) {
                    holder.likedByText.text = "Liked by $currentUsername and ${post.likesCount - 1} ${if (post.likesCount - 1 == 1) "other" else "others"}"
                } else {
                    holder.likedByText.text = "Liked by ${post.likesCount} people"
                }
            }
        } else {
            holder.likedByText.text = "Be the first to like this"
        }

        holder.likeIcon.setOnClickListener {
            onLikeClick(post)
        }

        holder.commentIcon.setOnClickListener {
            onCommentClick(post)
        }

        holder.shareIcon.setOnClickListener {
            showShareDialog(post)
        }
    }

    override fun getItemCount(): Int = posts.size

    private fun showShareDialog(post: Post) {
        val options = arrayOf("Send in Direct Message", "Share to...", "Cancel")

        AlertDialog.Builder(context)
            .setTitle("Share Post")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> showUserSelectionDialog(post)
                    1 -> onShareClick(post) // Original share functionality
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun showUserSelectionDialog(post: Post) {
        // Get current user ID
        val sp = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val currentUserId = sp.getString("userid", "") ?: ""

        // Fetch chats
        val url = "http://172.15.44.21/socially_app/get_chats.php?user_id=$currentUserId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getInt("statuscode") == 200) {
                        val chatsArray = json.getJSONArray("chats")
                        val chatsList = mutableListOf<Pair<String, String>>() // chatId to username

                        for (i in 0 until chatsArray.length()) {
                            val chatJson = chatsArray.getJSONObject(i)
                            val chatId = chatJson.getString("chat_id")
                            val username = chatJson.getString("username")
                            chatsList.add(Pair(chatId, username))
                        }

                        if (chatsList.isEmpty()) {
                            Toast.makeText(context, "No chats available. Start a conversation first!", Toast.LENGTH_SHORT).show()
                            return@StringRequest
                        }

                        // Show dialog with chat list
                        val usernames = chatsList.map { it.second }.toTypedArray()

                        AlertDialog.Builder(context)
                            .setTitle("Send to")
                            .setItems(usernames) { dialog, which ->
                                val selectedChat = chatsList[which]
                                sharePostToChat(post, selectedChat.first, selectedChat.second)
                            }
                            .setNegativeButton("Cancel", null)
                            .show()

                    } else {
                        Toast.makeText(context, "Failed to load chats", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(context, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(context).add(request)
    }

    private fun sharePostToChat(post: Post, chatId: String, username: String) {
        val sp = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val currentUserId = sp.getString("userid", "") ?: ""

        val url = "http://172.15.44.21/socially_app/send_message.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getInt("statuscode") == 200) {
                        Toast.makeText(context, "Post shared to $username", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to share post", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(context, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["chat_id"] = chatId
                params["sender_id"] = currentUserId
                params["message_type"] = "shared_post"
                params["shared_post_id"] = post.id.toString()
                params["content"] = ""
                return params
            }
        }

        Volley.newRequestQueue(context).add(request)
    }

    private fun loadProfilePicture(profilePicPath: String, imageView: ImageView) {
        if (profilePicPath.isEmpty()) {
            imageView.setImageResource(R.drawable.settings)
            return
        }

        val url = "http://172.15.44.21/socially_app/get_profile_pic.php?path=$profilePicPath"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        val imageBase64 = obj.getString("image")
                        val decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        imageView.setImageBitmap(bitmap)
                    } else {
                        imageView.setImageResource(R.drawable.settings)
                    }
                } catch (e: Exception) {
                    Log.e("PostAdapter", "Error loading profile pic: ${e.message}")
                    imageView.setImageResource(R.drawable.settings)
                }
            },
            { error ->
                Log.e("PostAdapter", "Network error: ${error.message}")
                imageView.setImageResource(R.drawable.settings)
            }
        )

        Volley.newRequestQueue(context).add(request)
    }
}