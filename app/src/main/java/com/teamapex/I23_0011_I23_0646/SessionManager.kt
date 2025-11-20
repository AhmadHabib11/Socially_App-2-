package com.teamapex.I23_0011_I23_0646

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        // THESE KEYS MUST MATCH EXACTLY WHAT YOU SAVE IN login.kt
        private const val KEY_USER_ID = "userid"           // ← Changed from "user_id" to "userid"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_FIRST_NAME = "first_name"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_PROFILE_PIC = "profile_pic"
        private const val KEY_IS_LOGGED_IN = "is_logged_in" // ← Changed from "is_logged_in" to "is_logged_in"
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    fun getEmail(): String? {
        return prefs.getString(KEY_EMAIL, null)
    }

    fun getFirstName(): String? {
        return prefs.getString(KEY_FIRST_NAME, null)
    }

    fun getLastName(): String? {
        return prefs.getString(KEY_LAST_NAME, null)
    }

    fun getProfilePic(): String? {
        return prefs.getString(KEY_PROFILE_PIC, null)
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun clearSession() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }

    // Add this debug method to see what's stored
    fun debugSession() {
        println("DEBUG SessionManager:")
        println("userid: ${prefs.getString("userid", "NOT_FOUND")}")
        println("is_logged_in: ${prefs.getBoolean("is_logged_in", false)}")
        println("username: ${prefs.getString("username", "NOT_FOUND")}")
    }
}