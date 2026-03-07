package com.example.pinkmoney.ui.transactions

import androidx.lifecycle.ViewModel
import com.example.pinkmoney.data.db.PinkMoneyDatabase
import com.example.pinkmoney.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

class TransactionViewModel(
    database: PinkMoneyDatabase
) : ViewModel() {

    val transactions: Flow<List<TransactionEntity>> =
        database.transactionDao()
            .getAllTransactions()
}