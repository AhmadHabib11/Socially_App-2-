package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class authorization : AppCompatActivity() {

    private lateinit var profilePic: ImageView
    private lateinit var usernameText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.authorization)

        profilePic = findViewById(R.id.profilePic)
        usernameText = findViewById(R.id.usernameText)
        val loginButton = findViewById<TextView>(R.id.loginbutton)
        val createAccountBtn = findViewById<TextView>(R.id.signupText)
        val switchAccounts = findViewById<TextView>(R.id.switchaccounts)

        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        val isLoggedIn = sp.getBoolean("is_logged_in", false)

        // Get the last user ID to fetch fresh data
        val lastUserId = sp.getString("last_user_id", null)

        if (!lastUserId.isNullOrEmpty()) {
            // Fetch latest user data from database
            fetchLatestUserData(lastUserId)
        } else {
            // No previous user - show guest
            usernameText.text = "Guest"
        }

        // Login button behavior changes based on login status
        loginButton.setOnClickListener {
            if (isLoggedIn) {
                // User is already logged in - go directly to feedpage (quick login)
                startActivity(Intent(this, feedpage::class.java))
                finish()
            } else {
                // User is logged out - go to login page
                val intent = Intent(this, login::class.java)

                // Prefill with current displayed username
                val displayedUsername = usernameText.text.toString()
                if (displayedUsername != "Guest") {
                    intent.putExtra("prefill_username", displayedUsername)
                }

                startActivity(intent)
                finish()
            }
        }

        // Switch accounts - goes to login page without prefill
        switchAccounts.setOnClickListener {
            startActivity(Intent(this, login::class.java))
            finish()
        }

        // Create account
        createAccountBtn.setOnClickListener {
            startActivity(Intent(this, signup::class.java))
            finish()
        }
    }

    private fun fetchLatestUserData(userId: String) {
        val url = "http://192.168.100.76/socially_app/get_user_data.php?user_id=$userId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    Log.d("Authorization", "Response: $response")
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        val user = obj.getJSONObject("user")

                        // Get latest username
                        val username = user.getString("username")
                        usernameText.text = username

                        // Get latest profile picture
                        val profilePicPath = user.getString("profile_pic")
                        if (!profilePicPath.isNullOrEmpty() && profilePicPath != "null" && profilePicPath != "0") {
                            loadProfilePicture(profilePicPath, profilePic)
                        }

                        // Update SharedPreferences with latest data
                        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
                        val editor = sp.edit()
                        editor.putString("last_username", username)
                        editor.putString("last_profile_pic", profilePicPath)
                        editor.apply()

                    } else {
                        usernameText.text = "Guest"
                    }

                } catch (e: Exception) {
                    Log.e("Authorization", "Error: ${e.message}")
                    usernameText.text = "Guest"
                }
            },
            { error ->
                Log.e("Authorization", "Network error: ${error.message}")

                // Fallback to cached username if network fails
                val sp = getSharedPreferences("user_session", MODE_PRIVATE)
                val cachedUsername = sp.getString("last_username", "Guest")
                usernameText.text = cachedUsername ?: "Guest"

                val cachedPic = sp.getString("last_profile_pic", null)
                if (!cachedPic.isNullOrEmpty() && cachedPic != "null" && cachedPic != "0") {
                    loadProfilePicture(cachedPic, profilePic)
                }
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun loadProfilePicture(path: String, imageView: ImageView) {
        val url = "http://192.168.100.76/socially_app/get_profile_pic.php?path=$path"

        val request = object : StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        val base64Image = obj.getString("image")
                        val bytes = Base64.decode(base64Image, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                        bitmap?.let {
                            imageView.setImageBitmap(getCircularBitmap(it))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ProfilePic", e.message ?: "error")
                }
            },
            { error -> Log.e("ProfilePic", error.message ?: "network error") }
        ) {}

        Volley.newRequestQueue(this).add(request)
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)
        val paint = Paint().apply { isAntiAlias = true }
        val rect = Rect(0, 0, size, size)

        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2
        val srcRect = Rect(left, top, left + size, top + size)

        canvas.drawBitmap(bitmap, srcRect, rect, paint)

        return output
    }
}