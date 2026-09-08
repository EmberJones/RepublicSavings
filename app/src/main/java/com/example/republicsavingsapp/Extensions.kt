package com.example.republicsavingsapp

import android.app.Activity

fun Activity.expenseRepository(): ExpenseRepository = (application as RepublicSavingsApp).expenseRepository

fun Activity.userRepository(): UserRepository = (application as RepublicSavingsApp).userRepository