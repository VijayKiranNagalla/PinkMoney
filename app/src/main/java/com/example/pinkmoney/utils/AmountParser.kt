package com.example.pinkmoney.utils

import java.util.regex.Pattern

object AmountParser {

    private val amountPatterns = listOf(
        Pattern.compile("₹\\s?([0-9,]+\\.?[0-9]*)"),
        Pattern.compile("rs\\.?\\s?([0-9,]+\\.?[0-9]*)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("inr\\s?([0-9,]+\\.?[0-9]*)", Pattern.CASE_INSENSITIVE)
    )

    fun extractAmount(text: String): Double? {
        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val rawAmount = matcher.group(1)
                    .replace(",", "")
                return rawAmount.toDoubleOrNull()
            }
        }
        return null
    }
}
