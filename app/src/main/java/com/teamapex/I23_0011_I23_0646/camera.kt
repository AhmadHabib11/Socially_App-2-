package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class camera : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.camera)

        val close = findViewById<ImageView>(R.id.closeIcon)
        close.setOnClickListener {
            val intent = Intent(this, feedpage::class.java)
            startActivity(intent)
            finish()
        }
        val shuttenbtn = findViewById<ImageView>(R.id.shutterButton)
        shuttenbtn.setOnClickListener {
            val intent = Intent(this, storyupload::class.java)
            startActivity(intent)
            finish()
        }
//        val library = findViewById<ImageView>(R.id.galleryIcon)
//        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
//        startActivity(intent)  // Just opens gallery

    }
}