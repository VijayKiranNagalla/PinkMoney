package com.example.pinkmoney.ui.main

import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.pinkmoney.ui.transactions.TransactionListScreen
import com.example.pinkmoney.ui.transactions.TransactionViewModel
import com.example.pinkmoney.data.entity.TransactionEntity

@Composable
fun MainScreen(
    viewModel: TransactionViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedTxn by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AccountTree, null) },
                    label = { Text("Buckets") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.BarChart, null) },
                    label = { Text("Stats") }
                )
            }
        }
    ) { padding ->

        when (selectedTab) {

            0 -> TransactionListScreen(
                transactionsFlow = viewModel.transactions,
                onCategorizeClick = { txn ->
                    selectedTxn = txn
                    selectedTab = 1
                },
                modifier = Modifier.padding(padding)
            )

            1 -> BucketsScreen(
                incomingTransaction = selectedTxn,
                onDone = {
                    selectedTxn = null   // 🔥 RESET MAGIC
                },
                modifier = Modifier.padding(padding)
            )

            2 -> StatsScreen(
                modifier = Modifier.padding(padding)
            )
        }
    }
}
