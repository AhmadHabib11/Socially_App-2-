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

    private val client = OkHttpClient()
    private val allUsers = mutableListOf<MyData>()
    private val filteredUsers = mutableListOf<MyData>()
    private var currentUserId = ""

    companion object {
        private const val BASE_URL = "http://192.168.18.109/socially_app/" // Change for real device
    }

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
                        val json = JSONObject(responseData)
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
                                        firstName = userJson.getString("first_name"),
                                        lastName = userJson.getString("last_name"),
                                        email = userJson.getString("email"),
                                        dp = userJson.getString("profile_pic")
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
                                json.getString("message"),
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
        // Create chat on server
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
                        val json = JSONObject(responseData)
                        if (json.getInt("statuscode") == 200) {
                            val chatId = json.getString("chat_id")

                            // Create a Chat object and cache it immediately
                            val newChat = Chat(
                                chatId = chatId,
                                userId = user.id,
                                username = user.name,
                                profileImage = user.dp,
                                lastMessage = "Start a conversation", // Default message
                                timestamp = System.currentTimeMillis() / 1000 // Current timestamp
                            )

                            // Cache the chat locally so it appears immediately
                            val dbHelper = MessageDatabaseHelper(this@SearchUsersActivity)
                            dbHelper.cacheChat(newChat)

                            // Navigate to DM screen
                            val intent = Intent(this@SearchUsersActivity, dmscreen::class.java)
                            intent.putExtra("userId", user.id)
                            intent.putExtra("username", user.name)
                            intent.putExtra("chatId", chatId)
                            intent.putExtra("profileImage", user.dp)
                            startActivity(intent)

                            // Don't finish() here - let user go back to chatlist which should now show the new chat
                        } else {
                            Toast.makeText(
                                this@SearchUsersActivity,
                                json.getString("message"),
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