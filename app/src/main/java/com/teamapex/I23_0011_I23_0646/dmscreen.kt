package com.teamapex.I23_0011_I23_0646

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class dmscreen : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageBox: EditText
    private lateinit var btnSend: ImageView
    private lateinit var galleryBtn: ImageView
    private lateinit var cameraBtn: ImageView
    private lateinit var usernameTextView: TextView

    private lateinit var dbHelper: MessageDatabaseHelper
    private lateinit var sessionManager: SessionManager

    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null

    private var chatId = ""
    private var otherUserId = ""
    private var otherUsername = ""
    private var currentUserId = ""
    private var vanishMode = false
    private var lastTimestamp = 0L

    companion object {
        private const val PICK_IMAGE = 1
        private const val BASE_URL = "http://192.168.18.109/socially_app/" // Change for real device
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dmscreen)

        sessionManager = SessionManager(this)
        currentUserId = sessionManager.getUserId() ?: ""

        dbHelper = MessageDatabaseHelper(this)

        // Get intent extras
        chatId = intent.getStringExtra("chatId") ?: ""
        otherUserId = intent.getStringExtra("userId") ?: ""
        otherUsername = intent.getStringExtra("username") ?: ""

        initializeViews()
        setupRecyclerView()
        loadCachedMessages()
        startPollingMessages()
        setupListeners()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.rvMessages)
        messageBox = findViewById(R.id.messageBox)
        btnSend = findViewById(R.id.btnSend)
        galleryBtn = findViewById(R.id.gallery)
        cameraBtn = findViewById(R.id.camerabtn)
        usernameTextView = findViewById(R.id.username)

        // Set the username
        usernameTextView.text = otherUsername

        findViewById<ImageView>(R.id.backArrow).setOnClickListener {
            val intent = Intent(this, chatlist::class.java)
            startActivity(intent)

        }
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
        cachedMessages.forEach { messageAdapter.addMessage(it) }
        if (messageAdapter.itemCount > 0) {
            recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
        }
    }

    private fun setupListeners() {
        // Change send icon based on text input
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
            showVanishModeDialog()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            imageUri?.let { sendImageMessage(it) }
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

                            val message = Message(
                                id = messageId,
                                chatId = chatId,
                                senderId = currentUserId,
                                messageType = "text",
                                content = text,
                                timestamp = timestamp,
                                vanishMode = vanishMode
                            )

                            messageAdapter.addMessage(message)
                            dbHelper.cacheMessage(message)
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

                                val message = Message(
                                    id = messageId,
                                    chatId = chatId,
                                    senderId = currentUserId,
                                    messageType = "image",
                                    mediaPath = "data:image/jpeg;base64,$base64Image",
                                    timestamp = timestamp,
                                    vanishMode = vanishMode
                                )

                                messageAdapter.addMessage(message)
                                dbHelper.cacheMessage(message)
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
                handler.postDelayed(this, 3000) // Poll every 3 seconds
            }
        }
        handler.post(pollingRunnable!!)
    }

    private fun fetchNewMessages() {
        val url = "${BASE_URL}get_messages.php?chat_id=$chatId&user_id=$currentUserId&last_timestamp=$lastTimestamp"

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
                        val json = JSONObject(responseData ?: "{}")
                        if (json.getInt("statuscode") == 200) {
                            val messagesArray = json.getJSONArray("messages")

                            for (i in 0 until messagesArray.length()) {
                                val msgJson = messagesArray.getJSONObject(i)

                                val message = Message(
                                    id = msgJson.getInt("id"),
                                    chatId = chatId,
                                    senderId = msgJson.getString("sender_id"),
                                    messageType = msgJson.getString("message_type"),
                                    content = msgJson.optString("content", ""),
                                    mediaPath = msgJson.optString("media_path", ""),
                                    timestamp = msgJson.getLong("timestamp"),
                                    vanishMode = msgJson.getInt("vanish_mode") == 1,
                                    isDeleted = msgJson.getInt("is_deleted") == 1,
                                    isEdited = msgJson.getInt("is_edited") == 1,
                                    seenBy = msgJson.optString("seen_by", "")
                                )

                                messageAdapter.addMessage(message)
                                dbHelper.cacheMessage(message)

                                if (message.timestamp > lastTimestamp) {
                                    lastTimestamp = message.timestamp
                                }
                            }

                            if (messagesArray.length() > 0) {
                                recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                            }
                        }
                    } catch (e: Exception) {
                        // Silently handle errors for polling
                    }
                }
            }
        })
    }

    private fun showMessageOptions(message: Message) {
        if (message.senderId != currentUserId) return

        val currentTime = System.currentTimeMillis() / 1000
        val timeDiff = currentTime - message.timestamp
        val canEditDelete = timeDiff <= 300 // 5 minutes

        val options = if (canEditDelete) {
            arrayOf("Edit", "Delete", "Cancel")
        } else {
            arrayOf("Cannot edit/delete (>5 min)", "Cancel")
        }

        AlertDialog.Builder(this)
            .setTitle("Message Options")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> if (canEditDelete && message.messageType == "text") showEditDialog(message)
                    1 -> if (canEditDelete) deleteMessage(message.id)
                }
            }
            .show()
    }

    private fun showEditDialog(message: Message) {
        val editText = EditText(this)
        editText.setText(message.content)

        AlertDialog.Builder(this)
            .setTitle("Edit Message")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newContent = editText.text.toString().trim()
                if (newContent.isNotEmpty()) {
                    editMessage(message.id, newContent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    private fun showVanishModeDialog() {
        AlertDialog.Builder(this)
            .setTitle("Vanish Mode")
            .setMessage("Enable vanish mode? Messages will disappear once seen and chat is closed.")
            .setPositiveButton("Enable") { _, _ ->
                vanishMode = true
                Toast.makeText(this, "Vanish mode enabled", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { _, _ ->
                vanishMode = false
            }
            .show()
    }

    private fun deleteVanishMessages() {
        val request = FormBody.Builder()
            .add("chat_id", chatId)
            .add("user_id", currentUserId)
            .build()

        val httpRequest = Request.Builder()
            .url("${BASE_URL}delete_vanish_messages.php")
            .post(request)
            .build()

        client.newCall(httpRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    dbHelper.deleteVanishMessages(chatId, currentUserId)
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

    override fun onDestroy() {
        super.onDestroy()
        pollingRunnable?.let { handler.removeCallbacks(it) }
        deleteVanishMessages() // Delete vanish mode messages when leaving chat
    }
}