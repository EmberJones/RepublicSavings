package com.example.republicsavingsapp

object CurrentUser {
    var currency: String = ""
        private set
    var userID: Long = -1
        private set

    var userName: String = ""
        private set

    val isLoggedIn: Boolean         // checking if logged in or out
        get() = userID != -1L

    fun setUser(user: User)        // use to log in
    {
        userID = user.userID
        userName = user.userName
        currency = user.currency
    }

    fun updateCurrency(newCurrency: String) {
        currency = newCurrency
    }

    fun clear()             // use to log out
    {
        userID = -1
        userName = ""
        currency = ""
    }
}