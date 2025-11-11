package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class searchpage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.searchpage)

        val notif = findViewById<ImageView>(R.id.notifIcon)
        val home = findViewById<ImageView>(R.id.homeIcon)
        val search = findViewById<ImageView>(R.id.searchicon21)
        val profileicon = findViewById<ImageView>(R.id.profileIcon)
        val create = findViewById<ImageView>(R.id.reelsIcon)



        notif.setOnClickListener {
            val intent = Intent(this, notifications ::class.java)
            startActivity(intent)
            finish()

        }
        home.setOnClickListener {
            val intent = Intent(this, feedpage ::class.java)
            startActivity(intent)
            finish()
        }
        search.setOnClickListener {
            val intent = Intent(this, typesearch ::class.java)
            startActivity(intent)
            finish()
        }
        profileicon.setOnClickListener {
            val intent = Intent(this, profile ::class.java)
            startActivity(intent)
            finish()
        }
        create.setOnClickListener {
            val intent = Intent(this, createpost ::class.java)
            startActivity(intent)
            finish()
        }


    }
}