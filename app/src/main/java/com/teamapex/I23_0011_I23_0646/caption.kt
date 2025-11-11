package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream
import java.util.*

class caption : AppCompatActivity() {
    private lateinit var selectedBitmap: Bitmap
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.caption)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()

        // Get the image that was passed from createpost screen
        val byteArray = intent.getByteArrayExtra("imageBitmap")

        if (byteArray != null) {
            // Convert byte array back to Bitmap
            selectedBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

            // Display the image in ImageView
            val imageView = findViewById<ImageView>(R.id.captionImageView)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.setImageBitmap(selectedBitmap)
        } else {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Get UI elements
        val captionEditText = findViewById<EditText>(R.id.captionEditText)
        val postBtn = findViewById<TextView>(R.id.postBtn)
        val cancelBtn = findViewById<TextView>(R.id.cancelCaptionBtn)

        // Post button - saves post to Firebase
        postBtn.setOnClickListener {
            val caption = captionEditText.text.toString().trim()

            if (caption.isEmpty()) {
                Toast.makeText(this, "Please write a caption", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading message
            Toast.makeText(this, "Uploading post...", Toast.LENGTH_SHORT).show()

            // Upload to Firebase
            uploadPostToFirebase(caption)
        }

        // Cancel button - goes back to feed without posting
        cancelBtn.setOnClickListener {
            val intent = Intent(this, feedpage::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun scaleBitmapToFitImageView(bitmap: Bitmap, imageView: ImageView): Bitmap {
        val width = imageView.width
        val height = imageView.height

        // If ImageView dimensions are not ready, use default dimensions
        val targetWidth = if (width > 0) width else 500
        val targetHeight = if (height > 0) height else 500

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun uploadPostToFirebase(caption: String) {
        val currentUser = auth.currentUser

        // Check if user is logged in
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid

        // Fetch the actual username from Firebase database
        database.reference.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Try to get the username from different possible field names
                    val userName = snapshot.child("name").getValue(String::class.java)
                        ?: snapshot.child("username").getValue(String::class.java)
                        ?: snapshot.child("fullName").getValue(String::class.java)
                        ?: currentUser.displayName
                        ?: "Unknown User"

                    // Now save the post with the actual username
                    savePost(userId, userName, currentUser.email ?: "", caption)
                }

                override fun onCancelled(error: DatabaseError) {
                    // If fetching fails, use fallback
                    val userName = currentUser.displayName ?: "Unknown User"
                    savePost(userId, userName, currentUser.email ?: "", caption)

                    Toast.makeText(
                        this@caption,
                        "Warning: Could not fetch username",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun savePost(userId: String, userName: String, userEmail: String, caption: String) {
        // Convert Bitmap to Base64 string
        val stream = ByteArrayOutputStream()
        selectedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        val imageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)

        // Create a unique post ID
        val postId = UUID.randomUUID().toString()

        // Create the post object with actual username
        val post = hashMapOf(
            "postId" to postId,
            "userId" to userId,
            "userName" to userName,  // This now contains the actual username!
            "userEmail" to userEmail,
            "caption" to caption,
            "imageBase64" to imageBase64,
            "timestamp" to System.currentTimeMillis(),
            "likes" to 0,
            "comments" to emptyMap<String, Any>()
        )

        // Save to Firebase Realtime Database
        database.reference.child("posts").child(postId).setValue(post)
            .addOnSuccessListener {
                Toast.makeText(this, "Post uploaded successfully!", Toast.LENGTH_SHORT).show()

                // Go back to feed
                val intent = Intent(this, feedpage::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to upload: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}