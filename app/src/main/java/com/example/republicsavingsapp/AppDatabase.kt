package com.example.republicsavingsapp
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver

@Database(
    entities = [User::class, Expenses::class],
    version = 2, //was 1
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDAO(): UserDAO
    abstract fun expensesDAO(): ExpensesDAO

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase{
            return Instance ?: synchronized(this){
                Room.databaseBuilder<AppDatabase>(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "user_database"
                )
                    .setDriver(AndroidSQLiteDriver())
                    .build()
                    .also { Instance = it }

            }
        }
    }
}