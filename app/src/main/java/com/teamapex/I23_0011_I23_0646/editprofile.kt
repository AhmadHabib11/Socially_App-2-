package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.TextView
import android.widget.Toast
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

        val changedp = findViewById<TextView>(R.id.changePhoto)
        changedp.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivity(intent)  // Just opens gallery
        }

        val signout = findViewById<TextView>(R.id.signoutBtn)
        signout.setOnClickListener {
            handleLogout()
        }
    }

    private fun handleLogout() {
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        val editor = sp.edit()

        // ONLY clear the logged-in status, NOT the "last_" data
        editor.putBoolean("is_logged_in", false)

        // Clear current session data (but keep last_username, last_profile_pic, etc.)
        editor.remove("userid")
        editor.remove("username")
        editor.remove("first_name")
        editor.remove("last_name")
        editor.remove("email")
        editor.remove("profile_pic")

        // NOTE: We DO NOT remove last_username, last_first_name, last_last_name, last_profile_pic
        // So the authorization page can still show the last logged-in user

        editor.apply()

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        // Go to login page
        val intent = Intent(this, login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}