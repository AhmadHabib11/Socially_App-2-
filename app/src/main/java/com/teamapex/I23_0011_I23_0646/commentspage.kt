package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class commentspage : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var commentAdapter: CommentAdapter
    private val commentsList = mutableListOf<Comment>()
    private lateinit var commentInput: EditText
    private lateinit var postButton: Button
    private lateinit var backButton: ImageView
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var postId: String = ""
    private var userName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.commentspage)

        // Get data from intent
        postId = intent.getStringExtra("postId") ?: ""
        userName = intent.getStringExtra("userName") ?: ""

        // Initialize views
        recyclerView = findViewById(R.id.commentsRecyclerView)
        commentInput = findViewById(R.id.commentInput)
        postButton = findViewById(R.id.postCommentButton)
        backButton = findViewById(R.id.backButton)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        commentAdapter = CommentAdapter(commentsList)
        recyclerView.adapter = commentAdapter

        // Load comments
        loadComments()

        // Post comment button
        postButton.setOnClickListener {
            postComment()
        }

        // Back button
        backButton.setOnClickListener {
            val intent = Intent(this, feedpage::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loadComments() {
        database.reference.child("posts").child(postId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val post = snapshot.getValue(Post::class.java)
                    if (post != null && post.comments.isNotEmpty()) {
                        val newComments = mutableListOf<Comment>()

                        for ((commentId, commentData) in post.comments) {
                            try {
                                val commentMap = commentData as? Map<String, Any>
                                if (commentMap != null) {
                                    val userId = commentMap["userId"] as? String ?: ""
                                    val userName = commentMap["userName"] as? String ?: ""
                                    val userDp = commentMap["userDp"] as? String ?: ""
                                    val text = commentMap["text"] as? String ?: ""
                                    val timestamp = commentMap["timestamp"] as? Long ?: 0

                                    android.util.Log.d("CommentsPage", "Loading comment - User: $userName, DP length: ${userDp.length}")

                                    val comment = Comment(
                                        commentId = commentId,
                                        userId = userId,
                                        userName = userName,
                                        userDp = userDp,
                                        text = text,
                                        timestamp = timestamp
                                    )
                                    newComments.add(comment)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("CommentsPage", "Error loading comment: ${e.message}")
                                e.printStackTrace()
                            }
                        }

                        // Sort by timestamp (newest first)
                        newComments.sortByDescending { it.timestamp }
                        commentAdapter.setComments(newComments)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@commentspage, "Error loading comments", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun postComment() {
        val commentText = commentInput.text.toString().trim()

        if (commentText.isEmpty()) {
            Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val commentId = database.reference.push().key ?: return

        // Fetch current user's data (name and DP)
        database.reference.child("users").child(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userData = snapshot.getValue(MyData::class.java)
                    val displayName = userData?.name ?: currentUser.email?.substringBefore("@") ?: "Anonymous"
                    val userDp = userData?.dp ?: ""

                    android.util.Log.d("CommentsPage", "Posting comment - UserID: ${currentUser.uid}, Name: $displayName, DP length: ${userDp.length}")

                    val commentMap = mapOf(
                        "userId" to currentUser.uid,
                        "userName" to displayName,
                        "userDp" to userDp,
                        "text" to commentText,
                        "timestamp" to System.currentTimeMillis()
                    )

                    database.reference.child("posts").child(postId)
                        .child("comments").child(commentId)
                        .setValue(commentMap)
                        .addOnSuccessListener {
                            commentInput.setText("")
                            Toast.makeText(this@commentspage, "Comment posted!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@commentspage, "Failed to post comment", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@commentspage, "Error fetching user data", Toast.LENGTH_SHORT).show()
                }
            })
    }
}