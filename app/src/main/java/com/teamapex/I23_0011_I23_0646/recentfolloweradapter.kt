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

class RecentFollowerAdapter(
    private val context: Context,
    private val followers: List<FollowUser>,
    private val onClick: (FollowUser) -> Unit
) : RecyclerView.Adapter<RecentFollowerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification_follow, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(followers[position], context, onClick)
    }

    override fun getItemCount() = followers.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profilePic: ImageView = itemView.findViewById(R.id.profilePic)
        private val notificationText: TextView = itemView.findViewById(R.id.notificationText)

        fun bind(follower: FollowUser, context: Context, onClick: (FollowUser) -> Unit) {
            notificationText.text = "${follower.username} started following you"

            // Load profile picture
            loadProfilePicture(follower.profilePic, profilePic, context)

            itemView.setOnClickListener {
                onClick(follower)
            }
        }

        private fun loadProfilePicture(profilePicPath: String, imageView: ImageView, context: Context) {
            if (profilePicPath.isEmpty()) {
                imageView.setImageResource(R.drawable.story1)
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
                            imageView.setImageResource(R.drawable.story1)
                        }
                    } catch (e: Exception) {
                        Log.e("RecentFollowerAdapter", "Error: ${e.message}")
                        imageView.setImageResource(R.drawable.story1)
                    }
                },
                { error ->
                    Log.e("RecentFollowerAdapter", "Network error: ${error.message}")
                    imageView.setImageResource(R.drawable.story1)
                }
            )

            Volley.newRequestQueue(context).add(request)
        }
    }
}