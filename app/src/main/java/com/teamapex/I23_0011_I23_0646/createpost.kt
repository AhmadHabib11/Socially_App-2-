package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream

class createpost : AppCompatActivity() {

    private val GALLERY_REQUEST_CODE = 200
    private lateinit var previewImage: ImageView
    private var selectedImageBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.createpost)

        previewImage = findViewById(R.id.previewImage)

        val next = findViewById<TextView>(R.id.nextBtn)
        next.setOnClickListener {
            if (selectedImageBitmap != null) {
                // Navigate to caption screen with selected image
                navigateToCaptionScreen()
            } else {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            }
        }

        val cancel = findViewById<TextView>(R.id.cancelBtn)
        cancel.setOnClickListener {
            val intent = Intent(this, feedpage::class.java)
            startActivity(intent)
            finish()
        }

        // Open gallery to select image
        val lib = findViewById<TextView>(R.id.library)
        lib.setOnClickListener {
            openGallery()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            try {
                val imageUri: Uri? = data.data
                if (imageUri != null) {
                    // Load the selected image
                    selectedImageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)

                    // Display in preview
                    previewImage.setImageBitmap(selectedImageBitmap)

                    Toast.makeText(this, "Image selected! Click Next to continue", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToCaptionScreen() {
        if (selectedImageBitmap != null) {
            // Convert bitmap to byte array to pass to next activity
            val stream = ByteArrayOutputStream()
            selectedImageBitmap!!.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val byteArray = stream.toByteArray()

            val intent = Intent(this, caption::class.java)
            intent.putExtra("imageBitmap", byteArray)
            startActivity(intent)
            finish()
        }
    }
}