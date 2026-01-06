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
    private lateinit var onlineStatusManager: OnlineStatusManager

    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null

    private var currentUserId = ""

    companion object {
        private const val BASE_URL = "http://172.15.44.21/socially_app/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chatlist)

        sessionManager = SessionManager(this)
        currentUserId = sessionManager.getUserId() ?: ""

        // Check if user is logged in
        if (!sessionManager.isLoggedIn() || currentUserId.isEmpty()) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, login::class.java)
            startActivity(intent)
            finish()
            return
        }

        dbHelper = MessageDatabaseHelper(this)
        onlineStatusManager = OnlineStatusManager(this)

        // Start tracking this user's online status
        onlineStatusManager.startTracking(currentUserId)

        // START CALL RECEIVER SERVICE
        startCallReceiverService()

        setupViews()
        setupRecyclerView()
        refreshChats()
        startPollingChats()
    }

    private fun startCallReceiverService() {
        try {
            val serviceIntent = Intent(this, CallReceiverService::class.java)
            startService(serviceIntent) // Regular service, not foreground
            android.util.Log.d("ChatList", "Call receiver service started")
        } catch (e: Exception) {
            android.util.Log.e("ChatList", "Failed to start call service: ${e.message}")
        }
    }

    // ... rest of your existing chatlist code remains the same
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
            if (currentUserId.isNotEmpty()) {
                val intent = Intent(this, SearchUsersActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, login::class.java)
                startActivity(intent)
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rvChatList)
        val chats = mutableListOf<Chat>()

        chatAdapter = ChatAdapter(chats, currentUserId) { chat ->
            openChat(chat)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = chatAdapter
    }

    private fun refreshChats() {
        android.util.Log.d("ChatList", "Refreshing chats...")

        // Clear existing chats
        chatAdapter.clearChats()

        // Load cached chats first
        loadCachedChats()

        // Then fetch from server
        fetchChats()
    }

    private fun loadCachedChats() {
        val cachedChats = dbHelper.getCachedChats()
        android.util.Log.d("ChatList", "Loading ${cachedChats.size} cached chats")

        cachedChats.forEach { chat ->
            android.util.Log.d("ChatList", "Cached chat: ${chat.username} - ${chat.chatId}")
            chatAdapter.addChat(chat)
        }
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
                android.util.Log.e("ChatList", "Failed to fetch chats: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    try {
                        val json = JSONObject(responseData)
                        if (json.getInt("statuscode") == 200) {
                            val chatsArray = json.getJSONArray("chats")
                            android.util.Log.d("ChatList", "Fetched ${chatsArray.length()} chats from server")

                            for (i in 0 until chatsArray.length()) {
                                val chatJson = chatsArray.getJSONObject(i)

                                val chat = Chat(
                                    chatId = chatJson.getString("chat_id"),
                                    userId = chatJson.getString("user_id"),
                                    username = chatJson.getString("username"),
                                    profileImage = chatJson.optString("profile_pic", ""),
                                    lastMessage = chatJson.optString("last_message", "Start a conversation"),
                                    timestamp = chatJson.getLong("timestamp"),
                                    lastMessageSenderId = chatJson.optString("last_message_sender_id", ""),
                                    deliveryStatus = chatJson.optString("delivery_status", "sent"),
                                    isOnline = chatJson.optBoolean("is_online", false),
                                    lastSeen = chatJson.optLong("last_seen", 0)
                                )

                                android.util.Log.d("ChatList", "Chat ${chat.username}: isOnline=${chat.isOnline}, lastSeen=${chat.lastSeen}")

                                chatAdapter.addChat(chat)
                                dbHelper.cacheChat(chat)
                            }

                            // NEW: Check for deleted chats
                            checkForDeletedChats()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ChatList", "Error parsing chats: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    // ADD THIS NEW METHOD
    private fun checkForDeletedChats() {
        val cachedChatIds = dbHelper.getCachedChatIds()

        if (cachedChatIds.isEmpty()) return

        // Convert to comma-separated string with quotes
        val idsString = cachedChatIds.joinToString(",") { "'$it'" }
        val url = "${BASE_URL}check_deleted_chats.php?user_id=$currentUserId&chat_ids=$idsString"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("ChatList", "Failed to check deleted chats: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    try {
                        val json = JSONObject(responseData ?: "{}")
                        if (json.getInt("statuscode") == 200) {
                            val existingIdsArray = json.getJSONArray("existing_ids")
                            val existingIds = mutableListOf<String>()

                            for (i in 0 until existingIdsArray.length()) {
                                existingIds.add(existingIdsArray.getString(i))
                            }

                            // Find chats that were deleted
                            val deletedChatIds = cachedChatIds.filter { !existingIds.contains(it) }

                            if (deletedChatIds.isNotEmpty()) {
                                android.util.Log.d("ChatList", "Found ${deletedChatIds.size} deleted chats")

                                // Remove from adapter and database
                                deletedChatIds.forEach { chatId ->
                                    chatAdapter.removeChat(chatId)
                                    dbHelper.removeDeletedChat(chatId)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ChatList", "Error checking deleted chats: ${e.message}")
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
        android.util.Log.d("ChatList", "onResume called")

        // Resume online status tracking
        onlineStatusManager.startTracking(currentUserId)

        // Refresh chats when returning to this screen
        refreshChats()

        // Also trigger another refresh after a short delay
        handler.postDelayed({
            fetchChats()
        }, 1000)
    }

    override fun onPause() {
        super.onPause()
        // Don't stop tracking - keep heartbeat running in background
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingRunnable?.let { handler.removeCallbacks(it) }

        // Stop online status tracking when activity is destroyed
        onlineStatusManager.stopTracking()
    }
}