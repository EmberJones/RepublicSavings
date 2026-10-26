package com.example.republicsavingsapp.ui.categories

data class Category(
    val categoryID: Int = 0,

    val userId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val monthlyMax: Double,
    val monthlyMin: Double = 0.0,
    val docId: String = ""
)