package com.example.republicsavingsapp

object CurrentUser {
    var userID: Int = -1
        private set

    var userName: String = ""
        private set

    val isLoggedIn: Boolean         // checking if logged in or out
        get() = userID != -1

    fun setUser(user: Users)        // use to log in
    {
        userID = user.userID
        userName = user.userName
    }

    fun clear()             // use to log out
    {
        userID = -1
        userName = ""
    }
}