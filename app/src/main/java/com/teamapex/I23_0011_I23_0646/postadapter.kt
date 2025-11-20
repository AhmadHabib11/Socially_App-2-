package com.teamapex.I23_0011_I23_0646

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

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

        // Set username
        holder.postUsername.text = post.username
        holder.captionUser.text = post.username

        // Set caption
        holder.postCaption.text = post.caption

        // Load profile picture
        loadProfilePicture(post.userProfilePic, holder.postProfilePic)

        // Load post image from base64
        if (post.imageBase64.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(post.imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.postImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.postImage.setImageResource(R.drawable.selimg)
            }
        }

        // Set like count and icon
        holder.likeCount.text = "${post.likesCount}"
        if (post.isLikedByCurrentUser) {
            holder.likeIcon.setImageResource(R.drawable.redheart) // Filled red heart
        } else {
            holder.likeIcon.setImageResource(R.drawable.like) // Empty heart
        }

        // Set comment count
        holder.commentCount.text = "${post.commentsCount}"

        // Set liked by text
        if (post.likesCount > 0) {
            if (post.likesCount == 1) {
                // Show who liked it
                if (post.isLikedByCurrentUser) {
                    holder.likedByText.text = "Liked by $currentUsername"
                } else {
                    holder.likedByText.text = "Liked by ${post.username}"
                }
            } else {
                // Multiple likes
                if (post.isLikedByCurrentUser) {
                    holder.likedByText.text = "Liked by $currentUsername and ${post.likesCount - 1} ${if (post.likesCount - 1 == 1) "other" else "others"}"
                } else {
                    holder.likedByText.text = "Liked by ${post.likesCount} people"
                }
            }
        } else {
            holder.likedByText.text = "Be the first to like this"
        }

        // Click listeners
        holder.likeIcon.setOnClickListener {
            onLikeClick(post)
        }

        holder.commentIcon.setOnClickListener {
            onCommentClick(post)
        }

        holder.shareIcon.setOnClickListener {
            onShareClick(post)
        }
    }

    override fun getItemCount(): Int = posts.size

    private fun loadProfilePicture(profilePicPath: String, imageView: ImageView) {
        if (profilePicPath.isEmpty()) {
            imageView.setImageResource(R.drawable.settings) // Default
            return
        }

        val url = "http://192.168.18.109/socially_app/get_profile_pic.php?path=$profilePicPath"

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