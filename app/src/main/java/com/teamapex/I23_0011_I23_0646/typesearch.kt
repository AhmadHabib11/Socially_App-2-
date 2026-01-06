package com.teamapex.I23_0011_I23_0646

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class typesearch : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var clearIcon: ImageView
    private lateinit var backIcon: ImageView
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var loadingView: LinearLayout
    private lateinit var emptyView: TextView
    private lateinit var filterAll: TextView
    private lateinit var filterFollowers: TextView
    private lateinit var filterFollowing: TextView

    private lateinit var adapter: UserSearchAdapter
    private var currentUserId: String = ""
    private var currentFilter: String = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.typesearch)

        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        currentUserId = sp.getString("userid", "") ?: ""

        searchEditText = findViewById(R.id.searchEditText)
        clearIcon = findViewById(R.id.clearIcon)
        backIcon = findViewById(R.id.backIcon)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        loadingView = findViewById(R.id.loadingView)
        emptyView = findViewById(R.id.emptyView)
        filterAll = findViewById(R.id.filterAll)
        filterFollowers = findViewById(R.id.filterFollowers)
        filterFollowing = findViewById(R.id.filterFollowing)

        adapter = UserSearchAdapter(this, mutableListOf()) { user ->
            onUserClick(user)
        }
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
        searchResultsRecyclerView.adapter = adapter

        emptyView.visibility = View.VISIBLE
        searchResultsRecyclerView.visibility = View.GONE

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()

                if (query.isEmpty()) {
                    clearIcon.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    searchResultsRecyclerView.visibility = View.GONE
                    emptyView.text = "Start typing to search"
                } else {
                    clearIcon.visibility = View.VISIBLE
                    searchUsers(query)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        clearIcon.setOnClickListener {
            searchEditText.text.clear()
        }

        backIcon.setOnClickListener {
            val intent = Intent(this, searchpage::class.java)
            startActivity(intent)
            finish()
        }

        filterAll.setOnClickListener {
            setFilter("all")
        }

        filterFollowers.setOnClickListener {
            setFilter("followers")
        }

        filterFollowing.setOnClickListener {
            setFilter("following")
        }

        setupBottomNavigation()
    }

    private fun setFilter(filter: String) {
        currentFilter = filter

        when(filter) {
            "all" -> {
                filterAll.setBackgroundResource(R.drawable.filter_button_active)
                filterAll.setTextColor(getColor(R.color.white))
                filterFollowers.setBackgroundResource(R.drawable.filter_button_inactive)
                filterFollowers.setTextColor(getColor(R.color.gray_text))
                filterFollowing.setBackgroundResource(R.drawable.filter_button_inactive)
                filterFollowing.setTextColor(getColor(R.color.gray_text))
            }
            "followers" -> {
                filterAll.setBackgroundResource(R.drawable.filter_button_inactive)
                filterAll.setTextColor(getColor(R.color.gray_text))
                filterFollowers.setBackgroundResource(R.drawable.filter_button_active)
                filterFollowers.setTextColor(getColor(R.color.white))
                filterFollowing.setBackgroundResource(R.drawable.filter_button_inactive)
                filterFollowing.setTextColor(getColor(R.color.gray_text))
            }
            "following" -> {
                filterAll.setBackgroundResource(R.drawable.filter_button_inactive)
                filterAll.setTextColor(getColor(R.color.gray_text))
                filterFollowers.setBackgroundResource(R.drawable.filter_button_inactive)
                filterFollowers.setTextColor(getColor(R.color.gray_text))
                filterFollowing.setBackgroundResource(R.drawable.filter_button_active)
                filterFollowing.setTextColor(getColor(R.color.white))
            }
        }

        val query = searchEditText.text.toString().trim()
        if (query.isNotEmpty()) {
            searchUsers(query)
        }
    }

    private fun searchUsers(query: String) {
        loadingView.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        searchResultsRecyclerView.visibility = View.GONE

        val url = "http://172.15.44.21/socially_app/search_users.php?query=$query&current_user_id=$currentUserId&filter=$currentFilter"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                loadingView.visibility = View.GONE

                try {
                    val obj = JSONObject(response)

                    if (obj.getInt("statuscode") == 200) {
                        val usersArray = obj.getJSONArray("users")
                        val users = mutableListOf<MyData>()

                        for (i in 0 until usersArray.length()) {
                            val userObj = usersArray.getJSONObject(i)

                            users.add(
                                MyData(
                                    id = userObj.getString("id"),
                                    name = userObj.getString("username"),
                                    firstName = userObj.getString("first_name"),
                                    lastName = userObj.getString("last_name"),
                                    email = "", // Not returned in search
                                    dp = userObj.getString("profile_pic"),
                                    followStatus = userObj.getString("follow_status")
                                )
                            )
                        }

                        if (users.isEmpty()) {
                            emptyView.visibility = View.VISIBLE
                            emptyView.text = "No users found"
                            searchResultsRecyclerView.visibility = View.GONE
                        } else {
                            adapter.updateUsers(users)
                            searchResultsRecyclerView.visibility = View.VISIBLE
                            emptyView.visibility = View.GONE
                        }
                    } else {
                        emptyView.visibility = View.VISIBLE
                        emptyView.text = "Error: ${obj.getString("message")}"
                    }
                } catch (e: Exception) {
                    Log.e("TypeSearch", "Error: ${e.message}")
                    emptyView.visibility = View.VISIBLE
                    emptyView.text = "Error loading results"
                }
            },
            { error ->
                loadingView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
                emptyView.text = "Network error"
                Log.e("TypeSearch", "Network error: ${error.message}")
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun onUserClick(user: MyData) {
        val intent = if (user.followStatus == "accepted") {
            Intent(this, profilevisitfollowing::class.java)
        } else {
            Intent(this, profvisitnofollow::class.java)
        }

        intent.putExtra("user_id", user.id)
        startActivity(intent)
        finish()
    }

    private fun setupBottomNavigation() {
        val homeIcon = findViewById<ImageView>(R.id.homeIcon)
        val searchIcon = findViewById<ImageView>(R.id.searchIcon)
        val reelsIcon = findViewById<ImageView>(R.id.reelsIcon)
        val notifIcon = findViewById<ImageView>(R.id.notifIcon)
        val profileIcon = findViewById<ImageView>(R.id.profileIcon)

        homeIcon.setOnClickListener {
            startActivity(Intent(this, feedpage::class.java))
            finish()
        }

        searchIcon.setOnClickListener {
            startActivity(Intent(this, searchpage::class.java))
            finish()
        }

        reelsIcon.setOnClickListener {
            startActivity(Intent(this, createpost::class.java))
            finish()
        }

        notifIcon.setOnClickListener {
            startActivity(Intent(this, notifications::class.java))
            finish()
        }

        profileIcon.setOnClickListener {
            startActivity(Intent(this, profile::class.java))
            finish()
        }
    }
}