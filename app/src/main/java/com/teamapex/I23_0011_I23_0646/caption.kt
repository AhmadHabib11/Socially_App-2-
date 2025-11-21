package com.teamapex.I23_0011_I23_0646

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class caption : AppCompatActivity() {
    private lateinit var selectedBitmap: Bitmap
    private lateinit var captionImageView: ImageView
    private lateinit var captionEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.caption)

        captionImageView = findViewById(R.id.captionImageView)
        captionEditText = findViewById(R.id.captionEditText)

        // Get the image that was passed from createpost screen
        val byteArray = intent.getByteArrayExtra("imageBitmap")

        if (byteArray != null) {
            // Convert byte array back to Bitmap
            selectedBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

            // Display the image in ImageView
            captionImageView.scaleType = ImageView.ScaleType.CENTER_CROP
            captionImageView.setImageBitmap(selectedBitmap)
        } else {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Get UI elements
        val postBtn = findViewById<TextView>(R.id.postBtn)
        val cancelBtn = findViewById<TextView>(R.id.cancelCaptionBtn)

        // Post button - uploads post to server
        postBtn.setOnClickListener {
            val caption = captionEditText.text.toString().trim()

            if (caption.isEmpty()) {
                Toast.makeText(this, "Please write a caption", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Upload to server
            uploadPost(caption)
        }

        // Cancel button - goes back to feed without posting
        cancelBtn.setOnClickListener {
            val intent = Intent(this, feedpage::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun uploadPost(caption: String) {
        // Get user ID from session
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        val userId = sp.getString("userid", "") ?: ""

        if (userId.isEmpty()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        // Convert bitmap to base64
        val imageBase64 = bitmapToBase64(selectedBitmap)

        val url = "http://192.168.100.76/socially_app/upload_post.php"

        val pd = ProgressDialog(this)
        pd.setMessage("Uploading post...")
        pd.show()

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                pd.dismiss()

                try {
                    Log.d("PostUpload", "Response: $response")
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        Toast.makeText(this, "Post uploaded successfully!", Toast.LENGTH_SHORT).show()

                        // Navigate back to feed page
                        val intent = Intent(this, feedpage::class.java)
                        startActivity(intent)
                        finish()

                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_LONG).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("PostUpload", "Parse error: ${e.message}")
                }
            },
            { error ->
                pd.dismiss()
                Toast.makeText(this, "Network Error: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e("PostUpload", "Network error: ${error.message}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = userId
                params["caption"] = caption
                params["image_data"] = imageBase64
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

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}