package com.example.republicsavingsapp

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface UserDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM users WHERE userName = :username AND userPassword = :password LIMIT 1")
    suspend fun getUserByCredentials(username: String, password: String): User?

    @Query("Select * From users Where email = :email Limit 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT userID FROM users WHERE userName = :username LIMIT 1")
    suspend fun getUserIdByUsername(username: String): Int?

    @Query("Update users set userPassword = :newPassword Where email = :email")
    suspend fun updatePassword(email: String?, newPassword: String)

}