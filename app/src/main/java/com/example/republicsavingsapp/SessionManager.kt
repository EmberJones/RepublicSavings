package com.example.republicsavingsapp

import android.content.Context

/* Remembers which user is logged in so session survives app being closed,
    instead of starting all over from the login screen.

    User id is saved, not password. id is read from main activity to be loaded and login screen
    is skipped
*/
object SessionManager {
    //Name of shared preferences file
    private const val PREFS_NAME = "app_prefs"
    //Key for storing user id
    private const val KEY_USER_ID = "logged_in_user_id"

    //Call after login to remember who is logged in
    fun saveUserId(context: Context, userId: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_USER_ID, userId)
            .apply() //save in background without blocking UI
    }

    //returns saved userID or -1 if not found
    fun getUserId(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_USER_ID, -1)

    }

    //Call on logout so the app remembers that no user is logged in
    fun clear (context: Context){
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_USER_ID)
            .apply()
    }
}