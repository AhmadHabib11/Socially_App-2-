package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class notifications : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notifications)

        val youtab = findViewById<TextView>(R.id.youTab)
        val search = findViewById<ImageView>(R.id.searchIcon)
        val home = findViewById<ImageView>(R.id.homeIcon)
        val profile = findViewById<ImageView>(R.id.profileIcon)
        val create = findViewById<ImageView>(R.id.reelsIcon)
        val p1 = findViewById<ImageView>(R.id.p1)
        val p2 = findViewById<ImageView>(R.id.p2)
        val p3 = findViewById<ImageView>(R.id.p3)
        val p4 = findViewById<ImageView>(R.id.p4)
        val p5 = findViewById<ImageView>(R.id.p5)


        youtab.setOnClickListener {
            val intent = Intent(this, notifications2 ::class.java)
            startActivity(intent)
            finish()
        }
        search.setOnClickListener {
            val intent = Intent(this, searchpage ::class.java)
            startActivity(intent)
            finish()
        }
        home.setOnClickListener {
            val intent = Intent(this, feedpage ::class.java)
            startActivity(intent)
            finish()
        }
        profile.setOnClickListener {
            val intent = Intent(this, profile ::class.java)
            startActivity(intent)
            finish()
        }
        create.setOnClickListener {
            val intent = Intent(this, createpost ::class.java)
            startActivity(intent)
            finish()
        }

        p1.setOnClickListener {
            val intent = Intent(this, profvisitnofollow ::class.java)
            startActivity(intent)
            finish()
        }
        p2.setOnClickListener {
            val intent = Intent(this, profvisitnofollow ::class.java)
            startActivity(intent)
            finish()
        }
        p3.setOnClickListener {
            val intent = Intent(this, profvisitnofollow ::class.java)
            startActivity(intent)
            finish()
        }
        p4.setOnClickListener {
            val intent = Intent(this, profvisitnofollow ::class.java)
            startActivity(intent)
            finish()
        }
        p5.setOnClickListener {
            val intent = Intent(this, profvisitnofollow ::class.java)
            startActivity(intent)
            finish()
        }





    }
}