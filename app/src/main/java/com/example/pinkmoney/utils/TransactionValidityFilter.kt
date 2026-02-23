package com.example.pinkmoney.utils

object TransactionValidityFilter {

    // Phrases that usually indicate reminders / bills / promos
    private val nonTransactionKeywords = listOf(
        "bill",
        "due",
        "pay now",
        "payment pending",
        "clear your bill",
        "ignore if already paid",
        "to continue enjoying",
        "will be disconnected",
        "service will be",
        "recharge",
        "valid till",
        "offer",
        "discount",
        "cashback offer"
    )

    /**
     * Returns true if this notification looks like
     * a REAL completed transaction.
     */
    fun isValidTransaction(text: String): Boolean {
        val lower = text.lowercase()

        // If it contains any known non‑transaction phrases → reject
        if (nonTransactionKeywords.any { lower.contains(it) }) {
            return false
        }

        return true
    }
}
