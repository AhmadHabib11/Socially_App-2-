package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class createpost : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.createpost)

        val next = findViewById<TextView>(R.id.nextBtn)
        next.setOnClickListener {
            val intent = Intent(this, feedpage::class.java)
            startActivity(intent)
            finish()

        }

        val cancel = findViewById<TextView>(R.id.cancelBtn)
        cancel.setOnClickListener {
            val intent = Intent(this, feedpage::class.java)
            startActivity(intent)
            finish()

        }


        // this is an implicit intent
        val lib = findViewById<TextView>(R.id.library)
        lib.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivity(Intent.createChooser(intent, "Select Picture"))
        }


    }
}