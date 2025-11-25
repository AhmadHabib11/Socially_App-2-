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

class notifications : AppCompatActivity() {

    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var recentFollowerAdapter: RecentFollowerAdapter
    private lateinit var emptyView: TextView
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notifications)

        // Get current user ID
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        currentUserId = sp.getString("userid", "") ?: ""

        // Find the existing feedContainer and add RecyclerView before static content
        val feedContainer = findViewById<android.widget.LinearLayout>(R.id.feedContainer)

        // Create RecyclerView programmatically and add it at the top
        notificationsRecyclerView = RecyclerView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutManager = LinearLayoutManager(this@notifications)
            isNestedScrollingEnabled = false
        }

        // Insert RecyclerView at the beginning of feedContainer
        feedContainer.addView(notificationsRecyclerView, 0)

        // Setup adapter
        recentFollowerAdapter = RecentFollowerAdapter(
            this,
            listOf(),
            onClick = { follower -> onFollowerClick(follower) }
        )
        notificationsRecyclerView.adapter = recentFollowerAdapter

        // Load recent followers
        loadRecentFollowers()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        val youtab = findViewById<TextView>(R.id.youTab)
        val search = findViewById<ImageView>(R.id.searchIcon)
        val home = findViewById<ImageView>(R.id.homeIcon)
        val profileIcon = findViewById<ImageView>(R.id.profileIcon)  // Renamed to avoid conflict
        val create = findViewById<ImageView>(R.id.reelsIcon)
        val notifIcon = findViewById<ImageView>(R.id.notifIcon)

        youtab.setOnClickListener {
            startActivity(Intent(this, notifications2::class.java))
            finish()
        }

        search.setOnClickListener {
            startActivity(Intent(this, searchpage::class.java))
            finish()
        }

        home.setOnClickListener {
            startActivity(Intent(this, feedpage::class.java))
            finish()
        }

        // Fixed: Renamed variable to avoid conflict with profile class
        profileIcon.setOnClickListener {
            val intent = Intent(this, profile::class.java)
            startActivity(intent)
            finish()
        }

        create.setOnClickListener {
            startActivity(Intent(this, createpost::class.java))
            finish()
        }

        notifIcon.setOnClickListener {
            Toast.makeText(this, "You're already here", Toast.LENGTH_SHORT).show()
        }

        // Keep existing profile picture click listeners for static content
        val p1 = findViewById<ImageView>(R.id.p1)
        val p2 = findViewById<ImageView>(R.id.p2)
        val p3 = findViewById<ImageView>(R.id.p3)
        val p4 = findViewById<ImageView>(R.id.p4)
        val p5 = findViewById<ImageView>(R.id.p5)

        p1.setOnClickListener {
            startActivity(Intent(this, profvisitnofollow::class.java))
        }
        p2.setOnClickListener {
            startActivity(Intent(this, profvisitnofollow::class.java))
        }
        p3.setOnClickListener {
            startActivity(Intent(this, profvisitnofollow::class.java))
        }
        p4.setOnClickListener {
            startActivity(Intent(this, profvisitnofollow::class.java))
        }
        p5.setOnClickListener {
            startActivity(Intent(this, profvisitnofollow::class.java))
        }
    }

    private fun loadRecentFollowers() {
        val url = "http://192.168.100.76/socially_app/get_recent_followers.php?user_id=$currentUserId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    Log.d("Notifications", "Response: $response")
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        val followersArray = obj.getJSONArray("followers")
                        val followers = mutableListOf<FollowUser>()

                        for (i in 0 until followersArray.length()) {
                            val followerObj = followersArray.getJSONObject(i)

                            followers.add(
                                FollowUser(
                                    userId = followerObj.getString("user_id"),
                                    username = followerObj.getString("username"),
                                    firstName = followerObj.getString("first_name"),
                                    lastName = followerObj.getString("last_name"),
                                    profilePic = followerObj.getString("profile_pic"),
                                    followedAt = followerObj.getString("followed_at")
                                )
                            )
                        }

                        if (followers.isNotEmpty()) {
                            recentFollowerAdapter = RecentFollowerAdapter(
                                this,
                                followers,
                                onClick = { follower -> onFollowerClick(follower) }
                            )
                            notificationsRecyclerView.adapter = recentFollowerAdapter
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Notifications", "Error: ${e.message}")
                }
            },
            { error ->
                Log.e("Notifications", "Network error: ${error.message}")
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun onFollowerClick(follower: FollowUser) {
        // Navigate to the follower's profile (they're following you, so show following profile)
        val intent = Intent(this, profilevisitfollowing::class.java)
        intent.putExtra("user_id", follower.userId)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Reload followers when returning to this screen
        loadRecentFollowers()
    }
}