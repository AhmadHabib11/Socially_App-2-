package com.teamapex.I23_0011_I23_0646

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class ScreenshotNotificationManager(private val context: Context) {

    private val client = OkHttpClient()

    companion object {
        private const val TAG = "ScreenshotNotify"
        private const val CHANNEL_ID = "screenshot_alerts"
        private const val CHANNEL_NAME = "Screenshot Alerts"
        private const val NOTIFICATION_ID_BASE = 1000
        private const val BASE_URL = "http://192.168.100.76/socially_app/"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = "Notifications when someone takes a screenshot of your chat"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                enableLights(true)
                lightColor = android.graphics.Color.RED
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            notificationManager.createNotificationChannel(channel)

            Log.d(TAG, "✓ Notification channel created: $CHANNEL_ID")
        }
    }

    /**
     * Send screenshot notification to the other user via server
     */
    fun notifyScreenshotTaken(
        chatId: String,
        screenshotTakerUserId: String,
        screenshotTakerUsername: String,
        otherUserId: String
    ) {
        Log.d(TAG, "📸 Sending screenshot notification")
        Log.d(TAG, "Taker: $screenshotTakerUsername ($screenshotTakerUserId)")
        Log.d(TAG, "Recipient: $otherUserId")
        Log.d(TAG, "Chat: $chatId")

        val formBody = FormBody.Builder()
            .add("action", "screenshot_taken")
            .add("chat_id", chatId)
            .add("screenshot_taker_id", screenshotTakerUserId)
            .add("screenshot_taker_username", screenshotTakerUsername)
            .add("recipient_id", otherUserId)
            .build()

        val request = Request.Builder()
            .url("${BASE_URL}screenshot_notification.php")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "✗ Failed to send screenshot notification: ${e.message}")
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                Log.d(TAG, "Server response: $responseData")

                try {
                    val json = JSONObject(responseData ?: "{}")
                    val statusCode = json.getInt("statuscode")

                    if (statusCode == 200) {
                        Log.d(TAG, "✓ Screenshot notification sent successfully")
                    } else {
                        val message = json.optString("message", "Unknown error")
                        Log.e(TAG, "✗ Failed to send notification ($statusCode): $message")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Error parsing response: ${e.message}")
                    e.printStackTrace()
                }
            }
        })
    }

    /**
     * Show local notification that someone took a screenshot
     */
    fun showScreenshotAlert(username: String, chatId: String = "") {
        Log.d(TAG, "Showing screenshot alert for: $username")

        // Create intent to open the chat when notification is tapped
        val intent = if (chatId.isNotEmpty()) {
            Intent(context, chatlist::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_chat_id", chatId)
            }
        } else {
            Intent(context, chatlist::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            pendingIntentFlags
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Screenshot Alert 📸")
            .setContentText("$username took a screenshot of your chat")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$username captured a screenshot of your conversation. They may have saved sensitive information.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setLights(android.graphics.Color.RED, 1000, 1000)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .build()

        try {
            val notificationId = NOTIFICATION_ID_BASE + username.hashCode()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Check notification permission for Android 13+
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    NotificationManagerCompat.from(context).notify(notificationId, notification)
                    Log.d(TAG, "✓ Screenshot alert shown (ID: $notificationId)")
                } else {
                    Log.w(TAG, "⚠ Notification permission not granted")
                }
            } else {
                NotificationManagerCompat.from(context).notify(notificationId, notification)
                Log.d(TAG, "✓ Screenshot alert shown (ID: $notificationId)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to show notification: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Show in-app alert (when user is active in the app)
     */
    fun showInAppAlert(context: Context, username: String) {
        if (context is android.app.Activity) {
            context.runOnUiThread {
                android.widget.Toast.makeText(
                    context,
                    "📸 $username took a screenshot",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Check for new screenshot notifications from server
     */
    fun checkNotifications(
        userId: String,
        lastCheckTimestamp: Long,
        onNotificationsReceived: (List<ScreenshotNotification>) -> Unit
    ) {
        Log.d(TAG, "Checking for new screenshot notifications")
        Log.d(TAG, "User: $userId, Last check: $lastCheckTimestamp")

        val formBody = FormBody.Builder()
            .add("action", "check_notifications")
            .add("user_id", userId)
            .add("last_check", lastCheckTimestamp.toString())
            .build()

        val request = Request.Builder()
            .url("${BASE_URL}screenshot_notification.php")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "✗ Failed to check notifications: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()

                try {
                    val json = JSONObject(responseData ?: "{}")
                    val statusCode = json.getInt("statuscode")

                    if (statusCode == 200) {
                        val notificationsArray = json.getJSONArray("notifications")
                        val count = json.getInt("count")

                        Log.d(TAG, "✓ Received $count new screenshot notifications")

                        val notifications = mutableListOf<ScreenshotNotification>()

                        for (i in 0 until notificationsArray.length()) {
                            val notifJson = notificationsArray.getJSONObject(i)

                            val notification = ScreenshotNotification(
                                id = notifJson.getInt("id"),
                                chatId = notifJson.getString("chat_id"),
                                screenshotTakerUsername = notifJson.getString("screenshot_taker_username"),
                                timestamp = notifJson.getLong("timestamp")
                            )

                            notifications.add(notification)
                            Log.d(TAG, "  - ${notification.screenshotTakerUsername} at ${notification.timestamp}")
                        }

                        onNotificationsReceived(notifications)
                    } else {
                        Log.e(TAG, "✗ Error checking notifications: ${json.optString("message")}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Error parsing notifications: ${e.message}")
                    e.printStackTrace()
                }
            }
        })
    }

    /**
     * Cancel all screenshot notifications
     */
    fun cancelAllNotifications() {
        try {
            NotificationManagerCompat.from(context).cancelAll()
            Log.d(TAG, "✓ All notifications cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to cancel notifications: ${e.message}")
        }
    }
}

/**
 * Data class for screenshot notifications
 */
data class ScreenshotNotification(
    val id: Int,
    val chatId: String,
    val screenshotTakerUsername: String,
    val timestamp: Long
)