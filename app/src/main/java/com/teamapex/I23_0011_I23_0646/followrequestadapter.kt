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

class FollowRequestAdapter(
    private val context: Context,
    private var requests: MutableList<FollowRequest>,
    private val onAccept: (FollowRequest) -> Unit,
    private val onReject: (FollowRequest) -> Unit
) : RecyclerView.Adapter<FollowRequestAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profilePic: ImageView = view.findViewById(R.id.profilePic)
        val requestText: TextView = view.findViewById(R.id.requestText)
        val acceptButton: TextView = view.findViewById(R.id.acceptButton)
        val rejectButton: TextView = view.findViewById(R.id.rejectButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_follow_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]

        // Set request text
        holder.requestText.text = "${request.username} wants to follow you"

        // Load profile picture
        loadProfilePicture(request.profilePic, holder.profilePic)

        // Accept button
        holder.acceptButton.setOnClickListener {
            onAccept(request)
        }

        // Reject button
        holder.rejectButton.setOnClickListener {
            onReject(request)
        }
    }

    override fun getItemCount(): Int = requests.size

    fun removeRequest(request: FollowRequest) {
        val position = requests.indexOf(request)
        if (position != -1) {
            requests.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateRequests(newRequests: List<FollowRequest>) {
        requests.clear()
        requests.addAll(newRequests)
        notifyDataSetChanged()
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
                    Log.e("FollowRequestAdapter", "Error: ${e.message}")
                    imageView.setImageResource(R.drawable.settings)
                }
            },
            { error ->
                Log.e("FollowRequestAdapter", "Network error: ${error.message}")
                imageView.setImageResource(R.drawable.settings)
            }
        )

        Volley.newRequestQueue(context).add(request)
    }
}