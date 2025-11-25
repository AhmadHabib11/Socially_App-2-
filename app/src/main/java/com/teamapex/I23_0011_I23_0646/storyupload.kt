package com.teamapex.I23_0011_I23_0646

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class storyupload : AppCompatActivity() {

    private lateinit var storyImage: ImageView
    private var imageBase64: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.storyupload)

        storyImage = findViewById(R.id.storyImage)

        // Get image data from intent
        imageBase64 = intent.getStringExtra("image_base64") ?: ""

        // Display the captured image
        if (imageBase64.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                storyImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
                Log.e("StoryUpload", "Error: ${e.message}")
            }
        }

        val closebtn = findViewById<ImageView>(R.id.closeBtn)
        closebtn.setOnClickListener {
            val intent = Intent(this, feedpage::class.java)
            startActivity(intent)
            finish()
        }

        val send = findViewById<ImageView>(R.id.sendIcon)
        send.setOnClickListener {
            // Upload story when send icon is clicked
            uploadStory()
        }

        val story = findViewById<LinearLayout>(R.id.yourstory)
        story.setOnClickListener {
            // Upload to your story
            uploadStory()
        }

        val close = findViewById<LinearLayout>(R.id.closefriends)
        close.setOnClickListener {
            // Upload to close friends (for now, same as regular story)
            uploadStory()
        }
    }

    private fun uploadStory() {
        if (imageBase64.isEmpty()) {
            Toast.makeText(this, "No image to upload", Toast.LENGTH_SHORT).show()
            return
        }

        // Get user ID from session
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        val userId = sp.getString("userid", "") ?: ""

        if (userId.isEmpty()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://192.168.100.76/socially_app/upload_story.php"

        val pd = ProgressDialog(this)
        pd.setMessage("Uploading story...")
        pd.show()

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                pd.dismiss()

                try {
                    Log.d("StoryUpload", "Response: $response")
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        Toast.makeText(this, "Story uploaded successfully!", Toast.LENGTH_SHORT).show()

                        // Navigate to story activity to view the uploaded story
                        val intent = Intent(this, storyactivity::class.java)
                        startActivity(intent)
                        finish()

                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_LONG).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("StoryUpload", "Parse error: ${e.message}")
                }
            },
            { error ->
                pd.dismiss()
                Toast.makeText(this, "Network Error: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e("StoryUpload", "Network error: ${error.message}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = userId
                params["media_data"] = imageBase64
                params["media_type"] = "image"
                return params
            }
        }

        // Increase timeout for large image uploads
        request.setRetryPolicy(
            com.android.volley.DefaultRetryPolicy(
                30000, // 30 seconds timeout
                com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        )

        Volley.newRequestQueue(this).add(request)
    }
}