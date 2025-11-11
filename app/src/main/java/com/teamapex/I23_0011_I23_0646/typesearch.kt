package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class typesearch : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.typesearch)

        val cross = findViewById<ImageView>(R.id.clearIcon)
        val profile = findViewById<LinearLayout>(R.id.profilesearch)

        cross.setOnClickListener {
            val intent = Intent(this, searchpage ::class.java)
            startActivity(intent)
            finish()
        }
        profile.setOnClickListener {
            val intent = Intent(this, profvisitnofollow ::class.java)
            startActivity(intent)
            finish()

        }

    }
}