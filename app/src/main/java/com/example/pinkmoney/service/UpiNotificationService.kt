package com.example.pinkmoney.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.pinkmoney.utils.AmountParser
import com.example.pinkmoney.utils.MerchantParser
import com.example.pinkmoney.utils.UpiAppFilter
import com.example.pinkmoney.utils.SmsAppFilter

class UpiNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        val packageName = sbn.packageName

        // 1️⃣ Source filter: UPI apps OR SMS apps
        val isFinancialSource =
            UpiAppFilter.isUpiApp(packageName) ||
                    SmsAppFilter.isSmsApp(packageName)

        if (!isFinancialSource) return

        val extras = sbn.notification.extras

        val title = extras.getString("android.title") ?: return // if title doesnt exit then can return
        val text = extras.getCharSequence("android.text")?.toString() ?: return

        // 2️⃣ Keyword filter
        val combinedText = "$title $text".lowercase()

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


        val amount = AmountParser.extractAmount(combinedText)
        val merchant = MerchantParser.extractMerchant(combinedText)

        Log.d(
            "PinkMoneyParsed",
            "AMOUNT=$amount | MERCHANT=$merchant | TEXT=$combinedText"
        )
    }
}
