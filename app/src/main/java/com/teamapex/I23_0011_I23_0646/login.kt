package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        val backarrow = findViewById<ImageView>(R.id.backArrow)

        backarrow.setOnClickListener {
            val intent = Intent(this, signup::class.java)
            startActivity(intent)
            finish()
        }

        val login = findViewById< TextView>(R.id.loginbar)

        login.setOnClickListener {
            val intent = Intent(this, feedpage::class.java)
            startActivity(intent)
            finish()
        }

        val signup = findViewById< TextView>(R.id.signup)

        signup.setOnClickListener {
            val intent = Intent(this, signup::class.java)
            startActivity(intent)
            finish()
        }

    }
}

