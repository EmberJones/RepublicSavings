package com.example.republicsavingsapp

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController

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
        navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            val destinationId = when (item.itemId) {
                R.id.nav_home -> R.id.homeFragment
                R.id.nav_wallet -> R.id.expenseFragment
                R.id.nav_add -> R.id.addTransactionFragment
                R.id.nav_rewards -> R.id.rewardsFragment
                R.id.nav_categories -> R.id.categoriesFragment
                R.id.nav_settings -> R.id.settingsFragment
                else -> return@setOnItemSelectedListener false
            }
            if (navController.currentDestination?.id != destinationId) {
                navController.navigate(destinationId)
            }
            true

        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val tabId = when (destination.id) {
                R.id.homeFragment -> R.id.nav_home
                R.id.expenseFragment -> R.id.nav_wallet
                R.id.addTransactionFragment -> R.id.nav_add
                R.id.rewardsFragment -> R.id.nav_rewards
                R.id.categoriesFragment -> R.id.nav_categories
                R.id.settingsFragment -> R.id.nav_settings
                else -> null
            }
            tabId?.let { bottomNav.menu.findItem(it)?.isChecked = true }
        }

        val savedUserID = SessionManager.getUserId(this)

        if (CurrentUser.isLoggedIn) {
            showMainApp()
        } else if (savedUserID != -1L) {
            showAuthFlow() //keep home hidden while saved user loads
            restoreSession(savedUserID)
        } else {
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.auth_container, Login())
                    .commit()
            }
            showAuthFlow()
        }
    }

    fun showMainApp() {
        findViewById<View>(R.id.auth_container).visibility = View.GONE
        findViewById<View>(R.id.navHostFragment).visibility = View.VISIBLE
        findViewById<View>(R.id.bottomNav).visibility = View.VISIBLE
    }

    fun onLoginSuccess() {
        showMainApp()
        navController.navigate(R.id.homeFragment, null,
            NavOptions.Builder().setPopUpTo(R.id.homeFragment, true).build())
    }

    fun showAuthFlow() {
        findViewById<View>(R.id.auth_container).visibility = View.VISIBLE
        findViewById<View>(R.id.navHostFragment).visibility = View.GONE
        findViewById<View>(R.id.bottomNav).visibility = View.GONE
    }

    //Calls session manager on startup when an id is saved
    //Loads user from database or loads login screen
    private fun restoreSession(userId: Long) {
        val app = application as RepublicSavingsApp

        lifecycleScope.launch {
            //Look up user by saved ID
            try {
                val user = withContext(Dispatchers.IO) {
                    app.userRepository.getUserById(userId)
                }
                if(user != null){
                    //only ask for phone lock if user has opted in and their phone has it
                    if(user.biometricEnabled && PasskeyAuth.isAvailable(this@MainActivity)){
                        PasskeyAuth.prompt(
                            this@MainActivity, "Unlock",
                                    //set current user after passkey check is passed
                                    onSuccess = { CurrentUser.setUser(user); onLoginSuccess() },
                            //if passkey check fails, log user out
                            onFailure = {
                                supportFragmentManager.beginTransaction()
                                    .replace(R.id.auth_container, Login())
                                    .commit()
                                showAuthFlow()
                            }
                        )
                    } else {
                        //no phone lock needed
                        //user found, start up app from home screen
                        CurrentUser.setUser(user)
                        onLoginSuccess() //shows home screen and reloads it for current user
                    }
                } else {
                    //saved id does not match any user in database
                    SessionManager.clear(this@MainActivity)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.auth_container, Login())
                        .commit()
                    showAuthFlow()
                }
            } catch (e: Exception) {
                //in the event of no internet, login shows but saves ID for next login
                supportFragmentManager.beginTransaction()
                    .replace(R.id.auth_container, Login())
                    .commit()
            }
        }
    }

    //Log current user out
    fun logout() {
        Firebase.auth.signOut()
        CurrentUser.clear()
        SessionManager.clear(this)

        supportFragmentManager.beginTransaction()
            .replace(R.id.auth_container, Login())
            .commit()
        showAuthFlow()
    }


}