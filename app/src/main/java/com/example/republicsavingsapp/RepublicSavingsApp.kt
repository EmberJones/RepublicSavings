package com.example.republicsavingsapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class RepublicSavingsApp : Application() {
    private lateinit var expensesDAO: ExpensesDAO
    private lateinit var categoryDAO: CategoryDAO
    public lateinit var expenseRepository: ExpenseRepository
    public lateinit var userRepository: UserRepository
    public lateinit var categoryRepository: CategoryRepository

    override fun onCreate() {
        super.onCreate()

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val savedThemeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedThemeMode)

        val db = AppDatabase.getDatabase(applicationContext)
        expensesDAO = db.expensesDAO()
        categoryDAO = db.categoryDAO()
        expenseRepository = ExpenseRepository(expensesDAO)
        userRepository = UserRepository(expensesDAO)
        categoryRepository = CategoryRepository(categoryDAO)
    }
}