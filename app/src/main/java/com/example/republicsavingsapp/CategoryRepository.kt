package com.example.republicsavingsapp
import com.example.republicsavingsapp.ui.categories.Category

class CategoryRepository(private val categoryDAO: CategoryDAO) {
    suspend fun addCategory(userId: Int, name: String, icon: String, monthlyMax: Double): Long {
        return categoryDAO.addCategory(Category(userId = userId, categoryName = name, categoryIcon = icon, monthlyMax = monthlyMax))
    }
    suspend fun getCategoriesForUser(userId: Int): List<Category> = categoryDAO.getAllForUser(userId)
}