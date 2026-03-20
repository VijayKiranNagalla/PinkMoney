package com.example.pinkmoney.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.pinkmoney.ui.transactions.TransactionListScreen
import com.example.pinkmoney.ui.transactions.TransactionViewModel

@Composable
fun MainScreen(
    viewModel: TransactionViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {

                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Home") }
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AccountTree, contentDescription = null) },
                    label = { Text("Buckets") }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    label = { Text("Stats") }
                )
            }
        }
    ) { padding ->

        when (selectedTab) {
            0 -> TransactionListScreen(
                transactionsFlow = viewModel.transactions,
                modifier = Modifier.padding(padding)
            )

            1 -> BucketsScreen()

            2 -> StatsScreen()
        }
    }
}