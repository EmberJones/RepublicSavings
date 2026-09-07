package com.example.republicsavingsapp

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query

@Dao
interface ExpensesDAO {
    @Insert
    suspend fun AddExpense(expenses: Expenses): Long

    @Query("SELECT * FROM expenses WHERE userHash = :usernameHash AND expenseCategory = :category ORDER BY uploaded DESC")
    suspend fun getAllFromUserInCategory(usernameHash: String, category: String): List<Expenses>

    @Query("SELECT * FROM expenses WHERE userHash = :usernameHash ORDER BY uploaded DESC")
    suspend fun getAllFromUser(usernameHash: String): List<Expenses>

    @Query("SELECT * FROM users ORDER BY userID ASC")
    suspend fun getAllUsers(): List<Users>
}