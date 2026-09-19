package com.example.republicsavingsapp.ui.categories

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val categoryID: Int = 0,

    val userId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val monthlyMax: Double
)