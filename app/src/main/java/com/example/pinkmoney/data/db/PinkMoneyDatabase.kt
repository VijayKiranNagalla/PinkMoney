package com.example.pinkmoney.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pinkmoney.data.dao.TransactionDao
import com.example.pinkmoney.data.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PinkMoneyDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {

        @Volatile
        private var INSTANCE: PinkMoneyDatabase? = null

        fun getInstance(context: Context): PinkMoneyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PinkMoneyDatabase::class.java,
                    "pinkmoney_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
