package com.example.republicsavingsapp

import android.hardware.biometrics.BiometricManager
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val userID: String,
    val userName: String,
    val userSurname: String,
    val email: String,
    val userPassword: String,
    val currency: String, // 'ZAR', 'USD', OR 'EUR'
    val biometricEnabled: Boolean
)
