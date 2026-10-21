package com.example.republicsavingsapp.ui.categories

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val categoryID: Int = 0,

    val userId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val monthlyMax: Double
)
