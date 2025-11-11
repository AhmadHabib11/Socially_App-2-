package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class dmscreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dmscreen)
        val img = findViewById<ImageView>(R.id.gallery)


        val vidcall = findViewById<ImageView>(R.id.vidcall)

        val backarrow = findViewById<ImageView>(R.id.backArrow)

        val cam = findViewById<ImageView>(R.id.camera)

        vidcall.setOnClickListener {
            val intent = Intent(this, callpage ::class.java)
            startActivity(intent)
            finish()
        }

        img.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivity(intent)  // Just opens gallery
        }
        backarrow.setOnClickListener {
            val intent = Intent(this, chatlist ::class.java)
            startActivity(intent)
            finish()
        }

        cam.setOnClickListener {
            val intent = Intent(this, camera ::class.java)
            startActivity(intent)
        }


    }
}