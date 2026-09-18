package com.example.republicsavingsapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CurrentUser.setUser(User(
            userID = 1, userName = "Jonny",
            userSurname = "Test",
            email = "JonnyTest@hotmail.com",
            userPassword = "guest123",
            currency = "R",
            biometricEnabled = false
        ))
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            val destinationId = when (item.itemId) {
                R.id.nav_home -> R.id.homeFragment
                R.id.nav_wallet -> R.id.expenseFragment
                R.id.nav_add -> R.id.addTransactionFragment
                R.id.nav_categories -> R.id.categoriesFragment
                R.id.nav_settings -> R.id.settingsFragment
                else -> return@setOnItemSelectedListener false
            }
            if (navController.currentDestination?.id != destinationId) {
                navController.navigate(destinationId)
            }
            true
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