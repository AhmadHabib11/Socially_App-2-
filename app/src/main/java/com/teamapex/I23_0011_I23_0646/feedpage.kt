package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class feedpage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.feedpage)

        val messenger = findViewById<ImageView>(R.id.messageIcon)
        val search = findViewById<ImageView>(R.id.searchIcon)
        val notif = findViewById<ImageView>(R.id.notifIcon)
        val prof = findViewById<ImageView>(R.id.profileIcon)
        val create = findViewById<ImageView>(R.id.reelsIcon)

        val cam = findViewById<ImageView>(R.id.cameraIcon)
     //   val yourstory = findViewById<ImageView>(R.id.yourstory)
//        val story1 = findViewById<ImageView>(R.id.story1)
//        val story2 = findViewById<ImageView>(R.id.story2)
//        val story3 = findViewById<ImageView>(R.id.story3)
//        val story4 = findViewById<ImageView>(R.id.story4)

      //  val postprofpic = findViewById<ImageView>(R.id.postProfilePic)

//     //   val send = findViewById<ImageView>(R.id.shareIcon)
//        send.setOnClickListener {
//            val intent = Intent(this, chatlist::class.java)
//            startActivity(intent)
//            finish()
//        }


        messenger.setOnClickListener {
            val intent = Intent(this, chatlist::class.java)
            startActivity(intent)

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
        cam.setOnClickListener {
            val intent = Intent(this, camera::class.java)
            startActivity(intent)
            finish()
        }
//        yourstory.setOnClickListener {
//            val intent = Intent(this, storyactivity::class.java)
//            startActivity(intent)
//            finish()
//        }
//        story1.setOnClickListener {
//            val intent = Intent(this, storyview::class.java)
//            startActivity(intent)
//            finish()
//        }
//        story2.setOnClickListener {
//            val intent = Intent(this, storyview::class.java)
//            startActivity(intent)
//            finish()
//        }
//        story3.setOnClickListener {
//            val intent = Intent(this, storyview::class.java)
//            startActivity(intent)
//            finish()
//        }
//        story4.setOnClickListener {
//            val intent = Intent(this, storyview::class.java)
//            startActivity(intent)
//            finish()
//        }
//        create.setOnClickListener {
//            val intent = Intent(this, createpost::class.java)
//            startActivity(intent)
//            finish()
//        }
//
//        postprofpic.setOnClickListener {
//            val intent = Intent(this, profvisitnofollow::class.java)
//            startActivity(intent)
//            finish()
//        }





    }
}