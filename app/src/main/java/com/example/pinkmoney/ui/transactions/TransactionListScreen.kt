package com.example.pinkmoney.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.pinkmoney.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionListScreen(
    transactionsFlow: Flow<List<TransactionEntity>>,
    modifier: Modifier = Modifier
) {
    val transactions by transactionsFlow.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(transactions) { txn ->
            TransactionItem(txn)
        }
    }
}

@Composable
fun TransactionItem(txn: TransactionEntity) {

    val date = remember(txn.timestamp) {
        SimpleDateFormat("dd MMM", Locale.getDefault())
            .format(Date(txn.timestamp))
    }

    val amountColor = when (txn.transactionType) {
        "DEBIT" -> Color.Red
        "CREDIT" -> Color(0xFF2E7D32)
        else -> Color.Gray
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = txn.merchant ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "₹${txn.amount}",
                color = amountColor
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "$date • ${txn.transactionType}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }

    Divider()
}