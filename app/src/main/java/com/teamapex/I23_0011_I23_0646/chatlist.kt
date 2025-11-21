package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class chatlist : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var dbHelper: MessageDatabaseHelper
    private lateinit var sessionManager: SessionManager

    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null

    private var currentUserId = ""

    companion object {
        private const val BASE_URL = "http://192.168.100.76/socially_app/" // Change for real device
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chatlist)

        sessionManager = SessionManager(this)
        currentUserId = sessionManager.getUserId() ?: ""

        // Check if user is logged in
        if (!sessionManager.isLoggedIn() || currentUserId.isEmpty()) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, login::class.java) // Replace with your login activity
            startActivity(intent)
            finish()
            return
        }

        dbHelper = MessageDatabaseHelper(this)

        setupViews()
        setupRecyclerView()
        loadCachedChats()
        startPollingChats()
    }

    private fun setupViews() {
        val backarrow = findViewById<ImageView>(R.id.backArrow)
        val bottomcam = findViewById<ImageView>(R.id.cambottom)
        val newMessageBtn = findViewById<ImageView>(R.id.plus)

        backarrow.setOnClickListener {
            val intent = Intent(this, feedpage::class.java)
            startActivity(intent)
            finish()
        }

        bottomcam.setOnClickListener {
            val intent = Intent(this, camera::class.java)
            startActivity(intent)
        }

        newMessageBtn?.setOnClickListener {
            // Check if user is logged in before opening search
            if (currentUserId.isNotEmpty()) {
                val intent = Intent(this, SearchUsersActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
                // Optionally redirect to login screen
                val intent = Intent(this, login::class.java) // Replace with your login activity
                startActivity(intent)
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rvChatList) // Changed from rvChats to rvChatList
        val chats = mutableListOf<Chat>()

        chatAdapter = ChatAdapter(chats) { chat ->
            openChat(chat)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = chatAdapter
    }

    private fun loadCachedChats() {
        val cachedChats = dbHelper.getCachedChats()
        cachedChats.forEach { chatAdapter.addChat(it) }
    }

    private fun startPollingChats() {
        pollingRunnable = object : Runnable {
            override fun run() {
                fetchChats()
                handler.postDelayed(this, 5000) // Poll every 5 seconds
            }
        }
        handler.post(pollingRunnable!!)
    }

    private fun fetchChats() {
        val url = "${BASE_URL}get_chats.php?user_id=$currentUserId"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Silently fail for polling
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    try {
                        val json = JSONObject(responseData)
                        if (json.getInt("statuscode") == 200) {
                            val chatsArray = json.getJSONArray("chats")

                            chatAdapter.clearChats()

                            for (i in 0 until chatsArray.length()) {
                                val chatJson = chatsArray.getJSONObject(i)

                                val chat = Chat(
                                    chatId = chatJson.getString("chat_id"),
                                    userId = chatJson.getString("user_id"),
                                    username = chatJson.getString("username"),
                                    profileImage = chatJson.getString("profile_pic"),
                                    lastMessage = chatJson.getString("last_message"),
                                    timestamp = chatJson.getLong("timestamp")
                                )

                                chatAdapter.addChat(chat)
                                dbHelper.cacheChat(chat)
                            }
                        }
                    } catch (e: Exception) {
                        // Silently handle errors for polling
                    }
                }
            }
        })
    }

    private fun openChat(chat: Chat) {
        val intent = Intent(this, dmscreen::class.java)
        intent.putExtra("chatId", chat.chatId)
        intent.putExtra("userId", chat.userId)
        intent.putExtra("username", chat.username)
        intent.putExtra("profileImage", chat.profileImage)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        fetchChats() // Refresh chats when returning to this screen

        // Also trigger a refresh after a short delay to ensure we get the latest data
        handler.postDelayed({
            fetchChats()
        }, 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingRunnable?.let { handler.removeCallbacks(it) }
    }
}