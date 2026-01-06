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

    private lateinit var feedRecyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val postsList = mutableListOf<Post>()

    private var currentUserId: String = ""
    private var currentUsername: String = ""
    private lateinit var profileIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.feedpage)

        // Get current user ID, username and profile pic from session
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        currentUserId = sp.getString("userid", "") ?: ""
        currentUsername = sp.getString("username", "You") ?: "You"
        val userProfilePic = sp.getString("profile_pic", "") ?: ""

        // Initialize Stories RecyclerView
        storyRecyclerView = findViewById(R.id.storyRecyclerView)
        storyRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        storyAdapter = StoryAdapter(this, storiesList, currentUserId) { story ->
            openStoryView(story)
        }
        storyRecyclerView.adapter = storyAdapter

        // Initialize Posts RecyclerView
        feedRecyclerView = findViewById(R.id.feedRecyclerView)
        feedRecyclerView.layoutManager = LinearLayoutManager(this)

        postAdapter = PostAdapter(
            this,
            postsList,
            currentUsername,
            onLikeClick = { post -> handleLikeClick(post) },
            onCommentClick = { post -> handleCommentClick(post) },
            onShareClick = { post -> handleShareClick(post) }
        )
        feedRecyclerView.adapter = postAdapter

        // Fetch data
        fetchStories()
        fetchPosts()

        // Navigation icons
        val messenger = findViewById<ImageView>(R.id.messageIcon)
        val search = findViewById<ImageView>(R.id.searchIcon)
        val notif = findViewById<ImageView>(R.id.notifIcon)
        profileIcon = findViewById<ImageView>(R.id.profileIcon)
        val cam = findViewById<ImageView>(R.id.cameraIcon)
        val create = findViewById<ImageView>(R.id.reelsIcon)

        // Load user's profile picture in bottom nav
        loadUserProfilePicture(userProfilePic)

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

        profileIcon.setOnClickListener {
            val intent = Intent(this, profile::class.java)
            startActivity(intent)
            finish()
        }

        cam.setOnClickListener {
            val intent = Intent(this, camera::class.java)
            startActivity(intent)
            finish()
        }

        create.setOnClickListener {
            val intent = Intent(this, createpost::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loadUserProfilePicture(profilePicPath: String) {
        if (profilePicPath.isEmpty()) {
            profileIcon.setImageResource(R.drawable.settings) // Default
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
                        val decodedBytes = android.util.Base64.decode(imageBase64, android.util.Base64.DEFAULT)
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        profileIcon.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    Log.e("FeedPage", "Error loading profile pic: ${e.message}")
                }
            },
            { error ->
                Log.e("FeedPage", "Network error loading profile pic: ${error.message}")
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun fetchStories() {
        val url = "http://172.15.44.21/socially_app/get_stories.php"

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

                        // Sort stories: Current user's stories first, then by newest
                        storiesList.sortWith(compareBy<Story> {
                            // Put current user's stories first (0), others second (1)
                            if (it.userId.toString() == currentUserId) 0 else 1
                        }.thenByDescending {
                            // Within each group, sort by creation time (newest first)
                            it.createdAt
                        })

                        storyAdapter.notifyDataSetChanged()
                        Log.d("Stories", "Loaded ${storiesList.size} stories")

                    } else {
                        Log.d("Stories", obj.getString("message"))
                    }

                } catch (e: Exception) {
                    Log.e("StoriesError", "Parse error: ${e.message}")
                }
            },
            { error ->
                Log.e("StoriesError", "Network error: ${error.message}")
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun fetchPosts() {
        val url = "http://172.15.44.21/socially_app/get_posts.php?current_user_id=$currentUserId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    Log.d("PostsResponse", "Response: $response")
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        val postsArray = obj.getJSONArray("posts")
                        postsList.clear()

                        for (i in 0 until postsArray.length()) {
                            val postObj = postsArray.getJSONObject(i)

                            val post = Post(
                                id = postObj.getInt("id"),
                                userId = postObj.getInt("user_id"),
                                username = postObj.getString("username"),
                                userProfilePic = postObj.getString("profile_pic"),
                                imagePath = postObj.getString("image_path"),
                                imageBase64 = postObj.getString("image_base64"),
                                caption = postObj.getString("caption"),
                                createdAt = postObj.getString("created_at"),
                                likesCount = postObj.getInt("likes_count"),
                                commentsCount = postObj.getInt("comments_count"),
                                isLikedByCurrentUser = postObj.getBoolean("is_liked_by_current_user")
                            )

                            postsList.add(post)
                        }

                        postAdapter.notifyDataSetChanged()
                        Log.d("Posts", "Loaded ${postsList.size} posts")

                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("PostsError", "Parse error: ${e.message}")
                }
            },
            { error ->
                Toast.makeText(this, "Network Error: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e("PostsError", "Network error: ${error.message}")
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

    private fun handleLikeClick(post: Post) {
        val url = "http://172.15.44.21/socially_app/like_post.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    Log.d("LikePost", "Response: $response")
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        val action = obj.getString("action")
                        val newLikesCount = obj.getInt("likes_count")

                        // Update the post in the list
                        val postIndex = postsList.indexOfFirst { it.id == post.id }
                        if (postIndex != -1) {
                            val updatedPost = post.copy(
                                likesCount = newLikesCount,
                                isLikedByCurrentUser = (action == "liked")
                            )
                            postsList[postIndex] = updatedPost
                            postAdapter.notifyItemChanged(postIndex)
                        }

                        // Optional: Show toast
                        val message = if (action == "liked") "Post liked" else "Post unliked"
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("LikePost", "Parse error: ${e.message}")
                }
            },
            { error ->
                Toast.makeText(this, "Network Error: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("LikePost", "Network error: ${error.message}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["post_id"] = post.id.toString()
                params["user_id"] = currentUserId
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun handleCommentClick(post: Post) {
        val intent = Intent(this, commentspage::class.java)
        intent.putExtra("post_id", post.id)
        startActivity(intent)
    }

    private fun handleShareClick(post: Post) {
        Toast.makeText(this, "Share post", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh posts and stories to get updated counts and new content
        fetchStories()
        fetchPosts()
    }
}