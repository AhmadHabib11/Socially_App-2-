package com.teamapex.I23_0011_I23_0646

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.TimeoutError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class signup : AppCompatActivity() {

    private var selectedImageBase64: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup)

        val username = findViewById<EditText>(R.id.username)
        val name = findViewById<EditText>(R.id.name)
        val lastname = findViewById<EditText>(R.id.lastname)
        val dob = findViewById<EditText>(R.id.dob)
        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val createAccount = findViewById<TextView>(R.id.loginbutton)
        val camera = findViewById<ImageView>(R.id.camera)
        val backArrow = findViewById<ImageView>(R.id.backArrow)
        val profilePreview = findViewById<ImageView>(R.id.profileCircle)

        // PICK IMAGE
        camera.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 100)
        }

        backArrow.setOnClickListener {
            startActivity(Intent(this, login::class.java))
            finish()
        }

        createAccount.setOnClickListener {
            // Validation
            if (username.text.toString().isEmpty()) {
                Toast.makeText(this, "Please enter username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (name.text.toString().isEmpty()) {
                Toast.makeText(this, "Please enter first name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (lastname.text.toString().isEmpty()) {
                Toast.makeText(this, "Please enter last name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dob.text.toString().isEmpty()) {
                Toast.makeText(this, "Please enter date of birth", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email.text.toString().isEmpty()) {
                Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.text.toString().isEmpty()) {
                Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val url = "http://192.168.100.13/socially_app/signup.php"

            val pd = ProgressDialog(this)
            pd.setMessage("Creating Account...")
            pd.show()

            Log.d("Signup", "Starting signup request to: $url")

            val request = object : StringRequest(
                Request.Method.POST,
                url,
                Response.Listener<String> { response ->
                    pd.dismiss()

                    try {
                        Log.d("ServerResponse", "Raw response: $response")

                        val obj = JSONObject(response)
                        val code = obj.getInt("statuscode")

                        if (code == 200) {
                            Toast.makeText(this, "Signup Successful!", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this, login::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, obj.getString("message"), Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Parse Error - Check Logcat", Toast.LENGTH_LONG).show()
                        Log.e("ParseError", "Error: ${e.message}")
                        Log.e("ParseError", "Response: $response")
                    }
                },
                Response.ErrorListener { error ->
                    pd.dismiss()

                    when (error) {
                        is TimeoutError -> {
                            Toast.makeText(this, "Connection Timeout - Check server", Toast.LENGTH_LONG).show()
                            Log.e("NetworkError", "Timeout error")
                        }
                        else -> {
                            val errorMsg = error.message ?: "Unknown error"
                            Toast.makeText(this, "Network Error: $errorMsg", Toast.LENGTH_LONG).show()
                            Log.e("NetworkError", "Error: $errorMsg")
                            Log.e("NetworkError", "Error class: ${error.javaClass.simpleName}")

                            error.networkResponse?.let { response ->
                                Log.e("NetworkError", "Status code: ${response.statusCode}")
                                Log.e("NetworkError", "Response data: ${String(response.data)}")
                            } ?: Log.e("NetworkError", "No network response - server unreachable")
                        }
                    }
                }
            ) {
                override fun getParams(): MutableMap<String, String> {
                    val map = HashMap<String, String>()
                    map["username"] = username.text.toString()
                    map["first_name"] = name.text.toString()
                    map["last_name"] = lastname.text.toString()
                    map["dob"] = dob.text.toString()
                    map["email"] = email.text.toString()
                    map["password"] = password.text.toString()
                    map["profile_pic"] = selectedImageBase64

                    Log.d("PostParams", "Username: ${username.text}")
                    Log.d("PostParams", "Email: ${email.text}")
                    Log.d("PostParams", "Image length: ${selectedImageBase64.length}")

                    return map
                }
            }

            // Increase timeout and disable cache
            request.setShouldCache(false)
            request.setRetryPolicy(
                com.android.volley.DefaultRetryPolicy(
                    30000, // 30 seconds timeout
                    0, // no retries
                    com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                )
            )

            Volley.newRequestQueue(this).add(request)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            try {
                val uri: Uri? = data.data
                var bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)

                // RESIZE IMAGE to reduce size
                val maxWidth = 800
                val maxHeight = 800

                if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
                    val ratio = Math.min(
                        maxWidth.toFloat() / bitmap.width,
                        maxHeight.toFloat() / bitmap.height
                    )
                    val width = (bitmap.width * ratio).toInt()
                    val height = (bitmap.height * ratio).toInt()
                    bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
                }

                // Create circular bitmap and display it
                val circularBitmap = getCircularBitmap(bitmap)
                findViewById<ImageView>(R.id.profileCircle).setImageBitmap(circularBitmap)

                // HIDE the camera icon after image is selected
                findViewById<ImageView>(R.id.camera).visibility = View.GONE

                val baos = ByteArrayOutputStream()
                // Use JPEG with 80% quality to reduce size
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val imageBytes = baos.toByteArray()

                // Remove newlines from base64 string
                selectedImageBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

                Log.d("ImageConversion", "Image converted successfully, length: ${selectedImageBase64.length}")
                Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(this, "Error converting image: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("ImageConversion", "Error: ${e.message}")
            }
        }
    }

    // Helper function to create circular bitmap
    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = Math.min(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)
        val paint = Paint()
        val rect = Rect(0, 0, size, size)

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2
        val srcRect = Rect(left, top, left + size, top + size)

        canvas.drawBitmap(bitmap, srcRect, rect, paint)

        return output
    }
}