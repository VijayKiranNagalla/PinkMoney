package com.example.pinkmoney.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.pinkmoney.data.db.PinkMoneyDatabase
import com.example.pinkmoney.data.entity.TransactionEntity
import com.example.pinkmoney.utils.AmountParser
import com.example.pinkmoney.utils.MerchantNormalizer
import com.example.pinkmoney.utils.MerchantParser
import com.example.pinkmoney.utils.SmsAppFilter
import com.example.pinkmoney.utils.TransactionTypeDetector
import com.example.pinkmoney.utils.UpiAppFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.pinkmoney.utils.TransactionHasher
import com.example.pinkmoney.utils.TransactionValidityFilter

class UpiNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        val packageName = sbn.packageName

        // 1️⃣ Source filter: UPI apps OR SMS apps
        val isFinancialSource =
            UpiAppFilter.isUpiApp(packageName) ||
                    SmsAppFilter.isSmsApp(packageName)

        if (!isFinancialSource) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: return
        val text  = extras.getCharSequence("android.text")?.toString() ?: return


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
        //also reject if its a bill
        if (!TransactionValidityFilter.isValidTransaction(combinedText)) {
            Log.d("PinkMoneyFilter", "Ignored non-transaction notification")
            return
        }

        // 3️⃣ Parse
        val amount = AmountParser.extractAmount(combinedText) ?: return
        val merchant = MerchantParser.extractMerchant(combinedText)
        val timestamp = sbn.postTime
        val detectedTransactionType =
            TransactionTypeDetector.detect(combinedText).name

        if (detectedTransactionType == "UNKNOWN") {
            Log.d("PinkMoneyType", "UNKNOWN | $rawText")
        }

        Log.d(
            "PinkMoneyParsed",
            "TYPE=$detectedTransactionType | AMOUNT=$amount | MERCHANT=$merchant | TIME=$timestamp | TEXT=$rawText"
        )

        val normalizedMerchant = MerchantNormalizer.normalize(merchant)
        Log.d("PinkMoneyNormalized", "RAW=$merchant | NORMALIZED=$normalizedMerchant")



        // 4️⃣ Persist
        val source = if (SmsAppFilter.isSmsApp(packageName)) "SMS" else "UPI"

        val transactionHash = TransactionHasher.generate(
            amount = amount,
            merchant = merchant,
            timestamp = timestamp,
            transactionType = detectedTransactionType,
            source = source
        )


        val transaction = TransactionEntity(
            amount = amount,
            merchant = merchant,
            timestamp = timestamp,
            source = source,
            transactionType = detectedTransactionType, // CREDIT / DEBIT / UNKNOWN
            rawText = rawText,
            transactionHash = transactionHash
        )

        val db = PinkMoneyDatabase.getInstance(applicationContext)

        CoroutineScope(Dispatchers.IO).launch {
            val rowId = db.transactionDao().insertTransaction(transaction)

            if (rowId == -1L) {
                Log.d("PinkMoneyDedup", "Duplicate transaction ignored")
            } else {
                Log.d("success", "transaction inserted in room")
            }
        }


    }
}