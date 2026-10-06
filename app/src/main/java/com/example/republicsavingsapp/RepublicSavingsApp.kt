package com.example.republicsavingsapp

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth

class RepublicSavingsApp : Application() {
    private lateinit var userDAO: UserDAO
    private lateinit var expensesDAO: ExpensesDAO
    private lateinit var categoryDAO: CategoryDAO

    public lateinit var expenseRepository: ExpenseRepository
    public lateinit var userRepository: UserRepository
    public lateinit var categoryRepository: CategoryRepository

    override fun onCreate() {
        super.onCreate()

        val app = FirebaseApp.getInstance()
        Log.d("Firebase", "project = " + app.options.projectId)

        Firebase.auth.signInAnonymously()
            .addOnSuccessListener { Log.d("Firebase", "Anon id = " + it.user?.uid) }
            .addOnFailureListener { Log.d("Firebase", "sign in failed" + it) }

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
