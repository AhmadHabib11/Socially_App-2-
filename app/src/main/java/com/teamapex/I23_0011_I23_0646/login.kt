package com.teamapex.I23_0011_I23_0646

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        val identifier = findViewById<EditText>(R.id.username)
        val password = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<TextView>(R.id.loginbar)
        val signup = findViewById<TextView>(R.id.signup)
        val back = findViewById<ImageView>(R.id.backArrow)

        // Check if username was passed from authorization page
        val prefilledUsername = intent.getStringExtra("prefill_username")
        if (!prefilledUsername.isNullOrEmpty()) {
            identifier.setText(prefilledUsername)
            // Focus on password field since username is already filled
            password.requestFocus()
        }

        back.setOnClickListener {
            startActivity(Intent(this, authorization::class.java))
            finish()
        }

        signup.setOnClickListener {
            startActivity(Intent(this, signup::class.java))
            finish()
        }

        loginButton.setOnClickListener {
            // Validation
            if (identifier.text.toString().isEmpty()) {
                Toast.makeText(this, "Please enter username or email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.text.toString().isEmpty()) {
                Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val url = "http://192.168.10.17/socially_app/login.php"

            val pd = ProgressDialog(this)
            pd.setMessage("Logging in...")
            pd.show()

            val request = object : StringRequest(
                Request.Method.POST, url,
                { response ->
                    pd.dismiss()

                    try {
                        Log.d("LoginResponse", "Response: $response")
                        val obj = JSONObject(response)

                        if (obj.getInt("statuscode") == 200) {
                            val user = obj.getJSONObject("user")

                            // SAVE SESSION with profile picture
                            val sp = getSharedPreferences("user_session", MODE_PRIVATE)
                            val editor = sp.edit()
                            editor.putString("userid", user.getString("id"))
                            editor.putString("username", user.getString("username"))
                            editor.putString("first_name", user.getString("first_name"))
                            editor.putString("last_name", user.getString("last_name"))
                            editor.putString("email", user.getString("email"))
                            editor.putString("profile_pic", user.getString("profile_pic"))
                            editor.putBoolean("is_logged_in", true)
                            editor.apply()

                            Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

                            startActivity(Intent(this, feedpage::class.java))
                            finish()

                        } else {
                            Toast.makeText(this, obj.getString("message"), Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e("LoginError", "Parse error: ${e.message}")
                    }
                },
                { error ->
                    pd.dismiss()
                    Toast.makeText(this, "Network Error: ${error.message}", Toast.LENGTH_LONG).show()
                    Log.e("LoginError", "Network error: ${error.message}")
                }
            ) {
                override fun getParams(): MutableMap<String, String> {
                    val map = HashMap<String, String>()
                    map["identifier"] = identifier.text.toString()
                    map["password"] = password.text.toString()
                    return map
                }
            }

            Volley.newRequestQueue(this).add(request)
        }
    }
}