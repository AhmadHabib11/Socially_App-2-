package com.teamapex.I23_0011_I23_0646

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class OnlineStatusManager(private val context: Context) {

    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private var heartbeatRunnable: Runnable? = null
    private var currentUserId: String = ""

    companion object {
        private const val BASE_URL = "http://192.168.100.76/socially_app/"
        private const val HEARTBEAT_INTERVAL = 15000L // 15 seconds
    }

    fun startTracking(userId: String) {
        currentUserId = userId
        updateStatus(true)
        startHeartbeat()
    }

    fun stopTracking() {
        updateStatus(false)
        stopHeartbeat()
    }

    private fun startHeartbeat() {
        heartbeatRunnable = object : Runnable {
            override fun run() {
                updateStatus(true)
                handler.postDelayed(this, HEARTBEAT_INTERVAL)
            }
        }
        handler.post(heartbeatRunnable!!)
    }

    private fun stopHeartbeat() {
        heartbeatRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun updateStatus(isOnline: Boolean) {
        if (currentUserId.isEmpty()) return

        val formBody = FormBody.Builder()
            .add("user_id", currentUserId)
            .add("is_online", if (isOnline) "1" else "0")
            .build()

        val request = Request.Builder()
            .url("${BASE_URL}update_online_status.php")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OnlineStatus", "Failed to update status: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("OnlineStatus", "Status updated: isOnline=$isOnline")
            }
        })
    }
}