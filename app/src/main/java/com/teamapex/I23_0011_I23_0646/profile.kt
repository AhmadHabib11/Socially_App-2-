package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class profile : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile)



        val search = findViewById<ImageView>(R.id.searchIcon)
        val notif = findViewById<ImageView>(R.id.notifIcon)
        val prof = findViewById<ImageView>(R.id.profileIcon)
        val create = findViewById<ImageView>(R.id.reelsIcon)
        val homeicon = findViewById<ImageView>(R.id.homeIcon)



        homeicon.setOnClickListener {
            val intent = Intent(this, feedpage ::class.java)
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
        create.setOnClickListener {
            val intent = Intent(this, createpost::class.java)
            startActivity(intent)
            finish()
        }

        val dp = findViewById<ImageView>(R.id.profilePic)
        dp.setOnClickListener {
            val intent = Intent(this, storyactivity::class.java)
            startActivity(intent)
            finish()
        }

        val editprof = findViewById<TextView>(R.id.editProfileBar)
        editprof.setOnClickListener {
            val intent = Intent(this, editprofile::class.java)
            startActivity(intent)
            finish()
        }


        val h1 = findViewById<ImageView>(R.id.h1)
        val h2 = findViewById<ImageView>(R.id.h2)
        val h3 = findViewById<ImageView>(R.id.h3)
        val newstory = findViewById<ImageView>(R.id.newstory)

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
        newstory.setOnClickListener {
            val intent = Intent(this, storyupload::class.java)
            startActivity(intent)
            finish()
        }


    }
}