package com.example.republicsavingsapp
import androidx.room.RoomDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.example.republicsavingsapp.ui.categories.Category

@Database(
    entities = [User::class, Expenses::class, Category::class],
    version = 3, //was 2, added category table
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDAO(): UserDAO
    abstract fun expensesDAO(): ExpensesDAO
    abstract fun categoryDAO(): CategoryDAO
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