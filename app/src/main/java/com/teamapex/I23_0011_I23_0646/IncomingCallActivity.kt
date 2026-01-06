package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.graphics.BitmapFactory
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class IncomingCallActivity : AppCompatActivity() {

    private lateinit var callerNameText: TextView
    private lateinit var callerImage: ImageView
    private lateinit var acceptBtn: ImageView
    private lateinit var rejectBtn: ImageView

    private val client = OkHttpClient()
    private var ringtone: Ringtone? = null

    private var callId = 0
    private var callerName = ""
    private var callerId = ""
    private var callerPic = ""
    private var callType = ""
    private var channelName = ""

    companion object {
        private const val BASE_URL = "http://172.15.44.21/socially_app/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        callId = intent.getIntExtra("call_id", 0)
        callerName = intent.getStringExtra("caller_name") ?: ""
        callerId = intent.getStringExtra("caller_id") ?: ""
        callerPic = intent.getStringExtra("caller_pic") ?: ""
        callType = intent.getStringExtra("call_type") ?: "audio"
        channelName = intent.getStringExtra("channel_name") ?: ""

        initializeViews()
        playRingtone()
    }

    private fun initializeViews() {
        callerNameText = findViewById(R.id.caller_name)
        callerImage = findViewById(R.id.caller_image)
        acceptBtn = findViewById(R.id.accept_call_btn)
        rejectBtn = findViewById(R.id.reject_call_btn)

        callerNameText.text = callerName

        val callTypeText = findViewById<TextView>(R.id.call_type_text)
        callTypeText.text = if (callType == "video") "Incoming Video Call" else "Incoming Voice Call"

        if (callerPic.isNotEmpty()) {
            loadCallerImage()
        }

        acceptBtn.setOnClickListener {
            acceptCall()
        }

        rejectBtn.setOnClickListener {
            rejectCall()
        }
    }

    private fun loadCallerImage() {
        val url = "${BASE_URL}get_profile_pic.php?path=$callerPic"

        val request = com.android.volley.toolbox.StringRequest(
            com.android.volley.Request.Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response)
                    if (obj.getInt("statuscode") == 200) {
                        val imageBase64 = obj.getString("image")
                        val decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        callerImage.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error -> error.printStackTrace() }
        )

        com.android.volley.toolbox.Volley.newRequestQueue(this).add(request)
    }

    private fun playRingtone() {
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(applicationContext, ringtoneUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopRingtone() {
        ringtone?.stop()
    }

    private fun acceptCall() {
        stopRingtone()

        val request = FormBody.Builder()
            .add("action", "answer")
            .add("call_id", callId.toString())
            .build()

        val httpRequest = Request.Builder()
            .url("${BASE_URL}call_invitation.php")
            .post(request)
            .build()

        client.newCall(httpRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@IncomingCallActivity, "Failed to answer call", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    val intent = Intent(this@IncomingCallActivity, callpage::class.java)
                    intent.putExtra("call_id", callId)
                    intent.putExtra("channel_name", channelName)
                    intent.putExtra("call_type", callType)
                    intent.putExtra("is_caller", false)
                    intent.putExtra("other_user_id", callerId)
                    intent.putExtra("other_username", callerName)
                    intent.putExtra("other_profile_image", callerPic)
                    startActivity(intent)
                    finish()
                }
            }
        })
    }

    private fun rejectCall() {
        stopRingtone()

        val request = FormBody.Builder()
            .add("action", "reject")
            .add("call_id", callId.toString())
            .build()

        val httpRequest = Request.Builder()
            .url("${BASE_URL}call_invitation.php")
            .post(request)
            .build()

        client.newCall(httpRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { finish() }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread { finish() }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingtone()
    }
}