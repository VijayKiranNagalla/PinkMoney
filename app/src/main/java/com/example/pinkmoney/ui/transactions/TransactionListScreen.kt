package com.example.pinkmoney.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.pinkmoney.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    transactionsFlow: Flow<List<TransactionEntity>>,
    onCategorizeClick: (TransactionEntity) -> Unit, // ✅ NEW
    modifier: Modifier = Modifier
) {

    val transactions by transactionsFlow.collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {

        Spacer(Modifier.height(12.dp))

        Text(
            text = "PinkMoney",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn {
            items(transactions) { txn ->
                TransactionCard(
                    txn = txn,
                    onCategorizeClick = onCategorizeClick // ✅ pass down
                )
            }
        }
    }
}

@Composable
fun TransactionCard(
    txn: TransactionEntity,
    onCategorizeClick: (TransactionEntity) -> Unit // ✅ NEW
) {

    val date = remember(txn.timestamp) {
        SimpleDateFormat("dd MMM", Locale.getDefault())
            .format(Date(txn.timestamp))
    }

    val amountColor = when (txn.transactionType) {
        "DEBIT" -> Color(0xFFE53935)
        "CREDIT" -> Color(0xFF2E7D32)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1F25)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text = txn.merchant ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "$date • ${txn.transactionType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Text(
                    text = "₹${txn.amount}",
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor
                )
            }

            // 🔥 SHOW ONLY FOR "Others"
            if (txn.category == "Others") {

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "➕ Categorize",
                    color = Color(0xFFBB86FC),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable {
                        onCategorizeClick(txn) // 🚀 trigger navigation
                    }
                )
            }
        }
    }
}