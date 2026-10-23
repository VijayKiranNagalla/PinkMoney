package com.example.pinkmoney.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_alerts")
data class BudgetAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val timestamp: Long,
    val category: String,
    val isRead: Boolean = false
)
