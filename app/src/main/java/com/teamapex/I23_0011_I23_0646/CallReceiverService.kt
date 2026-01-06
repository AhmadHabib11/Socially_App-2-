package com.teamapex.I23_0011_I23_0646

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class CallReceiverService : Service() {

    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null
    private lateinit var sessionManager: SessionManager

    companion object {
        private const val BASE_URL = "http://172.15.44.21/socially_app/"
        private const val CALL_NOTIFICATION_CHANNEL_ID = "call_notifications"
        private const val CALL_NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
        createNotificationChannel()
        startPolling()
        android.util.Log.d("CallReceiver", "Call receiver service created")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("CallReceiver", "Call receiver service started")
        // Service will be restarted if killed
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CALL_NOTIFICATION_CHANNEL_ID,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incoming calls"
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), null)
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startPolling() {
        pollingRunnable?.let { handler.removeCallbacks(it) }

        pollingRunnable = object : Runnable {
            override fun run() {
                checkForIncomingCalls()
                handler.postDelayed(this, 3000) // Check every 3 seconds
            }
        }
        handler.post(pollingRunnable!!)
        android.util.Log.d("CallReceiver", "Started polling for incoming calls")
    }

    private fun checkForIncomingCalls() {
        val userId = sessionManager.getUserId() ?: return

        val request = FormBody.Builder()
            .add("action", "check_incoming")
            .add("user_id", userId)
            .build()

        val httpRequest = Request.Builder()
            .url("${BASE_URL}call_invitation.php")
            .post(request)
            .build()

        client.newCall(httpRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Silent failure - will retry on next poll
                android.util.Log.d("CallReceiver", "Polling failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                try {
                    val json = JSONObject(responseData ?: "{}")
                    android.util.Log.d("CallReceiver", "Poll response: $json")

                    if (json.getInt("statuscode") == 200 && json.getBoolean("has_call")) {
                        val callData = json.getJSONObject("call")
                        android.util.Log.d("CallReceiver", "Incoming call detected: ${callData.getString("caller_name")}")
                        showIncomingCallNotification(callData)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CallReceiver", "Error parsing call response: ${e.message}")
                }
            }
        })
    }

    private fun showIncomingCallNotification(callData: JSONObject) {
        try {
            val callId = callData.getInt("call_id")
            val callerName = callData.getString("caller_name")
            val callType = callData.getString("call_type")
            val channelName = callData.getString("channel_name")
            val callerId = callData.getString("caller_id")
            val callerPic = callData.optString("caller_pic", "")

            // Answer intent
            val answerIntent = Intent(this, IncomingCallActivity::class.java).apply {
                putExtra("call_id", callId)
                putExtra("caller_name", callerName)
                putExtra("caller_id", callerId)
                putExtra("caller_pic", callerPic)
                putExtra("call_type", callType)
                putExtra("channel_name", channelName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val answerPendingIntent = PendingIntent.getActivity(
                this, callId, answerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Reject broadcast intent
            val rejectIntent = Intent(this, CallActionReceiver::class.java).apply {
                action = "REJECT_CALL"
                putExtra("call_id", callId)
            }

            val rejectPendingIntent = PendingIntent.getBroadcast(
                this, callId + 1000, rejectIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val callTypeText = if (callType == "video") "Video Call" else "Voice Call"

            val notification = NotificationCompat.Builder(this, CALL_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_call)
                .setContentTitle("Incoming $callTypeText")
                .setContentText("$callerName is calling...")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(true)
                .setOngoing(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
                .setFullScreenIntent(answerPendingIntent, true)
                .addAction(R.drawable.ic_call_answer, "Answer", answerPendingIntent)
                .addAction(R.drawable.ic_call_reject, "Reject", rejectPendingIntent)
                .build()

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(CALL_NOTIFICATION_ID, notification)

            android.util.Log.d("CallReceiver", "Incoming call notification shown for $callerName")
        } catch (e: Exception) {
            android.util.Log.e("CallReceiver", "Error showing call notification: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingRunnable?.let { handler.removeCallbacks(it) }
        android.util.Log.d("CallReceiver", "Call receiver service destroyed")
    }
}