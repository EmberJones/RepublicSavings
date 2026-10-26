package com.example.republicsavingsapp

class ExpenseRepository(private val expensesDAO: ExpensesDAO) {
    suspend fun addExpense(
        name: String, amount: String, category: String,
        includeInBudget: Boolean = true,
        description: String? = null, photoFilePath: String? = null, date: Long = System.currentTimeMillis(),
        type: TransactionType = TransactionType.EXPENSE
    ): Long {
        val newExpense = Expenses(
            userId = CurrentUser.userID,
            expenseName = name,
            expenseDescription = description,
            expenseAmount = amount,
            expenseCategory = category,
            includeInBudget = includeInBudget,
            expenseDate = date,
            photoFilePath = photoFilePath,
            type = type
        )
        return expensesDAO.AddExpense(newExpense)
    }
    suspend fun updateExpense(updated: Expenses) {
        require(updated.docId.isNotBlank()) { "Only an expense read from the database can be updated" }
        expensesDAO.updateExpense(updated.docId, updated)
    }

    suspend fun deleteExpense(expense: Expenses) {
        require(expense.docId.isNotBlank()) { "Only an expense read from the database can be deleted" }
        expensesDAO.deleteExpense(expense.docId)
    }
    suspend fun getExpenses(optionalCategoryFilter: String? = null): List<Expenses> {
        return if (optionalCategoryFilter != null) {
            expensesDAO.getAllFromUserInCategory(CurrentUser.userID, optionalCategoryFilter)
        } else
        {
            expensesDAO.getAllFromUser(CurrentUser.userID)
        }
    }

    suspend fun getExpensesUpTo(date: Long): List<Expenses> {
        return expensesDAO.getAllExpensesUpTo(date)
    }

    suspend fun getAllExpensesAfter(date: Long): List<Expenses> {
        return expensesDAO.getAllExpensesSince(date)
    }

    suspend fun getAllExpensesBetween(startDate: Long, endDate: Long): List<Expenses>
    {
        return expensesDAO.getAllExpensesBetween(startDate, endDate)
    }

    suspend fun getAllUniqueCategories(): List<String> {
        return expensesDAO.getAllFromUser(CurrentUser.userID).map { it.expenseCategory }.distinct()
    }

    suspend fun getTotalsByCategory(startDate: Long, endDate: Long, budgetedOnly: Boolean = false): List<CategoryTotal> {
        return expensesDAO.getTotalsByCategory(CurrentUser.userID, startDate, endDate, budgetedOnly)
    }
}