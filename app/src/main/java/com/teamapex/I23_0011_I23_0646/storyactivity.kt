package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class storyactivity : AppCompatActivity() {

    private lateinit var storyImage: ImageView
    private lateinit var storyProfileImage: ImageView
    private lateinit var username: TextView

    private var currentStoryIndex = 0
    private val userStories = mutableListOf<Story>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.storyactivity)

        // Initialize views
        storyImage = findViewById(R.id.storyImage)
        storyProfileImage = findViewById(R.id.storyProfileImage)
        username = findViewById(R.id.username)

        // Get current user info from session
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        val currentUserProfilePic = sp.getString("profile_pic", "") ?: ""

        // Load current user's profile picture
        loadProfilePicture(currentUserProfilePic)

        // Fetch user's own stories
        fetchUserStories()

        val close = findViewById<ImageView>(R.id.closeIcon)
        close.setOnClickListener {
            val intent = Intent(this, feedpage::class.java)
            startActivity(intent)
            finish()
        }

        val highlighticon = findViewById<LinearLayout>(R.id.highlight)
        highlighticon.setOnClickListener {
            val intent = Intent(this, highlight::class.java)
            startActivity(intent)
            finish()
        }

        val create = findViewById<LinearLayout>(R.id.createvid)
        create.setOnClickListener {
            val intent = Intent(this, camera::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loadProfilePicture(profilePicPath: String) {
        if (profilePicPath.isEmpty()) {
            storyProfileImage.setImageResource(R.drawable.gurskydp) // Default
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
                        storyProfileImage.setImageBitmap(bitmap)
                    } else {
                        storyProfileImage.setImageResource(R.drawable.gurskydp)
                    }
                } catch (e: Exception) {
                    Log.e("StoryActivity", "Error loading profile pic: ${e.message}")
                    storyProfileImage.setImageResource(R.drawable.gurskydp)
                }
            },
            { error ->
                Log.e("StoryActivity", "Network error loading profile pic: ${error.message}")
                storyProfileImage.setImageResource(R.drawable.gurskydp)
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun fetchUserStories() {
        // Get logged-in user's ID
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        val userId = sp.getString("userid", "") ?: ""
        val usernameStr = sp.getString("username", "Your Story") ?: "Your Story"

        if (userId.isEmpty()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://192.168.18.35/socially_app/get_stories.php?user_id=$userId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    Log.d("UserStories", "Response: $response")
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        val storiesArray = obj.getJSONArray("stories")
                        userStories.clear()

                        for (i in 0 until storiesArray.length()) {
                            val storyObj = storiesArray.getJSONObject(i)

                            val story = Story(
                                id = storyObj.getInt("id"),
                                userId = storyObj.getInt("user_id"),
                                username = storyObj.getString("username"),
                                firstName = storyObj.getString("first_name"),
                                lastName = storyObj.getString("last_name"),
                                profilePic = storyObj.getString("profile_pic"),
                                mediaPath = storyObj.getString("media_path"),
                                mediaType = storyObj.getString("media_type"),
                                mediaBase64 = storyObj.getString("media_base64"),
                                createdAt = storyObj.getString("created_at"),
                                expiresAt = storyObj.getString("expires_at"),
                                views = storyObj.getInt("views")
                            )

                            userStories.add(story)
                        }

                        if (userStories.isNotEmpty()) {
                            displayStory(0)
                        } else {
                            Toast.makeText(this, "No active stories", Toast.LENGTH_SHORT).show()
                            username.text = usernameStr
                        }

                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
                        username.text = usernameStr
                    }

                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("UserStories", "Parse error: ${e.message}")
                }
            },
            { error ->
                Toast.makeText(this, "Network Error: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e("UserStories", "Network error: ${error.message}")
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun displayStory(index: Int) {
        if (index < 0 || index >= userStories.size) return

        val story = userStories[index]
        currentStoryIndex = index

        // Set username
        username.text = story.username

        // Load story image
        if (story.mediaBase64.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(story.mediaBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                storyImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.e("StoryActivity", "Error decoding image: ${e.message}")
            }
        }

        // Update progress bars based on story count and current index
        updateProgressBars()
    }

    private fun updateProgressBars() {
        // You can implement progress bar updates here based on story count
        // For now, this is a placeholder
    }
}