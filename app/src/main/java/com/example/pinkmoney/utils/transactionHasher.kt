package com.example.pinkmoney.utils

import java.security.MessageDigest

object TransactionHasher {

    fun generate(
        amount: Double,
        merchant: String?,
        timestamp: Long,
        transactionType: String,
        source: String
    ): String {

        // Round timestamp to minute to avoid tiny differences
        val normalizedTime = timestamp / (60 * 1000)

        val baseString = listOf(
            amount.toString(),
            merchant ?: "UNKNOWN",
            transactionType,
            source,
            normalizedTime.toString()
        ).joinToString("|")

        return sha256(baseString)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(input.toByteArray())

        return bytes.joinToString("") { "%02x".format(it) }
    }
}