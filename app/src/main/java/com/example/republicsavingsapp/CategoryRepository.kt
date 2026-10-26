package com.example.republicsavingsapp
import com.example.republicsavingsapp.ui.categories.Category

class CategoryRepository(private val categoryDAO: CategoryDAO) {

    /**
     * Creates a category. [monthlyMin] is the minimum monthly spending goal (0 means none) and can
     * never be above [monthlyMax].
     */
    suspend fun addCategory(userId: Long, name: String, icon: String, monthlyMax: Double, monthlyMin: Double = 0.0): Long {
        requireValidGoals(monthlyMin, monthlyMax)
        return categoryDAO.addCategory(
            Category(userId = userId, categoryName = name, categoryIcon = icon, monthlyMax = monthlyMax, monthlyMin = monthlyMin)
        )
    }

    suspend fun getCategoriesForUser(userId: Long): List<Category> = categoryDAO.getAllForUser(userId)


    suspend fun updateCategory(updated: Category) {
        require(updated.docId.isNotBlank()) { "Only a category read from the database can be updated" }
        requireValidGoals(updated.monthlyMin, updated.monthlyMax)
        categoryDAO.updateCategory(updated.docId, updated)
    }

    suspend fun deleteCategory(category: Category) {
        require(category.docId.isNotBlank()) { "Only a category read from the database can be deleted" }
        categoryDAO.deleteCategory(category.docId)
    }

    private fun requireValidGoals(monthlyMin: Double, monthlyMax: Double) {
        require(monthlyMin >= 0.0) { "The minimum goal cannot be negative" }
        require(monthlyMax > 0.0) { "The maximum goal must be above zero" }
        require(monthlyMin <= monthlyMax) { "The minimum goal cannot be above the maximum" }
    }
}