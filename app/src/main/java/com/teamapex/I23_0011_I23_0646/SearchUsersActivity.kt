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
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class SearchUsersActivity : AppCompatActivity() {

    private lateinit var searchBar: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserSearchAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: MessageDatabaseHelper

    private val client = OkHttpClient()
    private val allUsers = mutableListOf<MyData>()
    private val filteredUsers = mutableListOf<MyData>()
    private var currentUserId = ""

    companion object {
        private const val BASE_URL = "http://192.168.18.35/socially_app/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_users)

        sessionManager = SessionManager(this)
        dbHelper = MessageDatabaseHelper(this)
        currentUserId = sessionManager.getUserId() ?: ""

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        setupRecyclerView()
        setupSearchFunctionality()
        loadUsers()
    }

    private fun setupViews() {
        searchBar = findViewById(R.id.searchEditText)
        recyclerView = findViewById(R.id.rvUsers)
        val backArrow = findViewById<ImageView>(R.id.backArrow)

        backArrow.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        userAdapter = UserSearchAdapter(this, filteredUsers) { user ->
            startChatWithUser(user)
        }
        recyclerView.adapter = userAdapter
    }

    private fun setupSearchFunctionality() {
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadUsers() {
        val request = Request.Builder()
            .url("${BASE_URL}get_all_users.php")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@SearchUsersActivity,
                        "Failed to load users: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    try {
                        val json = JSONObject(responseData ?: "{}")
                        if (json.getInt("statuscode") == 200) {
                            val usersArray = json.getJSONArray("users")
                            allUsers.clear()

                            for (i in 0 until usersArray.length()) {
                                val userJson = usersArray.getJSONObject(i)
                                val userId = userJson.getString("id")

                                // Don't include current user
                                if (userId != currentUserId) {
                                    val user = MyData(
                                        id = userId,
                                        name = userJson.getString("username"),
                                        firstName = userJson.optString("first_name", ""),
                                        lastName = userJson.optString("last_name", ""),
                                        email = userJson.optString("email", ""),
                                        dp = userJson.optString("profile_pic", ""),
                                        followStatus = "none"
                                    )
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
                        } else {
                            Toast.makeText(
                                this@SearchUsersActivity,
                                json.optString("message", "Failed to load users"),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@SearchUsersActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
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
        // Show loading state
        Toast.makeText(this, "Creating chat...", Toast.LENGTH_SHORT).show()

        val request = FormBody.Builder()
            .add("user1_id", currentUserId)
            .add("user2_id", user.id)
            .build()

        val httpRequest = Request.Builder()
            .url("${BASE_URL}create_chat.php")
            .post(request)
            .build()

        client.newCall(httpRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@SearchUsersActivity,
                        "Failed to create chat: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    try {
                        val json = JSONObject(responseData ?: "{}")
                        if (json.getInt("statuscode") == 200) {
                            val chatId = json.getString("chat_id")

                            // Create a Chat object and cache it immediately
                            val newChat = Chat(
                                chatId = chatId,
                                userId = user.id,
                                username = user.name,
                                profileImage = user.dp,
                                lastMessage = "Start a conversation",
                                timestamp = System.currentTimeMillis() / 1000
                            )

                            // Cache the chat locally so it appears immediately in chat list
                            dbHelper.cacheChat(newChat)

                            // Navigate to DM screen
                            val intent = Intent(this@SearchUsersActivity, dmscreen::class.java)
                            intent.putExtra("chatId", chatId)
                            intent.putExtra("userId", user.id)
                            intent.putExtra("username", user.name)
                            intent.putExtra("profileImage", user.dp)
                            startActivity(intent)

                            // Finish this activity so when user goes back, they return to chatlist
                            finish()
                        } else {
                            Toast.makeText(
                                this@SearchUsersActivity,
                                json.optString("message", "Failed to create chat"),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@SearchUsersActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }
}