package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class notifications2 : AppCompatActivity() {

    private lateinit var followRequestsRecyclerView: RecyclerView
    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var followRequestsAdapter: FollowRequestAdapter
    private lateinit var noRequestsText: TextView
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notifications2)

        // Get current user ID
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        currentUserId = sp.getString("userid", "") ?: ""

        // Initialize views
        followRequestsRecyclerView = findViewById(R.id.followRequestsRecyclerView)
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView)
        noRequestsText = findViewById(R.id.noRequestsText)

        // Setup Follow Requests RecyclerView
        followRequestsAdapter = FollowRequestAdapter(
            this,
            mutableListOf(),
            onAccept = { request -> acceptFollowRequest(request) },
            onReject = { request -> rejectFollowRequest(request) }
        )

        followRequestsRecyclerView.layoutManager = LinearLayoutManager(this)
        followRequestsRecyclerView.adapter = followRequestsAdapter

        // Setup Notifications RecyclerView (placeholder for now)
        notificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        // TODO: Add notifications adapter when you have other notifications

        // Load follow requests
        loadFollowRequests()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        val ftab = findViewById<TextView>(R.id.followingTab)
        val home = findViewById<ImageView>(R.id.homeIcon)
        val search = findViewById<ImageView>(R.id.searchIcon)
        val create = findViewById<ImageView>(R.id.reelsIcon)
        val notifIcon = findViewById<ImageView>(R.id.notifIcon)
        val profileIcon = findViewById<ImageView>(R.id.profileIcon)

        ftab.setOnClickListener {
            startActivity(Intent(this, notifications::class.java))
            finish()
        }

        home.setOnClickListener {
            startActivity(Intent(this, feedpage::class.java))
            finish()
        }

        search.setOnClickListener {
            startActivity(Intent(this, searchpage::class.java))
            finish()
        }

        create.setOnClickListener {
            startActivity(Intent(this, createpost::class.java))
            finish()
        }

        notifIcon.setOnClickListener {
            Toast.makeText(this, "You're already here", Toast.LENGTH_SHORT).show()
        }

        profileIcon.setOnClickListener {
            startActivity(Intent(this, profile::class.java))
            finish()
        }
    }

    private fun loadFollowRequests() {
        val url = "http://192.168.18.35/socially_app/get_follow_requests.php?user_id=$currentUserId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    Log.d("Notifications", "Response: $response")
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        val requestsArray = obj.getJSONArray("requests")
                        val requests = mutableListOf<FollowRequest>()

                        for (i in 0 until requestsArray.length()) {
                            val reqObj = requestsArray.getJSONObject(i)

                            requests.add(
                                FollowRequest(
                                    followId = reqObj.getInt("follow_id"),
                                    followerId = reqObj.getInt("follower_id"),
                                    username = reqObj.getString("username"),
                                    firstName = reqObj.getString("first_name"),
                                    lastName = reqObj.getString("last_name"),
                                    profilePic = reqObj.getString("profile_pic"),
                                    createdAt = reqObj.getString("created_at")
                                )
                            )
                        }

                        if (requests.isEmpty()) {
                            noRequestsText.visibility = View.VISIBLE
                            followRequestsRecyclerView.visibility = View.GONE
                        } else {
                            followRequestsAdapter.updateRequests(requests)
                            noRequestsText.visibility = View.GONE
                            followRequestsRecyclerView.visibility = View.VISIBLE
                        }
                    } else {
                        noRequestsText.visibility = View.VISIBLE
                        followRequestsRecyclerView.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    Log.e("Notifications", "Error: ${e.message}")
                    Toast.makeText(this, "Error loading requests", Toast.LENGTH_SHORT).show()
                    noRequestsText.visibility = View.VISIBLE
                    followRequestsRecyclerView.visibility = View.GONE
                }
            },
            { error ->
                Log.e("Notifications", "Network error: ${error.message}")
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
                noRequestsText.visibility = View.VISIBLE
                followRequestsRecyclerView.visibility = View.GONE
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun acceptFollowRequest(request: FollowRequest) {
        val url = "http://192.168.18.35/socially_app/accept_follow_request.php"

        val volleyRequest = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    Log.d("Notifications", "Accept response: $response")
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        Toast.makeText(this, "Follow request accepted", Toast.LENGTH_SHORT).show()

                        // Remove request from list
                        followRequestsAdapter.removeRequest(request)

                        // Check if list is now empty
                        if (followRequestsAdapter.itemCount == 0) {
                            noRequestsText.visibility = View.VISIBLE
                            followRequestsRecyclerView.visibility = View.GONE
                        }

                        // Update follower count
                        updateLocalFollowerCount(1)
                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("Notifications", "Error: ${e.message}")
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("Notifications", "Network error: ${error.message}")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["follow_id"] = request.followId.toString()
                return params
            }
        }

        Volley.newRequestQueue(this).add(volleyRequest)
    }

    private fun rejectFollowRequest(request: FollowRequest) {
        val url = "http://192.168.18.35/socially_app/reject_follow_request.php"

        val volleyRequest = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    Log.d("Notifications", "Reject response: $response")
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        Toast.makeText(this, "Follow request rejected", Toast.LENGTH_SHORT).show()

                        // Remove request from list
                        followRequestsAdapter.removeRequest(request)

                        // Check if list is now empty
                        if (followRequestsAdapter.itemCount == 0) {
                            noRequestsText.visibility = View.VISIBLE
                            followRequestsRecyclerView.visibility = View.GONE
                        }
                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("Notifications", "Error: ${e.message}")
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("Notifications", "Network error: ${error.message}")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["follow_id"] = request.followId.toString()
                return params
            }
        }

        Volley.newRequestQueue(this).add(volleyRequest)
    }

    private fun updateLocalFollowerCount(increment: Int) {
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        val editor = sp.edit()
        val currentCount = sp.getInt("followers_count", 0)
        editor.putInt("followers_count", currentCount + increment)
        editor.apply()
        Log.d("Notifications", "Updated follower count: ${currentCount + increment}")
    }

    override fun onResume() {
        super.onResume()
        // Reload follow requests when returning to this screen
        loadFollowRequests()
    }
}