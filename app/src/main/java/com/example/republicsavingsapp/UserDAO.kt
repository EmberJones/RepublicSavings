package com.example.republicsavingsapp

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface UserDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Query("Select * From users Where username = :username and password = :password Limit 1")
    suspend fun getUserByCredentials(username: String, password: String): User?

    @Query("Select * From users Where email = :email Limit 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("Select * From users Where userID = :userID Limit 1")
    suspend fun getUserIdByUsername(username: String): Long?

    @Query("Update users set password = :newPassword Where email = :email")
    suspend fun updatePassword(email: String, newPassword: String)

}