package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class authorization : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.authorization)

        val createAccountbtn = findViewById<TextView>(R.id.signupText)

        createAccountbtn.setOnClickListener {
            val intent = Intent(this, signup::class.java)
            startActivity(intent)
            finish()
        }

        val switchaccs = findViewById<TextView>(R.id.switchaccounts)

        switchaccs.setOnClickListener {
            val intent = Intent(this, login::class.java)
            startActivity(intent)
            finish()
        }

        val loginb = findViewById<TextView>(R.id.loginbutton)

        loginb.setOnClickListener {
            val intent = Intent(this, feedpage::class.java)
            startActivity(intent)
            finish()
        }


    }
}