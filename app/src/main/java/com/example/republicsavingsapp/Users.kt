package com.example.republicsavingsapp

import android.hardware.biometrics.BiometricManager

data class User(
    val userID: Long = 0,
    val userName: String,
    val userSurname: String,
    val email: String,
    val userPassword: String,
    val currency: String, // 'ZAR', 'USD', OR 'EUR'
    val biometricEnabled: Boolean
)
