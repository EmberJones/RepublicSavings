package com.example.republicsavingsapp

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query

@Dao
interface ExpensesDAO {
    @Insert
    suspend fun AddExpense(expenses: Expenses): Long

    @Insert
    suspend fun AddUser(users: Users): Long

    @Query("SELECT * FROM expenses WHERE userID = :activeUserID AND expenseCategory = :category ORDER BY uploaded DESC")
    suspend fun getAllFromUserInCategory(activeUserID: Int, category: String): List<Expenses>

    @Query("SELECT * FROM expenses WHERE userID = :activeUserID ORDER BY uploaded DESC")
    suspend fun getAllFromUser(activeUserID: Int): List<Expenses>

    @Query("SELECT * FROM users ORDER BY userID ASC")
    suspend fun getAllUsers(): List<Users>
}