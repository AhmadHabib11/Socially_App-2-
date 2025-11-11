package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class highlight : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.highlight)

        val close = findViewById<ImageView>(R.id.closeIcon)
        close.setOnClickListener {
            val intent = Intent(this, profile::class.java)
            startActivity(intent)
            finish()
        }

        val createcam = findViewById<LinearLayout>(R.id.createvid)
        createcam.setOnClickListener {
            val intent = Intent(this, camera::class.java)
            startActivity(intent)
            finish()
        }

        val create = findViewById<LinearLayout>(R.id.createpost)
        create.setOnClickListener {
            val intent = Intent(this, createpost::class.java)
            startActivity(intent)
            finish()
        }

        val send = findViewById<LinearLayout>(R.id.send)
        send.setOnClickListener {
            val intent = Intent(this, chatlist::class.java)
            startActivity(intent)
            finish()
        }






    }
}