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

class UserSearchAdapter(
    private val context: Context,
    private val users: MutableList<MyData>,
    private val onClick: (MyData) -> Unit
) : RecyclerView.Adapter<UserSearchAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_search, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position], context, onClick)
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<MyData>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        private val username: TextView = itemView.findViewById(R.id.tvUsername)
        private val fullName: TextView = itemView.findViewById(R.id.tvFullName)
        private val statusText: TextView = itemView.findViewById(R.id.tvStatusText)

        fun bind(user: MyData, context: Context, onClick: (MyData) -> Unit) {
            username.text = user.name // This is the username

            // Show full name if available, otherwise hide
            if (user.firstName.isNotEmpty() || user.lastName.isNotEmpty()) {
                fullName.text = "${user.firstName} ${user.lastName}".trim()
                fullName.visibility = View.VISIBLE
            } else {
                fullName.visibility = View.GONE
            }

            // Always show "Start a conversation" in the search screen
            statusText.text = "Start a conversation"
            statusText.setTextColor(context.getColor(android.R.color.darker_gray))
            statusText.visibility = View.VISIBLE

            // Load profile picture
            loadProfilePicture(user.dp, profileImage, context)

            itemView.setOnClickListener {
                onClick(user)
            }
        }

        private fun loadProfilePicture(profilePicPath: String, imageView: ImageView, context: Context) {
            if (profilePicPath.isEmpty()) {
                imageView.setImageResource(R.drawable.story1)
                return
            }

            val url = "http://192.168.18.35/socially_app/get_profile_pic.php?path=$profilePicPath"

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
                        Log.e("UserSearchAdapter", "Error: ${e.message}")
                        imageView.setImageResource(R.drawable.story1)
                    }
                },
                { error ->
                    Log.e("UserSearchAdapter", "Network error: ${error.message}")
                    imageView.setImageResource(R.drawable.story1)
                }
            )

            Volley.newRequestQueue(context).add(request)
        }
    }
}