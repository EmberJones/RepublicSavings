package com.example.republicsavingsapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}

//    suspend fun addNewExpense()
//    {
//        val expenseRepo = expenseRepository()
//
//        expenseRepo.addExpense(name = "Shopping", category = "GROCERIES", amount = "450.00", description = "Shopping for the basics")
//    }
//
//    suspend fun getAllUsers()
//    {
//        val userRepo = userRepository()
//
//        val allUsers = userRepo.getUsers()
//
//        for (user in allUsers)
//        {
//            // looping through every user in the database BE CAREFUL
//        }
//    }