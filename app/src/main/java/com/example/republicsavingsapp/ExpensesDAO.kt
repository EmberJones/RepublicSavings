package com.example.republicsavingsapp

interface ExpensesDAO {
    suspend fun AddExpense(expenses: Expenses): Long    // insert
    // replaces the stored values of the expense with this Firestore document id
    suspend fun updateExpense(docId: String, expense: Expenses)

    suspend fun deleteExpense(docId: String)
    suspend fun AddUser(users: User): Long  // insert

    //@Query("SELECT * FROM users WHERE userID = :userID LIMIT 1")
    suspend fun getUserById(userID: Long): User?

    //@Query("UPDATE users SET currency = :currency WHERE userID = :userID")
    suspend fun updateCurrency(userID: Long, currency: String)

    //@Query("SELECT * FROM expenses WHERE userID = :activeUserID AND expenseCategory = :category ORDER BY uploaded DESC")
    suspend fun getAllFromUserInCategory(activeUserID: Long, category: String): List<Expenses>

    // get expenses since date
    //@Query("SELECT * FROM expenses WHERE expenseDate > :date ORDER BY expenseDate ASC")
    suspend fun getAllExpensesSince(date: Long): List<Expenses>

    // get expenses up-to-date
    //@Query("SELECT * FROM expenses WHERE expenseDate < :date ORDER BY expenseDate DESC")
    suspend fun getAllExpensesUpTo(date: Long): List<Expenses>

    // get expenses between 2 dates
    //@Query("SELECT * FROM expenses WHERE expenseDate > :firstDate AND expenseDate < :lastDate ORDER BY expenseDate ASC")
    suspend fun getAllExpensesBetween(firstDate: Long, lastDate: Long): List<Expenses>

    // Only expenses are totalled (never income). With budgetedOnly = true, expenses the user excluded from the budget are left out too.
    suspend fun getTotalsByCategory(activeUserID: Long, startDate: Long, endDate: Long, budgetedOnly: Boolean = false): List<CategoryTotal>
    //@Query("SELECT * FROM expenses WHERE userID = :activeUserID ORDER BY uploaded DESC")
    suspend fun getAllFromUser(activeUserID: Long): List<Expenses>

    //@Query("SELECT * FROM users ORDER BY userID ASC")
    suspend fun getAllUsers(): List<User>
}