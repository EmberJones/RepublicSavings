package com.example.republicsavingsapp

object CurrentUser {
    var userID: Int = -1
        private set

    var userName: String = ""
        private set

    var currency: String = "ZAR"
        private set

    val isLoggedIn: Boolean
        get() = userID != -1

    fun setUser(user: User) {
        userID = user.userID
        userName = user.userName
        currency = user.currency
    }

    fun updateCurrency(newCurrency: String) {
        currency = newCurrency
    }

    fun clear() {
        userID = -1
        userName = ""
        currency = "ZAR"
    }
}