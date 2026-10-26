package com.example.republicsavingsapp

data class Expenses(
    val expenseID: Int = 0,     // not needing to show the user

    val userId: Long,       // the row number of the user in the Users Table (1 user:Many expenses relationship)
    val expenseName: String,
    val expenseDescription: String? = null,         // descriptions are allowed to be null
    val expenseAmount: String,
    val expenseCategory: String,
    val includeInBudget: Boolean = true,
    val photoFilePath: String?,             // the photo is stored as text inside the Firestore document; null when there is none
    val expenseDate: Long,                  // date AND time of the transaction, in epoch milliseconds

    val uploaded: Long = System.currentTimeMillis(),     // when the upload expense button is clicked, don't populate

    val type: TransactionType = TransactionType.EXPENSE, // income is money in and never counts toward a category budget
    val docId: String = ""                  // Firestore document id, filled in when read; needed to edit or delete
)