package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
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

        back.setOnClickListener {
            finish()
        }

        signup.setOnClickListener {
            startActivity(Intent(this, signup::class.java))
            finish()
        }

        loginButton.setOnClickListener {
            val url = "http://192.168.100.13/socially_app/login.php"

            val request = object : StringRequest(
                Request.Method.POST, url,
                { response ->
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {

                        val user = obj.getJSONObject("user")

                        // SAVE SESSION
                        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
                        val editor = sp.edit()
                        editor.putString("userid", user.getString("id"))
                        editor.putString("username", user.getString("username"))
                        editor.putString("email", user.getString("email"))
                        editor.apply()

                        startActivity(Intent(this, feedpage::class.java))
                        finish()

                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_LONG).show()
                    }
                },
                {
                    Toast.makeText(this, "Network Error", Toast.LENGTH_LONG).show()
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
