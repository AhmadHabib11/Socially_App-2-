package com.teamapex.I23_0011_I23_0646

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import android.content.Intent
import android.os.Handler
import android.os.Looper

class coverscreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.coverscreen)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, authorization::class.java)
            startActivity(intent)
            finish() // close coverscreen so user can’t go back to it
        }, 5000)

    }
}