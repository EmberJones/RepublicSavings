package com.example.republicsavingsapp

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

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

        if(savedInstanceState == null){
            if(CurrentUser.isLoggedIn){
                showMainApp()
            }
            else{
                supportFragmentManager.beginTransaction()
                    .replace(R.id.auth_container, Login())
                    .commit()
                showAuthFlow()
            }
        }
    }

    fun showMainApp(){
        findViewById<View>(R.id.auth_container).visibility = View.GONE
        findViewById<View>(R.id.navHostFragment).visibility = View.VISIBLE
        findViewById<View>(R.id.bottomNav).visibility = View.VISIBLE
    }

    fun showAuthFlow() {
        findViewById<View>(R.id.auth_container).visibility = View.VISIBLE
        findViewById<View>(R.id.navHostFragment).visibility = View.GONE
        findViewById<View>(R.id.bottomNav).visibility = View.GONE
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