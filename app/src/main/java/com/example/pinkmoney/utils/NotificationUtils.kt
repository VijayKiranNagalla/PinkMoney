package com.example.pinkmoney.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.pinkmoney.R
import com.example.pinkmoney.data.db.PinkMoneyDatabase
import com.example.pinkmoney.data.entity.BudgetAlertEntity
import com.example.pinkmoney.data.model.SpendingLimitManager

fun isNotificationAccessEnabled(context: Context): Boolean {
    val enabledListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    ) ?: return false

    return enabledListeners.contains(context.packageName)
}

object BudgetNotificationHelper {
    private const val CHANNEL_ID = "pink_money_budget_alerts"
    private const val CHANNEL_NAME = "PinkMoney Budget Alerts"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts and warnings when spending limits are reached or exceeded."
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    suspend fun checkAndSendLimitNotifications(
        context: Context,
        addedAmount: Double,
        category: String
    ) {
        createNotificationChannel(context)
        val limitManager = SpendingLimitManager(context)
        val settings = limitManager.getSettings()

        if (!settings.isEnabled) return

        val db = PinkMoneyDatabase.getInstance(context)
        val periodStart = limitManager.getPeriodStartTimestamp(settings.period)
        val allTransactions = db.transactionDao().getTransactionsList()

        val periodExpenseTransactions = allTransactions
            .filter { it.timestamp >= periodStart }
            .filter { isExpense(it.transactionType, it.rawText) }

        val currentOverallSpent = periodExpenseTransactions.sumOf { it.amount }
        val prevOverallSpent = currentOverallSpent - addedAmount

        // 1. Overall Limit check
        if (settings.overallLimitEnabled && settings.overallLimitAmount > 0.0) {
            val limit = settings.overallLimitAmount
            if (prevOverallSpent < limit && currentOverallSpent >= limit) {
                sendNotification(
                    context,
                    notificationId = 1001,
                    title = "⚠️ Overall Spending Limit Exceeded!",
                    message = "You spent ₹${currentOverallSpent.toInt()} out of your ₹${limit.toInt()} ${settings.period.label.lowercase()} budget.",
                    category = "Overall"
                )
            } else if (prevOverallSpent < limit * 0.8 && currentOverallSpent >= limit * 0.8 && currentOverallSpent < limit) {
                val percentage = ((currentOverallSpent / limit) * 100).toInt()
                sendNotification(
                    context,
                    notificationId = 1002,
                    title = "⚠️ Spending Limit Warning ($percentage%)",
                    message = "You reached $percentage% (₹${currentOverallSpent.toInt()}) of your ₹${limit.toInt()} ${settings.period.label.lowercase()} budget.",
                    category = "Overall"
                )
            }
        }

        // 2. Bucket Limit check
        val bucketLimit = settings.bucketLimits[category]
        if (bucketLimit != null && bucketLimit > 0.0) {
            val currentBucketSpent = periodExpenseTransactions
                .filter { it.category.equals(category, ignoreCase = true) }
                .sumOf { it.amount }
            val prevBucketSpent = currentBucketSpent - addedAmount

            if (prevBucketSpent < bucketLimit && currentBucketSpent >= bucketLimit) {
                sendNotification(
                    context,
                    notificationId = 2000 + category.hashCode(),
                    title = "⚠️ $category Limit Exceeded!",
                    message = "You spent ₹${currentBucketSpent.toInt()} in $category, exceeding your ₹${bucketLimit.toInt()} limit.",
                    category = category
                )
            } else if (prevBucketSpent < bucketLimit * 0.8 && currentBucketSpent >= bucketLimit * 0.8 && currentBucketSpent < bucketLimit) {
                val percentage = ((currentBucketSpent / bucketLimit) * 100).toInt()
                sendNotification(
                    context,
                    notificationId = 3000 + category.hashCode(),
                    title = "⚠️ $category Limit Warning ($percentage%)",
                    message = "You reached $percentage% (₹${currentBucketSpent.toInt()}) of your ₹${bucketLimit.toInt()} budget for $category.",
                    category = category
                )
            }
        }
    }

    private fun isExpense(transactionType: String, rawText: String): Boolean {
        return when {
            transactionType.equals("DEBIT", ignoreCase = true) -> true
            transactionType.equals("CREDIT", ignoreCase = true) -> false
            else -> TransactionTypeDetector.detect(rawText) == TransactionTypeDetector.TransactionType.DEBIT
        }
    }

    private suspend fun sendNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        category: String
    ) {
        val db = PinkMoneyDatabase.getInstance(context)
        db.budgetAlertDao().insertAlert(
            BudgetAlertEntity(
                title = title,
                message = message,
                timestamp = System.currentTimeMillis(),
                category = category,
                isRead = false
            )
        )

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            manager.notify(notificationId, builder.build())
        } catch (_: Exception) {}
    }
}
