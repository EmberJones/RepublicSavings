package com.example.republicsavingsapp

import android.app.Application

class RepublicSavingsApp : Application() {
    private lateinit var expensesDAO: ExpensesDAO
    private lateinit var categoryDAO: CategoryDAO
    public lateinit var expenseRepository: ExpenseRepository
    public lateinit var userRepository: UserRepository
    public lateinit var categoryRepository: CategoryRepository

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(applicationContext)
        expensesDAO = db.expensesDAO()
        categoryDAO = db.categoryDAO()
        expenseRepository = ExpenseRepository(expensesDAO)
        userRepository = UserRepository(expensesDAO)
        categoryRepository = CategoryRepository(categoryDAO)
    }
}