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

class storyview : AppCompatActivity() {

    private lateinit var storyImage: ImageView
    private lateinit var storyProfileImage: ImageView
    private lateinit var username: TextView

    private var storyId: Int = 0
    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.storyview)

        // Initialize views
        storyImage = findViewById(R.id.storyImage)
        storyProfileImage = findViewById(R.id.storyProfileImage)
        username = findViewById(R.id.username)

        // Get data from intent
        storyId = intent.getIntExtra("story_id", 0)
        userId = intent.getIntExtra("user_id", 0)
        val usernameStr = intent.getStringExtra("username") ?: "Unknown"
        val profilePic = intent.getStringExtra("profile_pic") ?: ""
        val mediaBase64 = intent.getStringExtra("media_base64") ?: ""
        val mediaType = intent.getStringExtra("media_type") ?: "image"

        // Set username
        username.text = usernameStr

        // Load profile picture
        loadProfilePicture(profilePic)

        // Load story media (image/video)
        if (mediaBase64.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(mediaBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                storyImage.setImageBitmap(bitmap)

                // Increment view count
                incrementStoryViews()

            } catch (e: Exception) {
                Log.e("StoryView", "Error decoding image: ${e.message}")
                Toast.makeText(this, "Error loading story", Toast.LENGTH_SHORT).show()
            }
        }

        // Click listeners
        val send = findViewById<ImageView>(R.id.send)
        send.setOnClickListener {
            val intent = Intent(this, dmscreen::class.java)
            startActivity(intent)
            finish()
        }

        val cam = findViewById<ImageView>(R.id.camera)
        cam.setOnClickListener {
            val intent = Intent(this, camera::class.java)
            startActivity(intent)
            finish()
        }

        val close = findViewById<ImageView>(R.id.closeIcon)
        close.setOnClickListener {
            val intent = Intent(this, feedpage::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loadProfilePicture(profilePicPath: String) {
        if (profilePicPath.isEmpty()) {
            storyProfileImage.setImageResource(R.drawable.p7) // Default
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
                        storyProfileImage.setImageResource(R.drawable.p7)
                    }
                } catch (e: Exception) {
                    Log.e("StoryView", "Error loading profile pic: ${e.message}")
                    storyProfileImage.setImageResource(R.drawable.p7)
                }
            },
            { error ->
                Log.e("StoryView", "Network error loading profile pic: ${error.message}")
                storyProfileImage.setImageResource(R.drawable.p7)
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun incrementStoryViews() {
        val url = "http://192.168.18.35/socially_app/increment_story_view.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                Log.d("StoryView", "View incremented: $response")
            },
            { error ->
                Log.e("StoryView", "Error incrementing view: ${error.message}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["story_id"] = storyId.toString()
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }
}