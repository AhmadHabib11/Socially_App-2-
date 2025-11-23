package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class profvisitnofollow : AppCompatActivity() {

    private lateinit var followButton: LinearLayout
    private lateinit var followButtonText: TextView
    private lateinit var profilePic: ImageView
    private lateinit var usernameText: TextView
    private lateinit var nameText: TextView
    private lateinit var bioLine1: TextView
    private lateinit var bioLine2: TextView
    private lateinit var websiteText: TextView
    private lateinit var postsCount: TextView
    private lateinit var followersCount: TextView
    private lateinit var followingCount: TextView

    private var visitedUserId: String = ""
    private var currentUserId: String = ""
    private var followStatus: String = "none"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profvisitnofollow)

        // Handle back button press with new API
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToSearch()
            }
        })

        // Initialize views with error handling
        try {
            followButton = findViewById(R.id.followButton)
            followButtonText = findViewById(R.id.followButtonText)
            profilePic = findViewById(R.id.profilePic)
            usernameText = findViewById(R.id.usernameText)
            nameText = findViewById(R.id.nameText)
            bioLine1 = findViewById(R.id.bioLine1)
            bioLine2 = findViewById(R.id.bioLine2)
            websiteText = findViewById(R.id.websiteText)
            postsCount = findViewById(R.id.postsCount)
            followersCount = findViewById(R.id.followersCount)
            followingCount = findViewById(R.id.followingCount)
        } catch (e: Exception) {
            Log.e("ProfileVisitNoFollow", "Error initializing views: ${e.message}")
            Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Get current user ID
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        currentUserId = sp.getString("userid", "") ?: ""

        // Get visited user ID from intent
        visitedUserId = intent.getStringExtra("user_id") ?: ""

        // Validate user IDs
        if (visitedUserId.isEmpty()) {
            Toast.makeText(this, "Error: User ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Error: Please log in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Check if trying to view own profile
        if (visitedUserId == currentUserId) {
            Toast.makeText(this, "This is your profile", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, profile::class.java))
            finish()
            return
        }

        // Hide static elements
        bioLine2.visibility = View.GONE
        websiteText.visibility = View.GONE

        try {
            findViewById<LinearLayout>(R.id.followedByLayout)?.visibility = View.GONE
            findViewById<android.widget.HorizontalScrollView>(R.id.highlightsScroll)?.visibility = View.GONE
        } catch (e: Exception) {
            Log.e("ProfileVisitNoFollow", "Optional views not found: ${e.message}")
        }

        // Load user profile data
        loadUserProfile()

        // Check follow status
        checkFollowStatus()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        try {
            val search = findViewById<ImageView>(R.id.searchIcon)
            val notif = findViewById<ImageView>(R.id.notifIcon)
            val prof = findViewById<ImageView>(R.id.profileIcon)
            val create = findViewById<ImageView>(R.id.reelsIcon)
            val home = findViewById<ImageView>(R.id.homeIcon)

            home?.setOnClickListener {
                val intent = Intent(this, feedpage::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }

            create?.setOnClickListener {
                startActivity(Intent(this, createpost::class.java))
            }

            search?.setOnClickListener {
                val intent = Intent(this, searchpage::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }

            notif?.setOnClickListener {
                val intent = Intent(this, notifications::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }

            prof?.setOnClickListener {
                val intent = Intent(this, profile::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }

            // Follow button click
            followButton.setOnClickListener {
                handleFollowAction()
            }

            val msg = findViewById<TextView>(R.id.messageButton)
            msg?.setOnClickListener {
                val intent = Intent(this, dmscreen::class.java)
                intent.putExtra("userId", visitedUserId)
                intent.putExtra("username", usernameText.text.toString())
                startActivity(intent)
            }

            // BACK BUTTON - Goes to Search Page
            val back = findViewById<ImageView>(R.id.backIcon)
            back?.setOnClickListener {
                navigateToSearch()
            }

            profilePic.setOnClickListener {
                Toast.makeText(this, "View story", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ProfileVisitNoFollow", "Error setting up listeners: ${e.message}")
        }
    }

    // Navigate to search page
    private fun navigateToSearch() {
        val intent = Intent(this, searchpage::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    private fun loadUserProfile() {
        val url = "http://192.168.18.35/socially_app/get_user_data.php?user_id=$visitedUserId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        val user = obj.getJSONObject("user")

                        // Set username at top
                        val username = user.getString("username")
                        usernameText.text = username

                        // Set name
                        val firstName = user.getString("first_name")
                        val lastName = user.getString("last_name")
                        nameText.text = "$firstName $lastName"

                        // Set bio
                        val bio = user.optString("bio", "")
                        if (bio.isNotEmpty()) {
                            bioLine1.visibility = View.VISIBLE
                            bioLine1.text = bio
                        } else {
                            bioLine1.visibility = View.GONE
                        }

                        // Set website
                        val website = user.optString("website", "")
                        if (website.isNotEmpty()) {
                            websiteText.visibility = View.VISIBLE
                            websiteText.text = "🔗 $website"
                        } else {
                            websiteText.visibility = View.GONE
                        }

                        // Load follower/following counts
                        followersCount.text = user.optString("followers_count", "0")
                        followingCount.text = user.optString("following_count", "0")

                        // Load profile picture
                        val profilePicPath = user.optString("profile_pic", "")
                        if (profilePicPath.isNotEmpty()) {
                            loadProfilePicture(profilePicPath)
                        } else {
                            profilePic.setImageResource(R.drawable.story1)
                        }

                        // Fetch posts count
                        fetchUserPostsCount()
                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: Exception) {
                    Log.e("ProfileVisitNoFollow", "Error parsing response: ${e.message}")
                    Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("ProfileVisitNoFollow", "Network error: ${error.message}")
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun loadProfilePicture(profilePicPath: String) {
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
                        profilePic.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    Log.e("ProfileVisitNoFollow", "Error loading pic: ${e.message}")
                }
            },
            { error ->
                Log.e("ProfileVisitNoFollow", "Network error: ${error.message}")
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun fetchUserPostsCount() {
        val url = "http:// 192.168.18.35/socially_app/get_user_posts_count.php?user_id=$visitedUserId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        val count = obj.getInt("posts_count")
                        postsCount.text = count.toString()
                    }
                } catch (e: Exception) {
                    Log.e("ProfileVisitNoFollow", "Error: ${e.message}")
                    postsCount.text = "0"
                }
            },
            { error ->
                Log.e("ProfileVisitNoFollow", "Network error: ${error.message}")
                postsCount.text = "0"
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun checkFollowStatus() {
        val url = "http://192.168.18.35/socially_app/check_follow_status.php?follower_id=$currentUserId&following_id=$visitedUserId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        followStatus = obj.getString("status")
                        updateFollowButton()
                    }
                } catch (e: Exception) {
                    Log.e("ProfileVisitNoFollow", "Error checking follow status: ${e.message}")
                }
            },
            { error ->
                Log.e("ProfileVisitNoFollow", "Network error: ${error.message}")
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun updateFollowButton() {
        when (followStatus) {
            "none" -> {
                followButtonText.text = "Follow"
                followButton.setBackgroundResource(R.drawable.followbar)
            }
            "pending" -> {
                followButtonText.text = "Requested"
                followButton.setBackgroundResource(R.drawable.profvisbar)
            }
            "accepted" -> {
                val intent = Intent(this, profilevisitfollowing::class.java)
                intent.putExtra("user_id", visitedUserId)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun handleFollowAction() {
        when (followStatus) {
            "none" -> sendFollowRequest()
            "pending" -> cancelFollowRequest()
            else -> {}
        }
    }

    private fun sendFollowRequest() {
        val url = "http://192.168.18.35/socially_app/send_follow_request.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        followStatus = obj.getString("status")
                        Toast.makeText(this, "Follow request sent", Toast.LENGTH_SHORT).show()
                        updateFollowButton()
                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ProfileVisitNoFollow", "Error: ${e.message}")
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("ProfileVisitNoFollow", "Network error: ${error.message}")
                Toast.makeText(this, "Network Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["follower_id"] = currentUserId
                params["following_id"] = visitedUserId
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun cancelFollowRequest() {
        val url = "http://192.168.18.35/socially_app/unfollow.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        followStatus = "none"
                        updateFollowButton()
                        Toast.makeText(this, "Request cancelled", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ProfileVisitNoFollow", "Error: ${e.message}")
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("ProfileVisitNoFollow", "Network error: ${error.message}")
                Toast.makeText(this, "Network Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["follower_id"] = currentUserId
                params["following_id"] = visitedUserId
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up any resources if needed
    }
}