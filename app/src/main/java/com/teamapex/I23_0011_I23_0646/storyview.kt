package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class storyview : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.storyview)

        val send = findViewById<ImageView>(R.id.send)
        send.setOnClickListener {
            val intent = Intent(this, dmscreen::class.java)
            startActivity(intent)
            finish()
        }
        val cam = findViewById<ImageView>(R.id.camera)
        cam.setOnClickListener {
            val intent = Intent(this, camera::class.java)
            startActivity(intent)
            finish()
        }

        val close = findViewById<ImageView>(R.id.closeIcon)
        close.setOnClickListener {
            val intent = Intent(this, feedpage::class.java)
            startActivity(intent)
            finish()
        }
    }
}