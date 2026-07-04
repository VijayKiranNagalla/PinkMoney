package com.example.pinkmoney.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pinkmoney.data.dao.BucketDao
import com.example.pinkmoney.data.dao.TransactionDao
import com.example.pinkmoney.data.entity.BucketEntity
import com.example.pinkmoney.data.entity.MerchantBucketMap
import com.example.pinkmoney.data.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        BucketEntity::class,
        MerchantBucketMap::class
    ],
    version = 11,
    exportSchema = false
)
abstract class PinkMoneyDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun bucketDao(): BucketDao

    companion object {

        @Volatile
        private var INSTANCE: PinkMoneyDatabase? = null

        private val defaultBucketNames = listOf(
            "Food",
            "Groceries",
            "Shopping",
            "Bills",
            "Transport",
            "Electronics",
            "Entertainment",
            "Transfers"
        )

        fun getInstance(context: Context): PinkMoneyDatabase {

            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PinkMoneyDatabase::class.java,
                    "pinkmoney_db"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { database ->
                        INSTANCE = database
                        seedDefaultBucketsIfNeeded(database)
                    }
            }
        }

        private fun seedDefaultBucketsIfNeeded(database: PinkMoneyDatabase) {
            CoroutineScope(Dispatchers.IO).launch {
                if (database.bucketDao().bucketCount() == 0) {
                    database.bucketDao().insertBuckets(
                        defaultBucketNames.map { name ->
                            BucketEntity(name = name)
                        }
                    )
                }
            }
        }
    }
}
