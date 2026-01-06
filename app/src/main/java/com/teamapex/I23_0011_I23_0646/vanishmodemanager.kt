package com.teamapex.I23_0011_I23_0646

import android.content.Context
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class VanishModeManager(private val context: Context) {

    private val client = OkHttpClient()

    companion object {
        private const val BASE_URL = "http://172.15.44.21/socially_app/"
        private const val TAG = "VanishModeManager"
    }

    /**
     * Delete vanish mode messages that have been seen by both users
     * Should be called when user leaves the chat
     */
    fun deleteSeenVanishMessages(
        chatId: String,
        userId: String,
        onComplete: ((success: Boolean, deletedCount: Int) -> Unit)? = null
    ) {
        Log.d(TAG, "=== INITIATING VANISH MESSAGE DELETION ===")
        Log.d(TAG, "Chat ID: $chatId")
        Log.d(TAG, "User ID: $userId")
        Log.d(TAG, "Timestamp: ${System.currentTimeMillis()}")

        val formBody = FormBody.Builder()
            .add("chat_id", chatId)
            .add("user_id", userId)
            .build()

        val request = Request.Builder()
            .url("${BASE_URL}delete_vanish_messages.php")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "✗ Network failure: ${e.message}")
                e.printStackTrace()
                onComplete?.invoke(false, 0)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                Log.d(TAG, "Server response: $responseData")

                try {
                    val json = JSONObject(responseData ?: "{}")
                    val statusCode = json.getInt("statuscode")
                    val deletedCount = json.optInt("deleted_count", 0)
                    val message = json.optString("message", "")

                    when (statusCode) {
                        200 -> {
                            if (deletedCount > 0) {
                                val deletedIds = json.optJSONArray("deleted_ids")
                                Log.d(TAG, "✓✓✓ SUCCESS: Deleted $deletedCount vanish messages")
                                Log.d(TAG, "Deleted IDs: $deletedIds")
                            } else {
                                Log.d(TAG, "✓ No vanish messages to delete (none seen by both users yet)")
                            }
                            onComplete?.invoke(true, deletedCount)
                        }
                        404 -> {
                            Log.w(TAG, "⚠ Chat not found: $chatId")
                            onComplete?.invoke(false, 0)
                        }
                        else -> {
                            Log.e(TAG, "✗ Server error ($statusCode): $message")
                            onComplete?.invoke(false, 0)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Error parsing response: ${e.message}")
                    e.printStackTrace()
                    onComplete?.invoke(false, 0)
                }
            }
        })
    }

    /**
     * Check if a message should be shown based on vanish mode rules
     *
     * Rules:
     * 1. Non-vanish messages: Always show
     * 2. Vanish messages we sent: Always show (until deleted by server)
     * 3. Vanish messages we received: Show only if we haven't seen them yet
     */
    fun shouldShowMessage(message: Message, currentUserId: String): Boolean {
        // Rule 1: Not a vanish message - always show
        if (!message.vanishMode) {
            Log.v(TAG, "Message ${message.id}: NOT vanish mode → SHOW")
            return true
        }

        // Rule 2: We sent this vanish message - always show until server deletes it
        if (message.senderId == currentUserId) {
            Log.v(TAG, "Message ${message.id}: Vanish mode, WE SENT → SHOW")
            return true
        }

        // Rule 3: We received this vanish message - show only if we haven't seen it
        val seenByList = message.seenBy.split(",").filter { it.isNotEmpty() }
        val weSawIt = seenByList.contains(currentUserId)

        if (weSawIt) {
            Log.d(TAG, "Message ${message.id}: Vanish mode, WE SAW IT → HIDE (will be deleted)")
            return false
        } else {
            Log.d(TAG, "Message ${message.id}: Vanish mode, NOT SEEN YET → SHOW")
            return true
        }
    }

    /**
     * Check if both users have seen a vanish message
     * Used for debugging and status checks
     */
    fun isBothUsersSeen(message: Message, user1Id: String, user2Id: String): Boolean {
        if (!message.vanishMode) {
            return false
        }

        val seenByList = message.seenBy.split(",").filter { it.isNotEmpty() }
        val user1Seen = seenByList.contains(user1Id)
        val user2Seen = seenByList.contains(user2Id)

        Log.d(TAG, "Message ${message.id} seen status: user1=$user1Seen, user2=$user2Seen")

        return user1Seen && user2Seen
    }

    /**
     * Get count of vanish messages in a chat
     */
    fun getVanishMessageCount(messages: List<Message>): Int {
        val count = messages.count { it.vanishMode }
        Log.d(TAG, "Vanish message count: $count")
        return count
    }

    /**
     * Get vanish messages that are ready to be deleted
     * (Both users have seen them)
     */
    fun getVanishMessagesReadyForDeletion(
        messages: List<Message>,
        user1Id: String,
        user2Id: String
    ): List<Message> {
        val readyMessages = messages.filter { message ->
            if (!message.vanishMode) {
                false
            } else {
                val seenByList = message.seenBy.split(",").filter { it.isNotEmpty() }
                val user1Seen = seenByList.contains(user1Id)
                val user2Seen = seenByList.contains(user2Id)
                user1Seen && user2Seen
            }
        }

        Log.d(TAG, "Messages ready for deletion: ${readyMessages.size}")
        readyMessages.forEach {
            Log.d(TAG, "  - Message ${it.id} (type: ${it.messageType})")
        }

        return readyMessages
    }

    /**
     * Format vanish mode status for display
     */
    fun getVanishModeStatusText(isEnabled: Boolean): String {
        return if (isEnabled) {
            "Vanish mode is ON 👻"
        } else {
            "Vanish mode is OFF"
        }
    }

    /**
     * Check if vanish mode is safe to use
     * (Network available, proper setup, etc.)
     */
    fun isVanishModeAvailable(): Boolean {
        // Check network connectivity
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as android.net.ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected = networkInfo != null && networkInfo.isConnected

        if (!isConnected) {
            Log.w(TAG, "⚠ Vanish mode unavailable: No network connection")
        }

        return isConnected
    }

    /**
     * Log vanish mode statistics for debugging
     */
    fun logVanishModeStats(
        chatId: String,
        messages: List<Message>,
        currentUserId: String
    ) {
        Log.d(TAG, "=== VANISH MODE STATISTICS ===")
        Log.d(TAG, "Chat ID: $chatId")
        Log.d(TAG, "Total messages: ${messages.size}")

        val vanishMessages = messages.filter { it.vanishMode }
        Log.d(TAG, "Vanish messages: ${vanishMessages.size}")

        val sentVanish = vanishMessages.filter { it.senderId == currentUserId }
        val receivedVanish = vanishMessages.filter { it.senderId != currentUserId }

        Log.d(TAG, "  - Sent by me: ${sentVanish.size}")
        Log.d(TAG, "  - Received: ${receivedVanish.size}")

        val unseenReceived = receivedVanish.filter { message ->
            val seenByList = message.seenBy.split(",").filter { it.isNotEmpty() }
            !seenByList.contains(currentUserId)
        }

        Log.d(TAG, "  - Unseen received: ${unseenReceived.size}")
        Log.d(TAG, "==============================")
    }

    /**
     * Validate vanish message before sending
     */
    fun validateVanishMessage(messageType: String, content: String, mediaPath: String): Boolean {
        // All message types are allowed in vanish mode
        when (messageType) {
            "text" -> {
                if (content.isEmpty()) {
                    Log.w(TAG, "⚠ Cannot send empty vanish message")
                    return false
                }
            }
            "image", "video" -> {
                if (mediaPath.isEmpty()) {
                    Log.w(TAG, "⚠ Cannot send $messageType without media")
                    return false
                }
            }
        }

        Log.d(TAG, "✓ Vanish message validated: type=$messageType")
        return true
    }

    /**
     * Show vanish mode info to user
     */
    fun getVanishModeExplanation(): String {
        return """
            Vanish Mode
            
            Messages will disappear after:
            • Both people have seen them
            • Both people close the chat
            
            Features:
            • Works for text, images, and videos
            • Messages show a 👻 indicator
            • Purple color indicates vanish mode
            • Vanish messages cannot be edited
            
            Note: The other person will know when vanish mode is on.
        """.trimIndent()
    }
}