package com.example.pinkmoney.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pinkmoney.data.dao.TransactionDao
import com.example.pinkmoney.data.dao.BucketDao
import com.example.pinkmoney.data.entity.TransactionEntity
import com.example.pinkmoney.data.entity.BucketEntity
import com.example.pinkmoney.data.entity.MerchantBucketMap

@Database(
    entities = [
        TransactionEntity::class,
        BucketEntity::class,
        MerchantBucketMap::class
    ],
    version = 8,
    exportSchema = false
)
abstract class PinkMoneyDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun bucketDao(): BucketDao

    companion object {

        @Volatile
        private var INSTANCE: PinkMoneyDatabase? = null

        fun getInstance(context: Context): PinkMoneyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PinkMoneyDatabase::class.java,
                    "pinkmoney_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}