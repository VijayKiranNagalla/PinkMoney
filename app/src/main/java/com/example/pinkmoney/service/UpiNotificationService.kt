package com.example.pinkmoney.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.pinkmoney.data.db.PinkMoneyDatabase
import com.example.pinkmoney.data.entity.TransactionEntity
import com.example.pinkmoney.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UpiNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        val packageName = sbn.packageName

        // 1️⃣ Source filter
        //We were checking for notifs from upi apps as well as messages, but that results in duplicates.
        // Trying if it works without UPI notifs.
        // If we want to parse UPI notifs as well, all we need to do is read the ref id of the transaction.
//        val isFinancialSource =
//            UpiAppFilter.isUpiApp(packageName) ||
//                    SmsAppFilter.isSmsApp(packageName)


        val isFinancialSource = SmsAppFilter.isSmsApp(packageName)

        if (!isFinancialSource) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: return
        val text = extras.getCharSequence("android.text")?.toString() ?: return

        val rawText = "$title $text"
        val combinedText = rawText.lowercase()

        // 2️⃣ Keyword filter
        val isFinancial = listOf(
            "paid", "debited", "credited", "received",
            "transaction", "upi", "₹", "inr"
        ).any { combinedText.contains(it) }

        if (!isFinancial) return

        // 3️⃣ Noise filter
        if (!TransactionValidityFilter.isValidTransaction(combinedText)) {
            Log.d("PinkMoneyFilter", "Ignored non‑transaction notification")
            return
        }

        // 4️⃣ Extract amount locally
        val amount = AmountParser.extractAmount(combinedText) ?: return
        val timestamp = sbn.postTime

        val source =
            if (SmsAppFilter.isSmsApp(packageName)) "SMS"
            else "UPI"

        val db = PinkMoneyDatabase.getInstance(applicationContext)

        Log.d("PinkMoneyAI", "Sending transaction to Gemini")
        Log.d("GeminiPrompt", rawText)

        // 5️⃣ CALL GEMINI (NEW SIGNATURE)
        GeminiParser.parseTransaction(rawText) { category, merchant, type ->

            Log.d("PinkMoneyAIResult", "CATEGORY=$category")

            // 🚨 VERY IMPORTANT
            if (category != "REAL_TRANSACTION") {
                Log.d("PinkMoneyAI", "Ignored $category message")
                return@parseTransaction
            }

            val finalMerchant = merchant ?: "Unknown"
            val finalType = type ?: "UNKNOWN"

            val normalizedMerchant =
                MerchantNormalizer.normalize(finalMerchant)

            val category =
                MerchantCategoryClassifier.classify(normalizedMerchant)

            Log.d(
                "PinkMoneyAIResult",
                "TYPE=$finalType | MERCHANT=$normalizedMerchant"
            )

            val transactionHash = TransactionHasher.generate(
                amount = amount,
                merchant = normalizedMerchant,
                timestamp = timestamp,
                transactionType = finalType,
                source = source
            )

            val transaction = TransactionEntity(
                amount = amount,
                merchant = normalizedMerchant,
                timestamp = timestamp,
                source = source,
                transactionType = finalType,
                rawText = rawText,
                transactionHash = transactionHash,
                category = category
            )

            CoroutineScope(Dispatchers.IO).launch {
                val rowId = db.transactionDao().insertTransaction(transaction)

                if (rowId == -1L) {
                    Log.d("PinkMoneyDedup", "Duplicate transaction ignored")
                } else {
                    Log.d("PinkMoneyAI", "Transaction inserted using Gemini")
                }
            }
        }
    }
}