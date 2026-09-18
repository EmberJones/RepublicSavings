package com.example.republicsavingsapp
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.example.republicsavingsapp.ui.categories.Category

@Dao
interface CategoryDAO {
    @Insert
    suspend fun addCategory(category: Category): Long

    @Query("SELECT * FROM categories WHERE userId = :activeUserID ORDER BY categoryName ASC")
    suspend fun getAllForUser(activeUserID: Int): List<Category>
}