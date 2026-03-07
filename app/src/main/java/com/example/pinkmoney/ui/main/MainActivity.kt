package com.example.pinkmoney.ui.main

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pinkmoney.data.db.PinkMoneyDatabase
import com.example.pinkmoney.ui.theme.PinkMoneyTheme
import com.example.pinkmoney.ui.transactions.TransactionListScreen
import com.example.pinkmoney.ui.transactions.TransactionViewModel
import com.example.pinkmoney.utils.isNotificationAccessEnabled

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            val database = PinkMoneyDatabase.getInstance(applicationContext)

            // Proper ViewModel factory
            val transactionViewModel: TransactionViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return TransactionViewModel(database) as T
                    }
                }
            )

            PinkMoneyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    TransactionListScreen(
                        transactionsFlow = transactionViewModel.transactions,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isNotificationAccessEnabled(this)) {
            showNotificationAccessDialog()
        }
    }

    private fun showNotificationAccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Notification Access")
            .setMessage("PinkMoney needs notification access to automatically detect UPI payments and track expenses.")
            .setCancelable(false)
            .setPositiveButton("Enable") { _, _ ->
                openNotificationAccessSettings()
            }
            .setNegativeButton("Not now", null)
            .show()
    }

    private fun openNotificationAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }
}