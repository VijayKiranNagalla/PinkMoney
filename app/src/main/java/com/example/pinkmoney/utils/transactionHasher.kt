package com.example.pinkmoney.utils

import java.security.MessageDigest

object TransactionHasher {

    fun generate(
        amount: Double,
        merchant: String?,
        timestamp: Long,
        transactionType: String?,
        source: String,
        rawText: String?
    ): String {
        val normalizedText = rawText
            ?.lowercase()
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()

        val baseString = listOf(
            amount.toString(),
            merchant ?: "UNKNOWN",
            transactionType,
            source,
            if (normalizedText.isNotEmpty()) {
                normalizedText
            } else {
                (timestamp / (60 * 1000)).toString()
            }
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
