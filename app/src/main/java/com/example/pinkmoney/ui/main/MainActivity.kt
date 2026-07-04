package com.example.pinkmoney.ui.main

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pinkmoney.data.db.PinkMoneyDatabase
import com.example.pinkmoney.ui.theme.PinkMoneyTheme
import com.example.pinkmoney.ui.transactions.TransactionViewModel
import com.example.pinkmoney.utils.isNotificationAccessEnabled

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val database = PinkMoneyDatabase.getInstance(applicationContext)

            val transactionViewModel: TransactionViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        if (!modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
                            throw IllegalArgumentException("Unknown ViewModel class")
                        }
                        @Suppress("UNCHECKED_CAST")
                        return TransactionViewModel(database) as T
                    }
                }
            )

            PinkMoneyTheme {
                MainScreen(
                    viewModel = transactionViewModel
                )
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
