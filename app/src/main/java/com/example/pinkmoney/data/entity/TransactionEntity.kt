package com.example.pinkmoney.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["txn_hash"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val amount: Double,
    val merchant: String?,
    val timestamp: Long,
    val source: String,
    val transactionType: String,
    val rawText: String,

    @ColumnInfo(name = "txn_hash")
    val transactionHash: String
)