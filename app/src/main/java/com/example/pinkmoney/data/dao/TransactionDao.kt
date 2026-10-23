package com.example.pinkmoney.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pinkmoney.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getTransactionsList(): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE txn_hash = :hash")
    suspend fun exists(hash: String): Int

    @Query("UPDATE transactions SET category = :category WHERE merchant = :merchant")
    suspend fun updateCategoryForMerchant(
        merchant: String,
        category: String
    )
}
