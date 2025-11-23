package com.teamapex.I23_0011_I23_0646

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import okhttp3.*
import java.io.IOException

class CallActionReceiver : BroadcastReceiver() {

    private val client = OkHttpClient()

    companion object {
        private const val BASE_URL = "http://192.168.18.35/socially_app/"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "REJECT_CALL") {
            val callId = intent.getIntExtra("call_id", 0)
            rejectCall(callId)
        }
    }

    private fun rejectCall(callId: Int) {
        val request = FormBody.Builder()
            .add("action", "reject")
            .add("call_id", callId.toString())
            .build()

        val httpRequest = Request.Builder()
            .url("${BASE_URL}call_invitation.php")
            .post(request)
            .build()

        client.newCall(httpRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {}
        })
    }
}