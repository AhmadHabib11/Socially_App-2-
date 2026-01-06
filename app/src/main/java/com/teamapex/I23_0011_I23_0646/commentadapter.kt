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
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(
    private val context: Context,
    private var comments: List<Comment>
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val commentUserDp: ImageView = view.findViewById(R.id.commentUserDp)
        val commentUsername: TextView = view.findViewById(R.id.commentUsername)
        val commentText: TextView = view.findViewById(R.id.commentText)
        val commentTime: TextView = view.findViewById(R.id.commentTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]

        // Set username
        holder.commentUsername.text = comment.username

        // Set comment text
        holder.commentText.text = comment.commentText

        // Load profile picture
        loadProfilePicture(comment.userProfilePic, holder.commentUserDp)

        // Format and set timestamp
        holder.commentTime.text = formatTimestamp(comment.createdAt)
    }

    override fun getItemCount(): Int = comments.size

    // Update comments list
    fun setComments(newComments: List<Comment>) {
        comments = newComments
        notifyDataSetChanged()
    }

    // Add a single comment to the top
    fun addComment(comment: Comment) {
        val mutableList = comments.toMutableList()
        mutableList.add(0, comment) // Add at beginning
        comments = mutableList
        notifyItemInserted(0)
    }

    private fun loadProfilePicture(profilePicPath: String, imageView: ImageView) {
        if (profilePicPath.isEmpty()) {
            imageView.setImageResource(R.drawable.settings) // Default
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
                    Log.e("CommentAdapter", "Error loading profile pic: ${e.message}")
                    imageView.setImageResource(R.drawable.settings)
                }
            },
            { error ->
                Log.e("CommentAdapter", "Network error: ${error.message}")
                imageView.setImageResource(R.drawable.settings)
            }
        )

        Volley.newRequestQueue(context).add(request)
    }

    private fun formatTimestamp(timestamp: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(timestamp)
            val now = Date()

            val diff = now.time - (date?.time ?: 0)
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            when {
                seconds < 60 -> "Just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                days < 7 -> "${days}d ago"
                else -> {
                    val displayFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                    displayFormat.format(date)
                }
            }
        } catch (e: Exception) {
            timestamp
        }
    }
}