package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class editprofile : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.editprofile)

        val cancel = findViewById<TextView>(R.id.cancelBtn)
        cancel.setOnClickListener {
            val intent = Intent(this, profile::class.java)
            startActivity(intent)
            finish()

        }

      //  val done = findViewById<TextView>(R.id.doneBtn)
//        done.setOnClickListener {
//            val intent = Intent(this, profile::class.java)
//            startActivity(intent)
//            finish()
//
//        }

        val changedp = findViewById<TextView>(R.id.changePhoto)
        changedp.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivity(intent)  // Just opens gallery
        }




    }
}