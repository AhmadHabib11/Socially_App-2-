package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class storyactivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.storyactivity)

        val close = findViewById<ImageView>(R.id.closeIcon)
        close.setOnClickListener {
             val intent = Intent(this, feedpage::class.java)
            startActivity(intent)
            finish()
        }
        val highlighticon = findViewById<LinearLayout>(R.id.highlight)
        highlighticon.setOnClickListener {
            val intent = Intent(this, highlight::class.java)
            startActivity(intent)
            finish()
        }

        val create = findViewById<LinearLayout>(R.id.createvid)
        create.setOnClickListener {
            val intent = Intent(this, camera::class.java)
            startActivity(intent)
            finish()
        }


    }
}