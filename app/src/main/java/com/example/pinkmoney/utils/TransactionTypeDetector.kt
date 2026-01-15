package com.example.pinkmoney.utils

object TransactionTypeDetector {

    enum class TransactionType {
        CREDIT,
        DEBIT
    }

    // Strong keywords banks consistently use
    private val creditKeywords = listOf(
        "credited",
        "received",
        "refund",
        "cashback"
    )

    private val debitKeywords = listOf(
        "debited",
        "spent",
        "paid",
        "withdrawn",
        "purchase"
    )

    /**
     * Determines whether the transaction is CREDIT or DEBIT
     * based on notification text.
     */
    fun detect(text: String): TransactionType? {
        val normalized = text.lowercase()

        // 1️⃣ Credit detection (highest priority)
        if (creditKeywords.any { normalized.contains(it) }) {
            return TransactionType.CREDIT
        }

        // 2️⃣ Debit detection
        if (debitKeywords.any { normalized.contains(it) }) {
            return TransactionType.DEBIT
        }

        // 3️⃣ Unknown / ambiguous
        return null
    }
}
