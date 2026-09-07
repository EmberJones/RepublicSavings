package com.example.republicsavingsapp

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "users")
data class Users(
    @PrimaryKey(autoGenerate = true)
    val userID: Int = 0,
    val userName: String,
    val userPassword: String
    //maybe add someway to automatically hash username? Will look into this
)
