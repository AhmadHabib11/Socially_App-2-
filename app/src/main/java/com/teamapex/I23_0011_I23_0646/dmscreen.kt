package com.teamapex.I23_0011_I23_0646

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class dmscreen : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageBox: EditText
    private lateinit var btnSend: ImageView
    private lateinit var galleryBtn: ImageView
    private lateinit var cameraBtn: ImageView
    private lateinit var videoBtn: ImageView
    private lateinit var vanishModeBtn: ImageView
    private lateinit var usernameTextView: TextView

    private lateinit var dbHelper: MessageDatabaseHelper
    private lateinit var sessionManager: SessionManager

    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null

    private var chatId = ""
    private var otherUserId = ""
    private var otherUsername = ""
    private var otherUserProfileImage = ""
    private var currentUserId = ""
    private var vanishMode = false
    private var lastTimestamp = 0L

    companion object {
        private const val PICK_IMAGE = 1
        private const val CAPTURE_IMAGE = 2
        private const val PICK_VIDEO = 3
        private const val CAPTURE_VIDEO = 4
        private const val BASE_URL = "http://192.168.18.35/socially_app/"
        private const val MAX_VIDEO_SIZE = 10 * 1024 * 1024 // 10MB limit
        private const val PERMISSION_REQ_ID = 22
        private val REQUESTED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dmscreen)

        sessionManager = SessionManager(this)
        currentUserId = sessionManager.getUserId() ?: ""

        dbHelper = MessageDatabaseHelper(this)

        chatId = intent.getStringExtra("chatId") ?: ""
        otherUserId = intent.getStringExtra("userId") ?: ""
        otherUsername = intent.getStringExtra("username") ?: ""
        otherUserProfileImage = intent.getStringExtra("profileImage") ?: ""

        initializeViews()
        setupRecyclerView()
        loadCachedMessages()
        markExistingMessagesAsSeen()
        startPollingMessages()
        setupListeners()
    }

    private fun markExistingMessagesAsSeen() {
        val cachedMessages = dbHelper.getCachedMessages(chatId)
        cachedMessages.forEach { message ->
            if (message.senderId != currentUserId && !message.seenBy.contains(currentUserId)) {
                markMessageAsSeen(message.id)
            }
        }
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.rvMessages)
        messageBox = findViewById(R.id.messageBox)
        btnSend = findViewById(R.id.btnSend)
        galleryBtn = findViewById(R.id.gallery)
        cameraBtn = findViewById(R.id.camerabtn)
        videoBtn = findViewById(R.id.videobtn) // Add this to your layout
        vanishModeBtn = findViewById(R.id.vanishModeBtn)
        usernameTextView = findViewById(R.id.username)

        usernameTextView.text = otherUsername

        val profileImageView = findViewById<ImageView>(R.id.dp)

        profileImageView.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
        profileImageView.clipToOutline = true

        if (otherUserProfileImage.isNotEmpty()) {
            android.util.Log.d("dmscreen", "Profile pic path: $otherUserProfileImage")
            loadProfilePicture(otherUserProfileImage, profileImageView)
        } else {
            profileImageView.setImageResource(R.drawable.profilepic)
        }

        findViewById<ImageView>(R.id.backArrow).setOnClickListener {
            finish()
        }

        val voiceCallBtn = findViewById<ImageView>(R.id.voicecall)
        val videoCallBtn = findViewById<ImageView>(R.id.vidcall)

        voiceCallBtn.setOnClickListener {
            if (checkCallPermissions()) {
                initiateCall("audio")
            } else {
                requestCallPermissions()
            }
        }

        videoCallBtn.setOnClickListener {
            if (checkCallPermissions()) {
                initiateCall("video")
            } else {
                requestCallPermissions()
            }
        }

    }

    private fun checkCallPermissions(): Boolean {
        return REQUESTED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestCallPermissions() {
        ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions required for calls", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initiateCall(callType: String) {
        val channelName = "call_${chatId}_${System.currentTimeMillis()}"

        val request = FormBody.Builder()
            .add("action", "initiate")
            .add("caller_id", currentUserId)
            .add("receiver_id", otherUserId)
            .add("call_type", callType)
            .add("channel_name", channelName)
            .build()

        val httpRequest = Request.Builder()
            .url("${BASE_URL}call_invitation.php")
            .post(request)
            .build()

        client.newCall(httpRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@dmscreen, "Failed to initiate call", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    try {
                        val json = JSONObject(responseData ?: "{}")
                        if (json.getInt("statuscode") == 200) {
                            val callId = json.getInt("call_id")

                            val intent = Intent(this@dmscreen, callpage::class.java)
                            intent.putExtra("call_id", callId)
                            intent.putExtra("channel_name", channelName)
                            intent.putExtra("call_type", callType)
                            intent.putExtra("is_caller", true)
                            intent.putExtra("other_user_id", otherUserId)
                            intent.putExtra("other_username", otherUsername)
                            intent.putExtra("other_profile_image", otherUserProfileImage)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@dmscreen, "Failed to start call", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@dmscreen, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }


    private fun loadProfilePicture(profilePicPath: String, imageView: ImageView) {
        if (profilePicPath.isEmpty()) {
            imageView.setImageResource(R.drawable.story1)
            return
        }

        val url = "${BASE_URL}get_profile_pic.php?path=$profilePicPath"

        android.util.Log.d("dmscreen", "Loading profile pic from: $url")

        val request = com.android.volley.toolbox.StringRequest(
            com.android.volley.Request.Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response)
                    if (obj.getInt("statuscode") == 200) {
                        val imageBase64 = obj.getString("image")
                        val decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        imageView.setImageBitmap(bitmap)
                        android.util.Log.d("dmscreen", "Profile pic loaded successfully")
                    } else {
                        android.util.Log.e("dmscreen", "Error: ${obj.optString("message")}")
                        imageView.setImageResource(R.drawable.story1)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("dmscreen", "Error loading profile pic: ${e.message}")
                    imageView.setImageResource(R.drawable.story1)
                }
            },
            { error ->
                android.util.Log.e("dmscreen", "Network error: ${error.message}")
                imageView.setImageResource(R.drawable.story1)
            }
        )

        com.android.volley.toolbox.Volley.newRequestQueue(this).add(request)
    }

    private fun setupRecyclerView() {
        val messages = mutableListOf<Message>()
        messageAdapter = MessageAdapter(messages, currentUserId) { message ->
            showMessageOptions(message)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = messageAdapter
    }

    private fun loadCachedMessages() {
        val cachedMessages = dbHelper.getCachedMessages(chatId)

        android.util.Log.e("CACHE_CHECK", "=== Loading cached messages ===")
        android.util.Log.e("CACHE_CHECK", "Total messages: ${cachedMessages.size}")

        cachedMessages.forEach { msg ->
            android.util.Log.e("CACHE_CHECK", "Message ID=${msg.id}, Type=${msg.messageType}, VanishMode=${msg.vanishMode}")
            if (msg.messageType == "image" || msg.messageType == "video") {
                android.util.Log.e("CACHE_CHECK", "${msg.messageType.uppercase()} - mediaPath is empty: ${msg.mediaPath.isEmpty()}, starts with data: = ${msg.mediaPath.startsWith("data:")}")
            }
            messageAdapter.addMessage(msg)
        }

        if (messageAdapter.itemCount > 0) {
            recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
        }
    }

    private fun setupListeners() {
        messageBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().trim().isNotEmpty()) {
                    btnSend.setImageResource(R.drawable.send)
                } else {
                    btnSend.setImageResource(R.drawable.mic)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSend.setOnClickListener {
            val text = messageBox.text.toString().trim()
            if (text.isNotEmpty()) {
                sendTextMessage(text)
                messageBox.text.clear()
            }
        }

        galleryBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE)
        }

        cameraBtn.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, CAPTURE_IMAGE)
        }

        // Video button listener
        videoBtn.setOnClickListener {
            showVideoOptions()
        }

        vanishModeBtn.setOnClickListener {
            toggleVanishMode()
        }

        updateVanishModeButton()
    }

    private fun showVideoOptions() {
        val options = arrayOf("Record Video", "Choose from Gallery", "Cancel")

        AlertDialog.Builder(this)
            .setTitle("Send Video")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60) // 60 seconds max
                        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0) // 0 = low quality for smaller size
                        startActivityForResult(intent, CAPTURE_VIDEO)
                    }
                    1 -> {
                        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                        startActivityForResult(intent, PICK_VIDEO)
                    }
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun toggleVanishMode() {
        if (!vanishMode) {
            AlertDialog.Builder(this)
                .setTitle("Enable Vanish Mode?")
                .setMessage("Messages will disappear after both users have seen them and closed the chat.")
                .setPositiveButton("Enable") { _, _ ->
                    vanishMode = true
                    updateVanishModeButton()
                    Toast.makeText(this, "Vanish mode enabled 👻", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            vanishMode = false
            updateVanishModeButton()
            Toast.makeText(this, "Vanish mode disabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateVanishModeButton() {
        if (vanishMode) {
            vanishModeBtn.setImageResource(R.drawable.ic_vanish_on)
            vanishModeBtn.alpha = 1.0f
        } else {
            vanishModeBtn.setImageResource(R.drawable.ic_vanish_off)
            vanishModeBtn.alpha = 0.5f
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                PICK_IMAGE -> {
                    val imageUri = data.data
                    imageUri?.let { sendImageMessage(it) }
                }
                CAPTURE_IMAGE -> {
                    val imageBitmap = data.extras?.get("data") as? Bitmap
                    imageBitmap?.let { sendCapturedImage(it) }
                }
                PICK_VIDEO -> {
                    val videoUri = data.data
                    videoUri?.let { sendVideoMessage(it) }
                }
                CAPTURE_VIDEO -> {
                    val videoUri = data.data
                    videoUri?.let { sendVideoMessage(it) }
                }
            }
        }
    }

    private fun sendVideoMessage(videoUri: Uri) {
        try {
            // Check video size
            val inputStream = contentResolver.openInputStream(videoUri)
            val videoSize = inputStream?.available() ?: 0
            inputStream?.close()

            if (videoSize > MAX_VIDEO_SIZE) {
                Toast.makeText(this, "Video is too large. Maximum size is 10MB", Toast.LENGTH_LONG).show()
                return
            }

            // Show progress
            Toast.makeText(this, "Uploading video...", Toast.LENGTH_SHORT).show()

            // Read video data
            val videoData = readVideoData(videoUri)
            val base64Video = Base64.encodeToString(videoData, Base64.DEFAULT)

            val request = FormBody.Builder()
                .add("chat_id", chatId)
                .add("sender_id", currentUserId)
                .add("message_type", "video")
                .add("media_data", base64Video)
                .add("vanish_mode", if (vanishMode) "1" else "0")
                .build()

            val httpRequest = Request.Builder()
                .url("${BASE_URL}send_message.php")
                .post(request)
                .build()

            client.newCall(httpRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@dmscreen, "Failed to send video", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseData = response.body?.string()
                    runOnUiThread {
                        try {
                            val json = JSONObject(responseData ?: "{}")
                            if (json.getInt("statuscode") == 200) {
                                val messageId = json.getInt("message_id")
                                val timestamp = json.getLong("timestamp")
                                val deliveryStatus = json.optString("delivery_status", "sent")

                                val message = Message(
                                    id = messageId,
                                    chatId = chatId,
                                    senderId = currentUserId,
                                    messageType = "video",
                                    mediaPath = "data:video/mp4;base64,$base64Video",
                                    timestamp = timestamp,
                                    vanishMode = vanishMode,
                                    deliveryStatus = deliveryStatus
                                )

                                messageAdapter.addMessage(message)
                                dbHelper.cacheMessage(message)

                                val updatedChat = Chat(
                                    chatId = chatId,
                                    userId = otherUserId,
                                    username = otherUsername,
                                    profileImage = otherUserProfileImage,
                                    lastMessage = "Video",
                                    timestamp = timestamp,
                                    lastMessageSenderId = currentUserId,
                                    deliveryStatus = deliveryStatus
                                )
                                dbHelper.cacheChat(updatedChat)

                                recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                                Toast.makeText(this@dmscreen, "Video sent!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@dmscreen, "Failed to send video", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@dmscreen, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading video: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readVideoData(videoUri: Uri): ByteArray {
        val inputStream: InputStream? = contentResolver.openInputStream(videoUri)
        val byteBuffer = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var len: Int

        inputStream?.use { input ->
            while (input.read(buffer).also { len = it } != -1) {
                byteBuffer.write(buffer, 0, len)
            }
        }

        return byteBuffer.toByteArray()
    }

    private fun sendCapturedImage(bitmap: Bitmap) {
        try {
            val resizedBitmap = resizeBitmap(bitmap, 800)
            val base64Image = bitmapToBase64(resizedBitmap)

            val request = FormBody.Builder()
                .add("chat_id", chatId)
                .add("sender_id", currentUserId)
                .add("message_type", "image")
                .add("media_data", base64Image)
                .add("vanish_mode", if (vanishMode) "1" else "0")
                .build()

            val httpRequest = Request.Builder()
                .url("${BASE_URL}send_message.php")
                .post(request)
                .build()

            client.newCall(httpRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@dmscreen, "Failed to send image", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseData = response.body?.string()
                    runOnUiThread {
                        try {
                            val json = JSONObject(responseData ?: "{}")
                            if (json.getInt("statuscode") == 200) {
                                val messageId = json.getInt("message_id")
                                val timestamp = json.getLong("timestamp")
                                val deliveryStatus = json.optString("delivery_status", "sent")

                                val message = Message(
                                    id = messageId,
                                    chatId = chatId,
                                    senderId = currentUserId,
                                    messageType = "image",
                                    mediaPath = "data:image/jpeg;base64,$base64Image",
                                    timestamp = timestamp,
                                    vanishMode = vanishMode,
                                    deliveryStatus = deliveryStatus
                                )

                                messageAdapter.addMessage(message)
                                dbHelper.cacheMessage(message)

                                val updatedChat = Chat(
                                    chatId = chatId,
                                    userId = otherUserId,
                                    username = otherUsername,
                                    profileImage = otherUserProfileImage,
                                    lastMessage = "Image",
                                    timestamp = timestamp,
                                    lastMessageSenderId = currentUserId,
                                    deliveryStatus = deliveryStatus
                                )
                                dbHelper.cacheChat(updatedChat)

                                recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@dmscreen, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendTextMessage(text: String) {
        val request = FormBody.Builder()
            .add("chat_id", chatId)
            .add("sender_id", currentUserId)
            .add("message_type", "text")
            .add("content", text)
            .add("vanish_mode", if (vanishMode) "1" else "0")
            .build()

        val httpRequest = Request.Builder()
            .url("${BASE_URL}send_message.php")
            .post(request)
            .build()

        client.newCall(httpRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@dmscreen, "Failed to send message", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    try {
                        val json = JSONObject(responseData ?: "{}")
                        if (json.getInt("statuscode") == 200) {
                            val messageId = json.getInt("message_id")
                            val timestamp = json.getLong("timestamp")
                            val deliveryStatus = json.optString("delivery_status", "sent")

                            val message = Message(
                                id = messageId,
                                chatId = chatId,
                                senderId = currentUserId,
                                messageType = "text",
                                content = text,
                                timestamp = timestamp,
                                vanishMode = vanishMode,
                                deliveryStatus = deliveryStatus
                            )

                            messageAdapter.addMessage(message)
                            dbHelper.cacheMessage(message)

                            val updatedChat = Chat(
                                chatId = chatId,
                                userId = otherUserId,
                                username = otherUsername,
                                profileImage = otherUserProfileImage,
                                lastMessage = text,
                                timestamp = timestamp,
                                lastMessageSenderId = currentUserId,
                                deliveryStatus = deliveryStatus
                            )
                            dbHelper.cacheChat(updatedChat)

                            recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                        } else {
                            Toast.makeText(this@dmscreen, json.optString("message", "Error sending message"), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@dmscreen, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun sendImageMessage(imageUri: Uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            val resizedBitmap = resizeBitmap(bitmap, 800)
            val base64Image = bitmapToBase64(resizedBitmap)

            val request = FormBody.Builder()
                .add("chat_id", chatId)
                .add("sender_id", currentUserId)
                .add("message_type", "image")
                .add("media_data", base64Image)
                .add("vanish_mode", if (vanishMode) "1" else "0")
                .build()

            val httpRequest = Request.Builder()
                .url("${BASE_URL}send_message.php")
                .post(request)
                .build()

            client.newCall(httpRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@dmscreen, "Failed to send image", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseData = response.body?.string()
                    runOnUiThread {
                        try {
                            val json = JSONObject(responseData ?: "{}")
                            if (json.getInt("statuscode") == 200) {
                                val messageId = json.getInt("message_id")
                                val timestamp = json.getLong("timestamp")
                                val deliveryStatus = json.optString("delivery_status", "sent")

                                val message = Message(
                                    id = messageId,
                                    chatId = chatId,
                                    senderId = currentUserId,
                                    messageType = "image",
                                    mediaPath = "data:image/jpeg;base64,$base64Image",
                                    timestamp = timestamp,
                                    vanishMode = vanishMode,
                                    deliveryStatus = deliveryStatus
                                )

                                messageAdapter.addMessage(message)
                                dbHelper.cacheMessage(message)

                                val updatedChat = Chat(
                                    chatId = chatId,
                                    userId = otherUserId,
                                    username = otherUsername,
                                    profileImage = otherUserProfileImage,
                                    lastMessage = "Image",
                                    timestamp = timestamp,
                                    lastMessageSenderId = currentUserId,
                                    deliveryStatus = deliveryStatus
                                )
                                dbHelper.cacheChat(updatedChat)

                                recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@dmscreen, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startPollingMessages() {
        pollingRunnable = object : Runnable {
            override fun run() {
                fetchNewMessages()
                handler.postDelayed(this, 3000)
            }
        }
        handler.post(pollingRunnable!!)
    }

    // Replace the entire fetchNewMessages() method in dmscreen.kt with this:

    private fun fetchNewMessages() {
        val url = "${BASE_URL}get_messages.php?chat_id=$chatId&user_id=$currentUserId&last_timestamp=$lastTimestamp"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Silent failure - will retry on next poll
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    try {
                        val json = JSONObject(responseData ?: "{}")
                        if (json.getInt("statuscode") == 200) {
                            val messagesArray = json.getJSONArray("messages")

                            for (i in 0 until messagesArray.length()) {
                                val msgJson = messagesArray.getJSONObject(i)

                                // Parse shared_post_id if present
                                val sharedPostId = if (msgJson.has("shared_post_id") && !msgJson.isNull("shared_post_id")) {
                                    msgJson.getInt("shared_post_id")
                                } else {
                                    null
                                }

                                // Get message type - ensure it's exactly what was sent
                                val messageType = msgJson.getString("message_type")

                                val message = Message(
                                    id = msgJson.getInt("id"),
                                    chatId = chatId,
                                    senderId = msgJson.getString("sender_id"),
                                    messageType = messageType, // Use the exact type from server
                                    content = msgJson.optString("content", ""),
                                    mediaPath = msgJson.optString("media_path", ""),
                                    timestamp = msgJson.getLong("timestamp"),
                                    vanishMode = msgJson.getInt("vanish_mode") == 1,
                                    isDeleted = msgJson.getInt("is_deleted") == 1,
                                    isEdited = msgJson.getInt("is_edited") == 1,
                                    seenBy = msgJson.optString("seen_by", ""),
                                    deliveryStatus = msgJson.optString("delivery_status", "sent"),
                                    sharedPostId = sharedPostId
                                )

                                // Only add if message type is valid
                                messageAdapter.addMessage(message)
                                dbHelper.cacheMessage(message)

                                if (message.timestamp > lastTimestamp) {
                                    lastTimestamp = message.timestamp
                                }

                                // Mark message as seen if not from current user
                                if (message.senderId != currentUserId) {
                                    markMessageAsSeen(message.id)
                                }
                            }

                            // Scroll to bottom if new messages arrived
                            if (messagesArray.length() > 0) {
                                recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("dmscreen", "Error fetching messages: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun markMessageAsSeen(messageId: Int) {
        val request = FormBody.Builder()
            .add("message_id", messageId.toString())
            .add("user_id", currentUserId)
            .build()

        val httpRequest = Request.Builder()
            .url("${BASE_URL}mark_message_seen.php")
            .post(request)
            .build()

        client.newCall(httpRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {}
        })
    }

    private fun showMessageOptions(message: Message) {
        if (message.senderId != currentUserId) return

        if (message.isDeleted) {
            Toast.makeText(this, "This message has been deleted", Toast.LENGTH_SHORT).show()
            return
        }

        val currentTime = System.currentTimeMillis() / 1000
        val timeDiff = currentTime - message.timestamp
        val canEditDelete = timeDiff <= 300

        if (!canEditDelete) {
            AlertDialog.Builder(this)
                .setTitle("Time Limit Exceeded")
                .setMessage("Messages can only be edited or deleted within 5 minutes of sending.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val optionsList = mutableListOf<String>()

        if (message.messageType == "text") {
            optionsList.add("Edit")
        }
        optionsList.add("Delete")
        optionsList.add("Cancel")

        val options = optionsList.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Message Options")
            .setItems(options) { dialog, which ->
                when (options[which]) {
                    "Edit" -> showEditDialog(message)
                    "Delete" -> confirmDeleteMessage(message.id)
                    "Cancel" -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun confirmDeleteMessage(messageId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteMessage(messageId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(message: Message) {
        val editText = EditText(this)
        editText.setText(message.content)
        editText.setSelection(message.content.length)

        AlertDialog.Builder(this)
            .setTitle("Edit Message")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newContent = editText.text.toString().trim()
                if (newContent.isEmpty()) {
                    Toast.makeText(this@dmscreen, "Message cannot be empty", Toast.LENGTH_SHORT).show()
                } else if (newContent == message.content) {
                    Toast.makeText(this@dmscreen, "No changes made", Toast.LENGTH_SHORT).show()
                } else {
                    editMessage(message.id, newContent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()

        editText.requestFocus()
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        editText.postDelayed({
            imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    private fun editMessage(messageId: Int, newContent: String) {
        val request = FormBody.Builder()
            .add("message_id", messageId.toString())
            .add("user_id", currentUserId)
            .add("new_content", newContent)
            .build()

        val httpRequest = Request.Builder()
            .url("${BASE_URL}edit_message.php")
            .post(request)
            .build()

        client.newCall(httpRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@dmscreen, "Failed to edit message", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    try {
                        val json = JSONObject(responseData ?: "{}")
                        if (json.getInt("statuscode") == 200) {
                            messageAdapter.updateMessage(messageId, newContent)
                            dbHelper.updateMessageContent(messageId, newContent)
                            Toast.makeText(this@dmscreen, "Message edited", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@dmscreen, json.optString("message", "Failed to edit"), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@dmscreen, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun deleteMessage(messageId: Int) {
        val request = FormBody.Builder()
            .add("message_id", messageId.toString())
            .add("user_id", currentUserId)
            .build()

        val httpRequest = Request.Builder()
            .url("${BASE_URL}delete_message.php")
            .post(request)
            .build()

        client.newCall(httpRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@dmscreen, "Failed to delete message", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    try {
                        val json = JSONObject(responseData ?: "{}")
                        if (json.getInt("statuscode") == 200) {
                            messageAdapter.deleteMessage(messageId)
                            dbHelper.deleteMessage(messageId)
                            Toast.makeText(this@dmscreen, "Message deleted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@dmscreen, json.optString("message", "Failed to delete"), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@dmscreen, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        var width = bitmap.width
        var height = bitmap.height

        if (width > height) {
            if (width > maxSize) {
                height = (height * maxSize.toFloat() / width).toInt()
                width = maxSize
            }
        } else {
            if (height > maxSize) {
                width = (width * maxSize.toFloat() / height).toInt()
                height = maxSize
            }
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingRunnable?.let { handler.removeCallbacks(it) }
    }
}