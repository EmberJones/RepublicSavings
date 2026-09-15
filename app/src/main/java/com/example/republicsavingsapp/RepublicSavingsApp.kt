package com.example.republicsavingsapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class RepublicSavingsApp : Application() {
    private lateinit var expensesDAO: ExpensesDAO

    private lateinit var categoryDAO: CategoryDAO

    public lateinit var categoryRepository: CategoryRepository
    public lateinit var expenseRepository: ExpenseRepository
    public lateinit var userRepository: UserRepository

    override fun onCreate() {
        super.onCreate()
        expensesDAO = AppDatabase.getDatabase(applicationContext).expensesDAO()
        expenseRepository = ExpenseRepository(expensesDAO)
        userRepository = UserRepository(expensesDAO)
        categoryDAO = AppDatabase.getDatabase(applicationContext).categoryDAO()
        categoryRepository = CategoryRepository(categoryDAO)
    }
}