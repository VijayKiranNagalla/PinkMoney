package com.example.pinkmoney.ui.transactions

import androidx.lifecycle.ViewModel
import com.example.pinkmoney.data.db.PinkMoneyDatabase
import com.example.pinkmoney.data.entity.TransactionEntity
import com.example.pinkmoney.utils.TransactionDeduplicator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionViewModel(
    database: PinkMoneyDatabase
) : ViewModel() {

    val transactions: Flow<List<TransactionEntity>> =
        database.transactionDao()
            .getAllTransactions()
            .map(TransactionDeduplicator::dedupe)
}
