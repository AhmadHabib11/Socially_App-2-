package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class feedpage : AppCompatActivity() {

    private lateinit var storyRecyclerView: RecyclerView
    private lateinit var storyAdapter: StoryAdapter
    private val storiesList = mutableListOf<Story>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.feedpage)

        // Initialize RecyclerView
        storyRecyclerView = findViewById(R.id.storyRecyclerView)
        storyRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Setup adapter with click listener
        storyAdapter = StoryAdapter(this, storiesList) { story ->
            // Handle story click - open story view
            openStoryView(story)
        }
        storyRecyclerView.adapter = storyAdapter

        // Fetch stories from server
        fetchStories()

        val messenger = findViewById<ImageView>(R.id.messageIcon)
        val search = findViewById<ImageView>(R.id.searchIcon)
        val notif = findViewById<ImageView>(R.id.notifIcon)
        val prof = findViewById<ImageView>(R.id.profileIcon)
        val cam = findViewById<ImageView>(R.id.cameraIcon)

        messenger.setOnClickListener {
            val intent = Intent(this, chatlist::class.java)
            startActivity(intent)
        }

        search.setOnClickListener {
            val intent = Intent(this, searchpage::class.java)
            startActivity(intent)
            finish()
        }

        notif.setOnClickListener {
            val intent = Intent(this, notifications::class.java)
            startActivity(intent)
            finish()
        }

        prof.setOnClickListener {
            val intent = Intent(this, profile::class.java)
            startActivity(intent)
            finish()
        }

        cam.setOnClickListener {
            val intent = Intent(this, camera::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun fetchStories() {
        val url = "http://192.168.18.109/socially_app/get_stories.php"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    Log.d("StoriesResponse", "Response: $response")
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        val storiesArray = obj.getJSONArray("stories")
                        storiesList.clear()

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

                            storiesList.add(story)
                        }

                        storyAdapter.notifyDataSetChanged()
                        Log.d("Stories", "Loaded ${storiesList.size} stories")

                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("StoriesError", "Parse error: ${e.message}")
                }
            },
            { error ->
                Toast.makeText(this, "Network Error: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e("StoriesError", "Network error: ${error.message}")
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun openStoryView(story: Story) {
        val intent = Intent(this, storyview::class.java)
        intent.putExtra("story_id", story.id)
        intent.putExtra("user_id", story.userId)
        intent.putExtra("username", story.username)
        intent.putExtra("profile_pic", story.profilePic)
        intent.putExtra("media_base64", story.mediaBase64)
        intent.putExtra("media_type", story.mediaType)
        startActivity(intent)
    }
}