package com.teamapex.I23_0011_I23_0646

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class callpage : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var callName: TextView
    private lateinit var callTime: TextView
    private lateinit var callEndBtn: ImageView
    private lateinit var muteBtn: ImageView
    private lateinit var localVideoContainer: FrameLayout
    private lateinit var remoteVideoContainer: FrameLayout

    private var agoraEngine: RtcEngine? = null
    private var isMuted = false
    private var isVideoEnabled = true
    private var callStartTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    private val client = OkHttpClient()
    private var callId = 0
    private var channelName = ""
    private var callType = "audio"
    private var isCaller = false
    private var otherUserId = ""
    private var otherUsername = ""
    private var otherProfileImage = ""
    private var currentUserId = ""
    private var agoraToken = ""

    private var statusCheckRunnable: Runnable? = null

    companion object {
        private const val BASE_URL = "http://172.15.44.21/socially_app/"
        private const val AGORA_APP_ID = "99ee06ca3522461d9a065a7ffd778724"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.callpage)

        val sessionManager = SessionManager(this)
        currentUserId = sessionManager.getUserId() ?: ""

        callId = intent.getIntExtra("call_id", 0)
        channelName = intent.getStringExtra("channel_name") ?: ""
        callType = intent.getStringExtra("call_type") ?: "audio"
        isCaller = intent.getBooleanExtra("is_caller", false)
        otherUserId = intent.getStringExtra("other_user_id") ?: ""
        otherUsername = intent.getStringExtra("other_username") ?: ""
        otherProfileImage = intent.getStringExtra("other_profile_image") ?: ""

        initializeViews()
        setupAgoraEngine()
        getAgoraToken()

        if (isCaller) {
            startCallStatusCheck()
        }
    }

    private fun initializeViews() {
        profileImage = findViewById(R.id.profile_image)
        callName = findViewById(R.id.callname)
        callTime = findViewById(R.id.calltime)
        callEndBtn = findViewById(R.id.callend)
        muteBtn = findViewById(R.id.volume)
        localVideoContainer = findViewById(R.id.local_video_container)
        remoteVideoContainer = findViewById(R.id.remote_video_container)

        callName.text = otherUsername
        callTime.text = "Connecting..."

        if (otherProfileImage.isNotEmpty()) {
            loadProfilePicture(otherProfileImage)
        }

        if (callType == "video") {
            localVideoContainer.visibility = View.VISIBLE
            remoteVideoContainer.visibility = View.VISIBLE
        } else {
            localVideoContainer.visibility = View.GONE
            remoteVideoContainer.visibility = View.GONE
        }

        callEndBtn.setOnClickListener {
            endCall()
        }

        muteBtn.setOnClickListener {
            toggleMute()
        }

        profileImage.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
        profileImage.clipToOutline = true
    }

    private fun loadProfilePicture(profilePicPath: String) {
        val url = "${BASE_URL}get_profile_pic.php?path=$profilePicPath"

        val request = com.android.volley.toolbox.StringRequest(
            com.android.volley.Request.Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response)
                    if (obj.getInt("statuscode") == 200) {
                        val imageBase64 = obj.getString("image")
                        val decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        profileImage.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    Log.e("callpage", "Error loading profile pic: ${e.message}")
                }
            },
            { error ->
                Log.e("callpage", "Network error: ${error.message}")
            }
        )

        com.android.volley.toolbox.Volley.newRequestQueue(this).add(request)
    }

    private fun setupAgoraEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = applicationContext
            config.mAppId = AGORA_APP_ID
            config.mEventHandler = object : IRtcEngineEventHandler() {
                override fun onUserJoined(uid: Int, elapsed: Int) {
                    runOnUiThread {
                        setupRemoteVideo(uid)
                        callTime.text = "Connected"
                        startCallTimer()
                    }
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    runOnUiThread {
                        Toast.makeText(this@callpage, "User left the call", Toast.LENGTH_SHORT).show()
                        endCall()
                    }
                }

                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                    runOnUiThread {
                        Log.d("callpage", "Joined channel: $channel")
                    }
                }
            }

            agoraEngine = RtcEngine.create(config)

            if (callType == "video") {
                agoraEngine?.enableVideo()
                setupLocalVideo()
            } else {
                agoraEngine?.disableVideo()
            }

        } catch (e: Exception) {
            Log.e("callpage", "Agora initialization error: ${e.message}")
            Toast.makeText(this, "Failed to initialize call", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun getAgoraToken() {
        val request = FormBody.Builder()
            .add("channel_name", channelName)
            .add("user_id", currentUserId)
            .build()

        val httpRequest = Request.Builder()
            .url("${BASE_URL}generate_agora_token.php")
            .post(request)
            .build()

        client.newCall(httpRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Log.e("CallPage", "Network error: ${e.message}")
                    Toast.makeText(this@callpage, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    try {
                        if (responseData.isNullOrEmpty()) {
                            Log.e("CallPage", "Empty response from server")
                            Toast.makeText(this@callpage, "Empty response from server", Toast.LENGTH_SHORT).show()
                            finish()
                            return@runOnUiThread
                        }

                        // Log the raw response for debugging
                        Log.d("CallPage", "Raw response: $responseData")

                        // Check if response contains HTML/errors
                        if (responseData.contains("<br") || responseData.contains("<html") ||
                            responseData.contains("<!DOCTYPE")) {
                            Log.e("CallPage", "Server returned HTML instead of JSON: $responseData")
                            Toast.makeText(
                                this@callpage,
                                "Server error. Check PHP error logs.",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                            return@runOnUiThread
                        }

                        // Try to parse JSON
                        val json = JSONObject(responseData)
                        Log.d("CallPage", "Parsed JSON: $json")

                        if (json.getInt("statuscode") == 200) {
                            agoraToken = json.getString("token")
                            Log.d("CallPage", "Token received: ${agoraToken.take(20)}...")
                            joinChannel()
                        } else {
                            val errorMsg = json.optString("message", "Failed to generate token")
                            Log.e("CallPage", "Server error: $errorMsg")
                            Toast.makeText(this@callpage, errorMsg, Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } catch (e: Exception) {
                        Log.e("CallPage", "Parse error: ${e.message}")
                        Log.e("CallPage", "Response was: $responseData")
                        Toast.makeText(
                            this@callpage,
                            "Parse error: ${e.message}\nCheck logs",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            }
        })
    }

    private fun joinChannel() {
        val options = ChannelMediaOptions()
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER

        val uid = currentUserId.toIntOrNull() ?: 0
        Log.d("CallPage", "Joining channel: $channelName with UID: $uid")

        agoraEngine?.joinChannel(agoraToken, channelName, uid, options)
    }

    private fun setupLocalVideo() {
        val surfaceView = RtcEngine.CreateRendererView(applicationContext)
        surfaceView.setZOrderMediaOverlay(true)
        localVideoContainer.addView(surfaceView)

        val uid = currentUserId.toIntOrNull() ?: 0
        agoraEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
        agoraEngine?.startPreview()
    }

    private fun setupRemoteVideo(uid: Int) {
        val surfaceView = RtcEngine.CreateRendererView(applicationContext)
        remoteVideoContainer.addView(surfaceView)

        agoraEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
        profileImage.visibility = View.GONE
    }

    private fun toggleMute() {
        isMuted = !isMuted
        agoraEngine?.muteLocalAudioStream(isMuted)

        if (isMuted) {
            muteBtn.setImageResource(R.drawable.ic_mic_off)
            Toast.makeText(this, "Muted", Toast.LENGTH_SHORT).show()
        } else {
            muteBtn.setImageResource(R.drawable.volume)
            Toast.makeText(this, "Unmuted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCallTimer() {
        callStartTime = System.currentTimeMillis()

        timerRunnable = object : Runnable {
            override fun run() {
                val elapsed = (System.currentTimeMillis() - callStartTime) / 1000
                val minutes = elapsed / 60
                val seconds = elapsed % 60
                callTime.text = String.format("%02d:%02d", minutes, seconds)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timerRunnable!!)
    }

    private fun startCallStatusCheck() {
        statusCheckRunnable = object : Runnable {
            override fun run() {
                checkCallStatus()
                handler.postDelayed(this, 2000)
            }
        }
        handler.post(statusCheckRunnable!!)
    }

    private fun checkCallStatus() {
        val request = FormBody.Builder()
            .add("action", "check_status")
            .add("call_id", callId.toString())
            .build()

        val httpRequest = Request.Builder()
            .url("${BASE_URL}call_invitation.php")
            .post(request)
            .build()

        client.newCall(httpRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    try {
                        val json = JSONObject(responseData ?: "{}")
                        if (json.getInt("statuscode") == 200) {
                            val status = json.getString("status")
                            if (status == "rejected" || status == "ended") {
                                Toast.makeText(this@callpage, "Call $status", Toast.LENGTH_SHORT).show()
                                finish()
                            } else if (status == "answered") {
                                statusCheckRunnable?.let { handler.removeCallbacks(it) }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("callpage", "Error: ${e.message}")
                    }
                }
            }
        })
    }

    private fun endCall() {
        val request = FormBody.Builder()
            .add("action", "end")
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

        timerRunnable?.let { handler.removeCallbacks(it) }
        statusCheckRunnable?.let { handler.removeCallbacks(it) }

        agoraEngine?.leaveChannel()

        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }
}