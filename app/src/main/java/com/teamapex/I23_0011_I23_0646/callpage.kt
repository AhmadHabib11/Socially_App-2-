package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class callpage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.callpage)

        val callend = findViewById<ImageView>(R.id.callroundsqr)
        val chat = findViewById<ImageView>(R.id.chatboxlines)

        callend.setOnClickListener {
            val intent = Intent(this, dmscreen ::class.java)
            startActivity(intent)
            finish()
        }

        chat.setOnClickListener {
            val intent = Intent(this, chatlist ::class.java)
            startActivity(intent)
            finish()
        }


    }
}