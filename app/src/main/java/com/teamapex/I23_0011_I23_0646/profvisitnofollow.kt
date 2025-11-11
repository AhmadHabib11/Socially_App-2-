package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class profvisitnofollow : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.profvisitnofollow)


        val search = findViewById<ImageView>(R.id.searchIcon)
        val notif = findViewById<ImageView>(R.id.notifIcon)
        val prof = findViewById<ImageView>(R.id.profileIcon)
        val create = findViewById<ImageView>(R.id.reelsIcon)

        create.setOnClickListener {
            val intent = Intent(this, createpost::class.java)
            startActivity(intent)
            finish()
        }

        search.setOnClickListener {
            val intent = Intent(this, searchpage ::class.java)
            startActivity(intent)
            finish()
        }
        notif.setOnClickListener {
            val intent = Intent(this, notifications ::class.java)
            startActivity(intent)
            finish()

        }
        prof.setOnClickListener {
            val intent = Intent(this, profile::class.java)
            startActivity(intent)
            finish()
        }

        val h1 = findViewById<ImageView>(R.id.h1)
        val h2 = findViewById<ImageView>(R.id.h2)
        val h3 = findViewById<ImageView>(R.id.h3)
        val h4 = findViewById<ImageView>(R.id.h4)

        h1.setOnClickListener {
            val intent = Intent(this, highlight::class.java)
            startActivity(intent)
            finish()
        }
        h2.setOnClickListener {
            val intent = Intent(this, highlight::class.java)
            startActivity(intent)
            finish()
        }
        h3.setOnClickListener {
            val intent = Intent(this, highlight::class.java)
            startActivity(intent)
            finish()
        }
        h4.setOnClickListener {
            val intent = Intent(this, highlight::class.java)
            startActivity(intent)
            finish()
        }



        val followbtn = findViewById<LinearLayout>(R.id.followButton)
        followbtn.setOnClickListener {
            val intent = Intent(this, profilevisitfollowing::class.java)
            startActivity(intent)
            finish()
        }

        val msg = findViewById<TextView>(R.id.messageButton)
        msg.setOnClickListener {
            val intent = Intent(this, dmscreen::class.java)
            startActivity(intent)
            finish()
        }

        val back = findViewById<ImageView>(R.id.backIcon)
        back.setOnClickListener {
            val intent = Intent(this, feedpage::class.java)
            startActivity(intent)
            finish()
        }

        val dp = findViewById<ImageView>(R.id.profilePic)
        dp.setOnClickListener {
            val intent = Intent(this, storyview::class.java)
            startActivity(intent)
            finish()
        }

    }
}