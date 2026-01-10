package com.example.pinkmoney.utils

object MerchantParser {

    private val keywords = listOf(
        "to",
        "at",
        "from",
        "by"
    )

    fun extractMerchant(text: String): String? {
        val words = text.split(" ")

        for (i in words.indices) {
            if (keywords.contains(words[i].lowercase()) && i + 1 < words.size) {
                return words[i + 1]
                    .replace(Regex("[^a-zA-Z0-9]"), "")
            }
        }
        return null
    }
}
