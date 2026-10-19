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
            "cashback",
            "deposited",
            "deposit",
            "reversal"
        ).any { lower.contains(it) }

        val isDebit = listOf(
            "debited",
            "paid",
            "spent",
            "sent",
            "withdrawn",
            "transfer",
            "transferred",
            "purchase",
            "dr."
        ).any { lower.contains(it) }

        return when {
            isCredit && !isDebit -> TransactionType.CREDIT
            isDebit && !isCredit -> TransactionType.DEBIT
            else -> TransactionType.UNKNOWN
        }
    }
}
