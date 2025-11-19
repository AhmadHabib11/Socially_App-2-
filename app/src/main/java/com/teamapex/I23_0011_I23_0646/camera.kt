package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream

class camera : AppCompatActivity() {

    private val GALLERY_REQUEST_CODE = 100
    private var capturedImageBase64: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera)

        val close = findViewById<ImageView>(R.id.closeIcon)
        close.setOnClickListener {
            val intent = Intent(this, feedpage::class.java)
            startActivity(intent)
            finish()
        }

        val shuttenbtn = findViewById<ImageView>(R.id.shutterButton)
        shuttenbtn.setOnClickListener {
            // For now, we'll use a placeholder image
            // In production, you'd capture from camera here
            openGallery()
        }

        val library = findViewById<ImageView>(R.id.galleryIcon)
        library.setOnClickListener {
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
                val imageUri = data.data
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)

                // Convert bitmap to base64
                capturedImageBase64 = bitmapToBase64(bitmap)

                // Navigate to upload screen with image data
                val intent = Intent(this, storyupload::class.java)
                intent.putExtra("image_base64", capturedImageBase64)
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}