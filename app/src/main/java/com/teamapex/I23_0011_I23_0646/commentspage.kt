package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class commentspage : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var commentAdapter: CommentAdapter
    private val commentsList = mutableListOf<Comment>()
    private lateinit var commentInput: EditText
    private lateinit var postButton: Button
    private lateinit var backButton: ImageView

    private var postId: Int = 0
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.commentspage)

        // Get data from intent
        postId = intent.getIntExtra("post_id", 0)

        // Get current user ID from session
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        currentUserId = sp.getString("userid", "") ?: ""

        // Initialize views
        recyclerView = findViewById(R.id.commentsRecyclerView)
        commentInput = findViewById(R.id.commentInput)
        postButton = findViewById(R.id.postCommentButton)
        backButton = findViewById(R.id.backButton)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        commentAdapter = CommentAdapter(this, commentsList)
        recyclerView.adapter = commentAdapter

        // Load comments
        loadComments()

        // Post comment button
        postButton.setOnClickListener {
            postComment()
        }

        // Back button
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadComments() {
        val url = "http://172.15.44.21/socially_app/get_comments.php?post_id=$postId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    Log.d("CommentsPage", "Response: $response")
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        val commentsArray = obj.getJSONArray("comments")
                        commentsList.clear()

                        for (i in 0 until commentsArray.length()) {
                            val commentObj = commentsArray.getJSONObject(i)

                            val comment = Comment(
                                id = commentObj.getInt("id"),
                                postId = commentObj.getInt("post_id"),
                                userId = commentObj.getInt("user_id"),
                                username = commentObj.getString("username"),
                                userProfilePic = commentObj.getString("profile_pic"),
                                commentText = commentObj.getString("comment_text"),
                                createdAt = commentObj.getString("created_at")
                            )

                            commentsList.add(comment)
                        }

                        commentAdapter.setComments(commentsList)
                        Log.d("CommentsPage", "Loaded ${commentsList.size} comments")

                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("CommentsPage", "Parse error: ${e.message}")
                }
            },
            { error ->
                Toast.makeText(this, "Network Error: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("CommentsPage", "Network error: ${error.message}")
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun postComment() {
        val commentText = commentInput.text.toString().trim()

        if (commentText.isEmpty()) {
            Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://172.15.44.21/socially_app/add_comment.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    Log.d("CommentsPage", "Response: $response")
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        // Get the new comment from response
                        val commentObj = obj.getJSONObject("comment")

                        val newComment = Comment(
                            id = commentObj.getInt("id"),
                            postId = commentObj.getInt("post_id"),
                            userId = commentObj.getInt("user_id"),
                            username = commentObj.getString("username"),
                            userProfilePic = commentObj.getString("profile_pic"),
                            commentText = commentObj.getString("comment_text"),
                            createdAt = commentObj.getString("created_at")
                        )

                        // Add comment to the list at the top
                        commentAdapter.addComment(newComment)

                        // Scroll to top to show new comment
                        recyclerView.smoothScrollToPosition(0)

                        // Clear input
                        commentInput.setText("")

                        Toast.makeText(this, "Comment posted!", Toast.LENGTH_SHORT).show()

                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("CommentsPage", "Parse error: ${e.message}")
                }
            },
            { error ->
                Toast.makeText(this, "Network Error: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("CommentsPage", "Network error: ${error.message}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["post_id"] = postId.toString()
                params["user_id"] = currentUserId
                params["comment_text"] = commentText
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }
}