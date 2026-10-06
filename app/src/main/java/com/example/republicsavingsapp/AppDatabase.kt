package com.example.republicsavingsapp

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore

class AppDatabase private constructor() {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val userDAOImpl: UserDAO by lazy { FirestoreUserDAO(firestore) }
    private val expensesDAOImpl: ExpensesDAO by lazy { FirestoreExpensesDAO(firestore) }
    private val categoryDAOImpl: CategoryDAO by lazy { FirestoreCategoryDAO(firestore) }

    fun userDAO(): UserDAO = userDAOImpl
    fun expensesDAO(): ExpensesDAO = expensesDAOImpl
    fun categoryDAO(): CategoryDAO = categoryDAOImpl

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Instance ?: AppDatabase().also { Instance = it }
            }
        }
    }
}
