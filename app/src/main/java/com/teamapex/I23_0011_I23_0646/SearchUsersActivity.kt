package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class SearchUsersActivity : AppCompatActivity() {

    private lateinit var searchBar: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserSearchAdapter
    private lateinit var database: DatabaseReference
    private lateinit var sessionManager: SessionManager

    private val allUsers = mutableListOf<MyData>()
    private val filteredUsers = mutableListOf<MyData>()
    private var currentUserId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_users)

        sessionManager = SessionManager(this)
        currentUserId = sessionManager.getUserId() ?: ""

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        database = FirebaseDatabase.getInstance().reference

        // Setup UI
        searchBar = findViewById(R.id.searchEditText)
        recyclerView = findViewById(R.id.rvUsers)
        val backArrow = findViewById<ImageView>(R.id.backArrow)

        recyclerView.layoutManager = LinearLayoutManager(this)
        userAdapter = UserSearchAdapter(filteredUsers) { user ->
            startChatWithUser(user)
        }
        recyclerView.adapter = userAdapter

        backArrow.setOnClickListener {
            finish()
        }

        // Setup search functionality
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Load all users
        loadUsers()
    }

    private fun loadUsers() {
        database.child("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allUsers.clear()

                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(MyData::class.java)
                        // Don't include current user in the list
                        if (user != null && user.id != currentUserId) {
                            allUsers.add(user)
                        }
                    }

                    // Show all users initially
                    filteredUsers.clear()
                    filteredUsers.addAll(allUsers)
                    userAdapter.notifyDataSetChanged()

                    if (allUsers.isEmpty()) {
                        Toast.makeText(
                            this@SearchUsersActivity,
                            "No other users found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@SearchUsersActivity,
                        "Failed to load users: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun filterUsers(query: String) {
        filteredUsers.clear()

        if (query.isEmpty()) {
            filteredUsers.addAll(allUsers)
        } else {
            val searchQuery = query.lowercase()
            filteredUsers.addAll(
                allUsers.filter { user ->
                    user.name.lowercase().contains(searchQuery) ||
                            user.email.lowercase().contains(searchQuery) ||
                            user.firstName.lowercase().contains(searchQuery) ||
                            user.lastName.lowercase().contains(searchQuery)
                }
            )
        }

        userAdapter.notifyDataSetChanged()
    }

    private fun startChatWithUser(user: MyData) {
        // Generate chat ID
        val chatId = generateChatId(currentUserId, user.id)

        // Get current user data
        database.child("users").child(currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                val currentUser = snapshot.getValue(MyData::class.java)
                if (currentUser != null) {
                    // Create chat entry for both users immediately
                    createChatEntryForBothUsers(currentUser, user, chatId)

                    // Navigate to DM screen
                    val intent = Intent(this, dmscreen::class.java)
                    intent.putExtra("userId", user.id)
                    intent.putExtra("username", user.name)
                    intent.putExtra("chatId", chatId)
                    intent.putExtra("profileImage", user.dp)
                    startActivity(intent)
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createChatEntryForBothUsers(currentUser: MyData, otherUser: MyData, chatId: String) {
        val timestamp = System.currentTimeMillis()

        // Create chat entry for current user
        val currentUserChat = Chat(
            chatId = chatId,
            userId = otherUser.id,
            username = otherUser.name,
            profileImage = otherUser.dp,
            lastMessage = "Start chatting...",
            timestamp = timestamp
        )

        // Create chat entry for other user
        val otherUserChat = Chat(
            chatId = chatId,
            userId = currentUser.id,
            username = currentUser.name,
            profileImage = currentUser.dp,
            lastMessage = "Start chatting...",
            timestamp = timestamp
        )

        // Save both chat entries simultaneously
        val updates = hashMapOf<String, Any>(
            "users/${currentUserId}/chats/${chatId}" to currentUserChat,
            "users/${otherUser.id}/chats/${chatId}" to otherUserChat
        )

        database.updateChildren(updates)
            .addOnSuccessListener {
                // Chat entries created successfully
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to create chat", Toast.LENGTH_SHORT).show()
            }
    }

    private fun generateChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "${userId1}_${userId2}"
        } else {
            "${userId2}_${userId1}"
        }
    }
}