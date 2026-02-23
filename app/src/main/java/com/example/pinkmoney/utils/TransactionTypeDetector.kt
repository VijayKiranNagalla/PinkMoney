package com.example.pinkmoney.utils

object TransactionTypeDetector {

    enum class TransactionType {
        CREDIT,
        DEBIT,
        UNKNOWN
    }

    fun detect(text: String): TransactionType {
        val lower = text.lowercase()

        val isCredit = listOf(
            "credited",
            "received",
            "refund",
            "cashback"
        ).any { lower.contains(it) }

        val isDebit = listOf(
            "debited",
            "paid",
            "spent",
            "sent",
            "withdrawn"
        ).any { lower.contains(it) }

        return when {
            isCredit && !isDebit -> TransactionType.CREDIT
            isDebit && !isCredit -> TransactionType.DEBIT
            else -> TransactionType.UNKNOWN
        }
    }
}