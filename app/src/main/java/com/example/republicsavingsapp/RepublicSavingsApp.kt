package com.example.republicsavingsapp

import android.app.Activity
import android.app.Application

class RepublicSavingsApp : Application() {
    private lateinit var expensesDAO: ExpensesDAO
    public lateinit var expenseRepository: ExpenseRepository
    public lateinit var userRepository: UserRepository

    override fun onCreate() {
        super.onCreate()
        expensesDAO = AppDatabase.getDatabase(applicationContext).ExpensesDAO()
        expenseRepository = ExpenseRepository(expensesDAO)
        userRepository = UserRepository(expensesDAO)
    }
}