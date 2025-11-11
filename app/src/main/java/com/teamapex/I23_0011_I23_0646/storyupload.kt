package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class storyupload : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.storyupload)

        val closebtn = findViewById<ImageView>(R.id.closeBtn)
        closebtn.setOnClickListener {
            val intent = Intent(this, profile::class.java)
            startActivity(intent)
            finish()
        }

        val send = findViewById<ImageView>(R.id.sendIcon)
        send.setOnClickListener {
            val intent = Intent(this, chatlist::class.java)
            startActivity(intent)
            finish()
        }

        val story = findViewById<LinearLayout>(R.id.yourstory)
        story.setOnClickListener {
            val intent = Intent(this, storyactivity::class.java)
            startActivity(intent)
            finish()
        }
        val close = findViewById<LinearLayout>(R.id.closefriends)
        close.setOnClickListener {
            val intent = Intent(this, storyactivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}