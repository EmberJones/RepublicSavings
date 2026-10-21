package com.example.republicsavingsapp

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["userName"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val userID: Long = 0,
    val userName: String,
    val userSurname: String,
    val email: String,
    val userPassword: String,
    val currency: String, // 'ZAR', 'USD', OR 'EUR'
    val biometricEnabled: Boolean
)
