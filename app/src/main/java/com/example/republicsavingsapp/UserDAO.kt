package com.example.republicsavingsapp

interface UserDAO {
    suspend fun getUserByCredentials(username: String, password: String): User?

    suspend fun getUserByEmail(email: String): User?

    suspend fun getUserByEmailOrUsername(identifier: String): User?

    suspend fun getUserIdByUsername(username: String): Int?

    suspend fun updatePassword(email: String?, newPassword: String)
}
