package com.example.republicsavingsapp

class ExpenseRepository(private val expensesDAO: ExpensesDAO) {

    suspend fun addExpense(
        name: String, amount: String, category: String,     // necessary parameters
        description: String? = null, photoFilePath: String? = null, date: Long = System.currentTimeMillis()     // not entirely necessary
    ): Long {
        val newExpense = Expenses(
            userId = CurrentUser.userID,
            expenseName = name,
            expenseDescription = description,
            expenseAmount = amount,
            expenseCategory = category,
            expensePhotoFilePath = photoFilePath,
            expenseDate = date
        )

        return expensesDAO.AddExpense(newExpense)
    }

    suspend fun getExpenses(optionalCategoryFilter: String? = null): List<Expenses> {
        return if (optionalCategoryFilter != null) {
            expensesDAO.getAllFromUserInCategory(CurrentUser.userID, optionalCategoryFilter)
        } else
        {
            expensesDAO.getAllFromUser(CurrentUser.userID)
        }
    }
}