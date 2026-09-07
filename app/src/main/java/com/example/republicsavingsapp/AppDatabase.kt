package com.example.republicsavingsapp
import androidx.room3.Database
import androidx.room3.RoomDatabase
import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver

@Database(
    entities = [Expenses::class, Users::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ExpensesDAO(): ExpensesDAO

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            Instance ?: synchronized(this) {
                Room.databaseBuilder<AppDatabase>(
                    context.applicationContext,
                    "expenses_database"
                )
                    .setDriver(AndroidSQLiteDriver())
                    .build()
                    .also { Instance = it }
            }
    }
}