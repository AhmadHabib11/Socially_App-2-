package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class signup : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup)


        val createAccountbtn = findViewById<TextView>(R.id.loginbutton)
        val backArrow = findViewById< ImageView>(R.id.backArrow)
        val camera = findViewById<ImageView>(R.id.camera)

        createAccountbtn.setOnClickListener {
            val intent = Intent(this, login::class.java)
            startActivity(intent)
            finish()
        }
        backArrow.setOnClickListener {
            val intent = Intent(this, login::class.java)
            startActivity(intent)
            finish()
        }

        camera.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivity(intent)  // Just opens gallery
        }

//        camera.setOnClickListener {
//            val intent = Intent(Intent.ACTION_VIEW).apply {
//                type = "image/*"
//            }
//            startActivity(intent)
//        }
//        this opens google photos


    }
}