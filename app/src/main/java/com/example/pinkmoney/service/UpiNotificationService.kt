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

        // ✅ Only SMS (avoid duplicates)
        val isFinancialSource = SmsAppFilter.isSmsApp(packageName)
        if (!isFinancialSource) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: return
        val text = extras.getCharSequence("android.text")?.toString() ?: return

        val rawText = "$title $text"
        val combinedText = rawText.lowercase()

        // ✅ Keyword filter
        val isFinancial = listOf(
            "paid", "debited", "credited", "received",
            "transaction", "upi", "₹", "inr"
        ).any { combinedText.contains(it) }

        if (!isFinancial) return

        // ✅ Noise filter
        if (!TransactionValidityFilter.isValidTransaction(combinedText)) {
            Log.d("PinkMoneyFilter", "Ignored non-transaction notification")
            return
        }

        // ✅ Extract amount
        val amount = AmountParser.extractAmount(combinedText) ?: return

        val timestamp = sbn.postTime
        val source = "SMS"

        val db = PinkMoneyDatabase.getInstance(applicationContext)

        Log.d("PinkMoneyAI", "Sending transaction to Gemini")
        Log.d("GeminiPrompt", rawText)

        // 🔥 GEMINI CALL
        GeminiParser.parseTransaction(rawText) { category, merchant, type ->

            Log.d("PinkMoneyAIResult", "CATEGORY=$category")

            if (category != "REAL_TRANSACTION") {
                Log.d("PinkMoneyAI", "Ignored $category message")
                return@parseTransaction
            }

            val finalMerchant = merchant ?: "Unknown"
            val finalType = type ?: "UNKNOWN"

            // 🔥 EVERYTHING IMPORTANT HAPPENS HERE
            CoroutineScope(Dispatchers.IO).launch {

                val normalizedMerchant =
                    MerchantNormalizer.normalize(finalMerchant)

                // ✅ 1. Check user mapping first
                val savedBucket = db.bucketDao()
                    .getBucketForMerchant(normalizedMerchant)

                val finalCategory = if (savedBucket != null) {

                    Log.d(
                        "BUCKET_OVERRIDE",
                        "$normalizedMerchant → $savedBucket"
                    )

                    savedBucket

                } else {
                    // ✅ fallback to rule engine
                    MerchantCategoryClassifier
                        .classify(normalizedMerchant)
                }

                Log.d(
                    "PinkMoneyAIResult",
                    "TYPE=$finalType | MERCHANT=$normalizedMerchant | CATEGORY=$finalCategory"
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
                    category = finalCategory
                )

                val rowId = db.transactionDao()
                    .insertTransaction(transaction)

                if (rowId == -1L) {
                    Log.d("PinkMoneyDedup", "Duplicate transaction ignored")
                } else {
                    Log.d("PinkMoneyAI", "Transaction inserted")
                }
            }
        }
    }
}