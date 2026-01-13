package com.example.pinkmoney.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pinkmoney.data.entity.TransactionEntity

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(
        transaction: TransactionEntity
    )

    @Query(
        "SELECT * FROM transactions ORDER BY timestamp DESC"
    )
    suspend fun getAllTransactions(): List<TransactionEntity>
}
