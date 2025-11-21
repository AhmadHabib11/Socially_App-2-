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

class FollowUserAdapter(
    private val context: Context,
    private var users: List<FollowUser>,
    private val onUserClick: (FollowUser) -> Unit
) : RecyclerView.Adapter<FollowUserAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profilePic: ImageView = view.findViewById(R.id.profilePic)
        val notificationText: TextView = view.findViewById(R.id.notificationText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification_follow, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]

        // Set text
        holder.notificationText.text = "${user.username} started following you"

        // Load profile picture
        loadProfilePicture(user.profilePic, holder.profilePic)

        // Click listener
        holder.itemView.setOnClickListener {
            onUserClick(user)
        }
    }

    override fun getItemCount(): Int = users.size

    fun updateUsers(newUsers: List<FollowUser>) {
        users = newUsers
        notifyDataSetChanged()
    }

    private fun loadProfilePicture(profilePicPath: String, imageView: ImageView) {
        if (profilePicPath.isEmpty()) {
            imageView.setImageResource(R.drawable.settings)
            return
        }

        val url = "http://192.168.100.76/socially_app/get_profile_pic.php?path=$profilePicPath"

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
                    Log.e("FollowUserAdapter", "Error: ${e.message}")
                    imageView.setImageResource(R.drawable.settings)
                }
            },
            { error ->
                Log.e("FollowUserAdapter", "Network error: ${error.message}")
                imageView.setImageResource(R.drawable.settings)
            }
        )

        Volley.newRequestQueue(context).add(request)
    }
}