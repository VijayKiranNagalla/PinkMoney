package com.example.pinkmoney.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.pinkmoney.data.db.PinkMoneyDatabase
import com.example.pinkmoney.data.entity.TransactionEntity
import com.example.pinkmoney.utils.AmountParser
import com.example.pinkmoney.utils.MerchantParser
import com.example.pinkmoney.utils.SmsAppFilter
import com.example.pinkmoney.utils.UpiAppFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UpiNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        val packageName = sbn.packageName

        // 1️⃣ Source filter: UPI apps OR SMS apps
        val isFinancialSource =
            UpiAppFilter.isUpiApp(packageName) ||
                    SmsAppFilter.isSmsApp(packageName)

        if (!isFinancialSource) return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: return
        val text = extras.getCharSequence("android.text")?.toString() ?: return

        val rawText = "$title $text"
        val combinedText = rawText.lowercase()

        // 2️⃣ Keyword filter
        val isFinancial = listOf(
            "paid",
            "debited",
            "credited",
            "received",
            "transaction",
            "upi",
            "₹",
            "inr"
        ).any { keyword ->
            combinedText.contains(keyword)
        }

        if (!isFinancial) return

        // 3️⃣ Parse
        val amount = AmountParser.extractAmount(combinedText) ?: return
        val merchant = MerchantParser.extractMerchant(combinedText)
        val timestamp = sbn.postTime

        Log.d(
            "PinkMoneyParsed",
            "AMOUNT=$amount | MERCHANT=$merchant | TIME=$timestamp | TEXT=$rawText"
        )

        // 4️⃣ Persist
        val source = if (SmsAppFilter.isSmsApp(packageName)) "SMS" else "UPI"

        val transaction = TransactionEntity(
            amount = amount,
            merchant = merchant,
            timestamp = timestamp,
            source = source,
            rawText = rawText
        )

        val db = PinkMoneyDatabase.getInstance(applicationContext)

        CoroutineScope(Dispatchers.IO).launch {
            db.transactionDao().insertTransaction(transaction)
        }
    }
}
