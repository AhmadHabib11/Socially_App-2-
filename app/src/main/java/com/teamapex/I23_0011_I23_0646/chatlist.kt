package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class chatlist : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chatlist)

        val backarrow = findViewById<ImageView>(R.id.backArrow)
        val chat1 = findViewById<LinearLayout>(R.id.chat1)
        val chat2 = findViewById<LinearLayout>(R.id.chat2)
        val chat3 = findViewById<LinearLayout>(R.id.chat3)
        val chat4 = findViewById<LinearLayout>(R.id.chat4)
        val chat5 = findViewById<LinearLayout>(R.id.chat5)
        val chat6 = findViewById<LinearLayout>(R.id.chat6)

        val bottomcam = findViewById<ImageView>(R.id.cambottom)
        val cam1 = findViewById<ImageView>(R.id.camera1)
        val cam2 = findViewById<ImageView>(R.id.camera2)
        val cam3 = findViewById<ImageView>(R.id.camera3)
        val cam4 = findViewById<ImageView>(R.id.camera4)
        val cam5 = findViewById<ImageView>(R.id.camera5)
        val cam6 = findViewById<ImageView>(R.id.camera6)


        backarrow.setOnClickListener {
            val intent = Intent(this, feedpage ::class.java)
            startActivity(intent)
            finish()
        }
        chat1.setOnClickListener {
            val intent = Intent(this, dmscreen ::class.java)
            startActivity(intent)
        }
        chat2.setOnClickListener {
            val intent = Intent(this, dmscreen ::class.java)
            startActivity(intent)
        }
        chat3.setOnClickListener {
            val intent = Intent(this, dmscreen ::class.java)
            startActivity(intent)
        }
        chat4.setOnClickListener {
            val intent = Intent(this, dmscreen ::class.java)
            startActivity(intent)
        }
        chat5.setOnClickListener {
            val intent = Intent(this, dmscreen ::class.java)
            startActivity(intent)
        }
        chat6.setOnClickListener {
            val intent = Intent(this, dmscreen ::class.java)
            startActivity(intent)
        }



        bottomcam.setOnClickListener {
            val intent = Intent(this, camera ::class.java)
            startActivity(intent)
        }
        cam1.setOnClickListener {
            val intent = Intent(this, camera ::class.java)
            startActivity(intent)
        }
        cam2.setOnClickListener {
            val intent = Intent(this, camera ::class.java)
            startActivity(intent)
        }
        cam3.setOnClickListener {
            val intent = Intent(this, camera ::class.java)
            startActivity(intent)
        }
        cam4.setOnClickListener {
            val intent = Intent(this, camera ::class.java)
            startActivity(intent)
        }
        cam5.setOnClickListener {
            val intent = Intent(this, camera ::class.java)
            startActivity(intent)
        }
        cam6.setOnClickListener {
            val intent = Intent(this, camera ::class.java)
            startActivity(intent)
        }




    }
}