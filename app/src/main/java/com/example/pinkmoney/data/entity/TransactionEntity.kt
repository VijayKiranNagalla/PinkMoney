package com.example.pinkmoney.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(

    @field:PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val amount: Double,

    val merchant: String?,

    val timestamp: Long,

    val source: String,   // "UPI" or "SMS"

    val rawText: String
)