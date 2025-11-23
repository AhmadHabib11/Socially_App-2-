package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class profile : AppCompatActivity() {

    private lateinit var profilePic: ImageView
    private lateinit var profileIconBottomNav: ImageView
    private lateinit var usernameText: TextView
    private lateinit var nameText: TextView
    private lateinit var bioLine1: TextView
    private lateinit var postsCount: TextView
    private lateinit var followersCount: TextView
    private lateinit var followingCount: TextView

    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile)

        // Get current user info from session
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        currentUserId = sp.getString("userid", "") ?: ""
        val username = sp.getString("username", "") ?: ""
        val firstName = sp.getString("first_name", "") ?: ""
        val lastName = sp.getString("last_name", "") ?: ""
        val userProfilePic = sp.getString("profile_pic", "") ?: ""

        Log.d("Profile", "=== PROFILE LOADED ===")
        Log.d("Profile", "Current User ID: $currentUserId")
        Log.d("Profile", "Username: $username")

        // Initialize views
        profilePic = findViewById(R.id.profilePic)
        profileIconBottomNav = findViewById(R.id.profileIcon)
        usernameText = findViewById(R.id.usernameText)
        nameText = findViewById(R.id.nameText)
        bioLine1 = findViewById(R.id.bioLine1)
        postsCount = findViewById(R.id.postsCount)
        followersCount = findViewById(R.id.followersCount)
        followingCount = findViewById(R.id.followingCount)

        Log.d("Profile", "All TextViews initialized")

        // Set username at top
        usernameText.text = username

        // Set name
        nameText.text = "$firstName $lastName"

        // Set initial values
        postsCount.text = "0"
        followersCount.text = "0"
        followingCount.text = "0"
        bioLine1.text = "Loading..."

        Log.d("Profile", "Initial values set")

        // Load profile pictures
        loadProfilePicture(userProfilePic)

        // Fetch user's full data
        fetchUserData()

        // Fetch user's posts count
        fetchUserPostsCount()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        val search = findViewById<ImageView>(R.id.searchIcon)
        val notif = findViewById<ImageView>(R.id.notifIcon)
        val create = findViewById<ImageView>(R.id.reelsIcon)
        val homeicon = findViewById<ImageView>(R.id.homeIcon)

        homeicon.setOnClickListener {
            startActivity(Intent(this, feedpage::class.java))
            finish()
        }

        search.setOnClickListener {
            startActivity(Intent(this, searchpage::class.java))
            finish()
        }

        notif.setOnClickListener {
            startActivity(Intent(this, notifications::class.java))
            finish()
        }

        profileIconBottomNav.setOnClickListener {
            Toast.makeText(this, "You're already on your profile", Toast.LENGTH_SHORT).show()
        }

        create.setOnClickListener {
            startActivity(Intent(this, createpost::class.java))
            finish()
        }

        profilePic.setOnClickListener {
            startActivity(Intent(this, storyactivity::class.java))
            finish()
        }

        val editprof = findViewById<TextView>(R.id.editProfileBar)
        editprof.setOnClickListener {
            startActivity(Intent(this, editprofile::class.java))
            finish()
        }

        val h1 = findViewById<ImageView>(R.id.h1)
        val h2 = findViewById<ImageView>(R.id.h2)
        val h3 = findViewById<ImageView>(R.id.h3)
        val newstory = findViewById<ImageView>(R.id.newstory)

        h1.setOnClickListener {
            startActivity(Intent(this, highlight::class.java))
            finish()
        }

        h2.setOnClickListener {
            startActivity(Intent(this, highlight::class.java))
            finish()
        }

        h3.setOnClickListener {
            startActivity(Intent(this, highlight::class.java))
            finish()
        }

        newstory.setOnClickListener {
            startActivity(Intent(this, camera::class.java))
            finish()
        }
    }

    private fun loadProfilePicture(profilePicPath: String) {
        if (profilePicPath.isEmpty()) {
            profilePic.setImageResource(R.drawable.story1)
            profileIconBottomNav.setImageResource(R.drawable.story1)
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

                        profilePic.setImageBitmap(bitmap)
                        profileIconBottomNav.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    Log.e("Profile", "Error loading profile pic: ${e.message}")
                }
            },
            { error ->
                Log.e("Profile", "Network error: ${error.message}")
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun fetchUserData() {
        val url = "http://192.168.18.35/socially_app/get_user_data.php?user_id=$currentUserId"

        Log.d("Profile", "=== FETCHING USER DATA ===")
        Log.d("Profile", "URL: $url")

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                Log.d("Profile", "=== RESPONSE RECEIVED ===")
                Log.d("Profile", "Response: $response")

                try {
                    val obj = JSONObject(response)
                    val statusCode = obj.getInt("statuscode")

                    Log.d("Profile", "Status Code: $statusCode")

                    if (statusCode == 200) {
                        val user = obj.getJSONObject("user")

                        Log.d("Profile", "User JSON: $user")

                        // Get bio
                        val bio = user.optString("bio", "")
                        bioLine1.text = if (bio.isEmpty()) "No bio yet" else bio

                        // Get counts
                        val followersValue = user.optInt("followers_count", -1)
                        val followingValue = user.optInt("following_count", -1)

                        Log.d("Profile", "=== COUNTS RECEIVED ===")
                        Log.d("Profile", "Followers from JSON: $followersValue")
                        Log.d("Profile", "Following from JSON: $followingValue")

                        // Update UI
                        followersCount.text = followersValue.toString()
                        followingCount.text = followingValue.toString()

                        Log.d("Profile", "=== UI UPDATED ===")
                        Log.d("Profile", "followersCount.text = ${followersCount.text}")
                        Log.d("Profile", "followingCount.text = ${followingCount.text}")

                    } else {
                        Log.e("Profile", "Error: ${obj.optString("message")}")
                        bioLine1.text = "Error loading bio"
                    }
                } catch (e: Exception) {
                    Log.e("Profile", "=== PARSING ERROR ===")
                    Log.e("Profile", "Error: ${e.message}")
                    e.printStackTrace()
                    bioLine1.text = "Error"
                }
            },
            { error ->
                Log.e("Profile", "=== NETWORK ERROR ===")
                Log.e("Profile", "Error: ${error.message}")
                error.printStackTrace()
                bioLine1.text = "Network error"
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun fetchUserPostsCount() {
        val url = "http://192.168.18.35/socially_app/get_user_posts_count.php?user_id=$currentUserId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        val count = obj.getInt("posts_count")
                        postsCount.text = count.toString()
                        Log.d("Profile", "Posts count: $count")
                    }
                } catch (e: Exception) {
                    Log.e("Profile", "Error fetching posts: ${e.message}")
                }
            },
            { error ->
                Log.e("Profile", "Network error: ${error.message}")
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    override fun onResume() {
        super.onResume()
        Log.d("Profile", "=== ON RESUME ===")

        // Refresh data
        fetchUserData()
        fetchUserPostsCount()

        // Reload profile picture
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        val userProfilePic = sp.getString("profile_pic", "") ?: ""
        loadProfilePicture(userProfilePic)
    }
}