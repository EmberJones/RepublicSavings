package com.example.republicsavingsapp

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp

class RepublicSavingsApp : Application() {
    private lateinit var userDAO: UserDAO
    private lateinit var expensesDAO: ExpensesDAO
    private lateinit var categoryDAO: CategoryDAO

    lateinit var expenseRepository: ExpenseRepository
    lateinit var userRepository: UserRepository
    lateinit var categoryRepository: CategoryRepository

    override fun onCreate() {
        super.onCreate()

        val app = FirebaseApp.getInstance()
        Log.d("Firebase", "project = " + app.options.projectId)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val savedThemeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedThemeMode)

        val db = AppDatabase.getDatabase(applicationContext)
        userDAO = db.userDAO()
        expensesDAO = db.expensesDAO()
        categoryDAO = db.categoryDAO()

        expenseRepository = ExpenseRepository(expensesDAO)
        userRepository = UserRepository(userDAO, expensesDAO)
        categoryRepository = CategoryRepository(categoryDAO)
    }
}
