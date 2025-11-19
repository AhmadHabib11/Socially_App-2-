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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.authorization)

        val profilePic = findViewById<ImageView>(R.id.profilePic)
        val usernameText = findViewById<TextView>(R.id.usernameText)
        val loginButton = findViewById<TextView>(R.id.loginbutton)
        val createAccountBtn = findViewById<TextView>(R.id.signupText)
        val switchAccounts = findViewById<TextView>(R.id.switchaccounts)

        val sp = getSharedPreferences("user_session", MODE_PRIVATE)

        // Load saved user data
        val lastUsername = sp.getString("username", "Guest")
        val lastPic = sp.getString("profile_pic", null)

        usernameText.text = lastUsername

        // Load profile picture if exists
        if (!lastPic.isNullOrEmpty() && lastPic != "null" && lastPic != "0") {
            loadProfilePicture(lastPic, profilePic)
        }

        // Login button
        loginButton.setOnClickListener {
            val intent = Intent(this, login::class.java)
            intent.putExtra("prefill_username", lastUsername)
            startActivity(intent)
        }

        // Switch accounts
        switchAccounts.setOnClickListener {
            startActivity(Intent(this, login::class.java))
        }

        // Create account
        createAccountBtn.setOnClickListener {
            startActivity(Intent(this, signup::class.java))
        }
    }

    private fun loadProfilePicture(path: String, imageView: ImageView) {
        val url = "http://192.168.18.100/socially_app/get_profile_pic.php?path=$path"

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
