package com.example.republicsavingsapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.republicsavingsapp.ui.categories.Category

@Database(
    entities = [User::class, Expenses::class, Category::class],
    version = 4, // was 3, added includeInBudget to Expenses
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDAO(): UserDAO
    abstract fun expensesDAO(): ExpensesDAO
    abstract fun categoryDAO(): CategoryDAO

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "user_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
