package com.example.republicsavingsapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.republicsavingsapp.ui.categories.Category

@Dao
interface CategoryDAO {
    @Insert
    suspend fun addCategory(category: Category): Long

    @Query("SELECT * FROM categories WHERE userId = :activeUserID ORDER BY categoryName ASC")
    suspend fun getAllForUser(activeUserID: Long): List<Category>
}
