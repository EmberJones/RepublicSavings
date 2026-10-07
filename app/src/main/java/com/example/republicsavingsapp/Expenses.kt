package com.example.republicsavingsapp

data class Expenses(
    val expenseID: Int = 0,     // not needing to show the user

    val userId: Long,       // the row number of the user in the Users Table (1 user:Many expenses relationship)
    val expenseName: String,
    val expenseDescription: String? = null,         // descriptions are allowed to be null
    val expenseAmount: String,
    val expenseCategory: String,
    val includeInBudget: Boolean = true,
    val photoFilePath: String?,             // images are allowed to be null, and the images are stored locally on the phone, not in the DB
    val expenseDate: Long,

    val uploaded: Long = System.currentTimeMillis()     // when the upload expense button is clicked, don't populate
)
