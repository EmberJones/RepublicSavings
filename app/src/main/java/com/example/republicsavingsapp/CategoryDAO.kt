package com.example.republicsavingsapp
import com.example.republicsavingsapp.ui.categories.Category

interface CategoryDAO {
    suspend fun addCategory(category: Category): Long

    suspend fun getAllForUser(activeUserID: Long): List<Category>
}