package com.teamapex.I23_0011_I23_0646

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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

class editprofile : AppCompatActivity() {

    private val GALLERY_REQUEST_CODE = 300
    private lateinit var profilePic: ImageView
    private lateinit var nameEdit: EditText
    private lateinit var usernameEdit: EditText
    private lateinit var websiteEdit: EditText
    private lateinit var bioEdit: EditText
    private lateinit var emailEdit: EditText
    private lateinit var phoneEdit: EditText
    private lateinit var genderEdit: EditText

    private var selectedImageBitmap: Bitmap? = null
    private var hasNewImage = false
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.editprofile)

        // Initialize views
        profilePic = findViewById(R.id.profilePic)
        nameEdit = findViewById(R.id.name)
        usernameEdit = findViewById(R.id.username)
        websiteEdit = findViewById(R.id.website)
        bioEdit = findViewById(R.id.bio)
        emailEdit = findViewById(R.id.email)
        phoneEdit = findViewById(R.id.phone)
        genderEdit = findViewById(R.id.gender)

        // Load current user data
        loadUserData()

        val cancel = findViewById<TextView>(R.id.cancelBtn)
        cancel.setOnClickListener {
            val intent = Intent(this, profile::class.java)
            startActivity(intent)
            finish()
        }

        val save = findViewById<TextView>(R.id.saveBtn)
        save.setOnClickListener {
            saveProfile()
        }

        val changedp = findViewById<TextView>(R.id.changePhoto)
        changedp.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, GALLERY_REQUEST_CODE)
        }

        val signout = findViewById<TextView>(R.id.signoutBtn)
        signout.setOnClickListener {
            handleLogout()
        }
    }

    private fun loadUserData() {
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        currentUserId = sp.getString("userid", "") ?: ""
        val username = sp.getString("username", "") ?: ""
        val firstName = sp.getString("first_name", "") ?: ""
        val lastName = sp.getString("last_name", "") ?: ""
        val email = sp.getString("email", "") ?: ""
        val userProfilePic = sp.getString("profile_pic", "") ?: ""

        // Set current data in fields
        usernameEdit.setText(username)
        nameEdit.setText("$firstName $lastName")
        emailEdit.setText(email)

        // Load profile picture
        loadProfilePicture(userProfilePic)

        // Fetch additional data from server (bio, website, phone, gender)
        fetchAdditionalUserData()
    }

    private fun loadProfilePicture(profilePicPath: String) {
        if (profilePicPath.isEmpty()) {
            profilePic.setImageResource(R.drawable.story1)
            return
        }

        val url = "http://172.15.44.21/socially_app/get_profile_pic.php?path=$profilePicPath"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        val imageBase64 = obj.getString("image")
                        val decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        profilePic.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    Log.e("EditProfile", "Error loading profile pic: ${e.message}")
                }
            },
            { error ->
                Log.e("EditProfile", "Network error: ${error.message}")
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun fetchAdditionalUserData() {
        val url = "http://172.15.44.21/socially_app/get_user_data.php?user_id=$currentUserId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        val user = obj.getJSONObject("user")

                        websiteEdit.setText(user.optString("website", ""))
                        bioEdit.setText(user.optString("bio", ""))
                        phoneEdit.setText(user.optString("phone", ""))
                        genderEdit.setText(user.optString("gender", ""))
                    }
                } catch (e: Exception) {
                    Log.e("EditProfile", "Error: ${e.message}")
                }
            },
            { error ->
                Log.e("EditProfile", "Network error: ${error.message}")
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            try {
                val imageUri: Uri? = data.data
                if (imageUri != null) {
                    selectedImageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                    profilePic.setImageBitmap(selectedImageBitmap)
                    hasNewImage = true
                    Toast.makeText(this, "Image selected! Click Save to update", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveProfile() {
        val fullName = nameEdit.text.toString().trim()
        val username = usernameEdit.text.toString().trim()
        val website = websiteEdit.text.toString().trim()
        val bio = bioEdit.text.toString().trim()
        val phone = phoneEdit.text.toString().trim()
        val gender = genderEdit.text.toString().trim()

        if (username.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Split name into first and last
        val nameParts = fullName.split(" ", limit = 2)
        val firstName = nameParts.getOrNull(0) ?: ""
        val lastName = nameParts.getOrNull(1) ?: ""

        val url = "http://172.15.44.21/socially_app/update_profile.php"

        val pd = ProgressDialog(this)
        pd.setMessage("Updating profile...")
        pd.show()

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                pd.dismiss()

                try {
                    Log.d("EditProfile", "Response: $response")
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        val user = obj.getJSONObject("user")

                        // Update SharedPreferences
                        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
                        val editor = sp.edit()

                        // Update current session
                        editor.putString("username", user.getString("username"))
                        editor.putString("first_name", user.getString("first_name"))
                        editor.putString("last_name", user.getString("last_name"))
                        if (hasNewImage) {
                            editor.putString("profile_pic", user.getString("profile_pic"))
                        }

                        // Update "last" user data (for authorization page)
                        editor.putString("last_username", user.getString("username"))
                        editor.putString("last_first_name", user.getString("first_name"))
                        editor.putString("last_last_name", user.getString("last_name"))
                        if (hasNewImage) {
                            editor.putString("last_profile_pic", user.getString("profile_pic"))
                        }

                        editor.apply()

                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this, profile::class.java)
                        startActivity(intent)
                        finish()

                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_LONG).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("EditProfile", "Parse error: ${e.message}")
                }
            },
            { error ->
                pd.dismiss()
                Toast.makeText(this, "Network Error: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e("EditProfile", "Network error: ${error.message}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = currentUserId
                params["username"] = username
                params["first_name"] = firstName
                params["last_name"] = lastName
                params["website"] = website
                params["bio"] = bio
                params["phone"] = phone
                params["gender"] = gender

                // Add profile picture if changed
                if (hasNewImage && selectedImageBitmap != null) {
                    params["profile_pic_data"] = bitmapToBase64(selectedImageBitmap!!)
                }

                return params
            }
        }

        request.setRetryPolicy(
            com.android.volley.DefaultRetryPolicy(
                30000,
                0,
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

    private fun handleLogout() {
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        val editor = sp.edit()

        // Save current user ID as last_user_id before clearing session
        val currentUserId = sp.getString("userid", "")
        if (!currentUserId.isNullOrEmpty()) {
            editor.putString("last_user_id", currentUserId)
        }

        // Clear login status
        editor.putBoolean("is_logged_in", false)

        // Clear current session data
        editor.remove("userid")
        editor.remove("username")
        editor.remove("first_name")
        editor.remove("last_name")
        editor.remove("email")
        editor.remove("profile_pic")

        editor.apply()

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, authorization::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}