package com.example.pinkmoney.utils

object MerchantParser {

    // Common verbs banks use
    private val verbPatterns = listOf(
        Regex("([a-zA-Z0-9\\s]+)\\s+credited", RegexOption.IGNORE_CASE),
        Regex("paid\\s+to\\s+([a-zA-Z0-9\\s]+)", RegexOption.IGNORE_CASE),
        Regex("received\\s+from\\s+([a-zA-Z0-9\\s]+)", RegexOption.IGNORE_CASE),
        Regex("debited\\s+to\\s+([a-zA-Z0-9\\s]+)", RegexOption.IGNORE_CASE)
    )

    private val prepositions = setOf("to", "at", "from", "by")

    fun extractMerchant(text: String): String? {

        // 1️⃣ Verb-based extraction (BEST)
        for (pattern in verbPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                return clean(match.groupValues[1])
            }
        }

        // 2️⃣ Preposition-based extraction (improved)
        val words = text.split(" ")

        for (i in words.indices) {
            if (prepositions.contains(words[i].lowercase())) {

                val merchantWords = mutableListOf<String>()

                for (j in i + 1 until words.size) {
                    val word = words[j]

                    // Stop if we hit common terminators
                    if (
                        word.matches(Regex("\\d+")) ||        // numbers
                        word.contains("₹") ||
                        word.equals("on", true) ||
                        word.equals("via", true) ||
                        word.equals("using", true)
                    ) break

                    merchantWords.add(word)
                }

                if (merchantWords.isNotEmpty()) {
                    return clean(merchantWords.joinToString(" "))
                }
            }
        }


        // 3️⃣ Nothing reliable found
        return null
    }

    private fun clean(input: String): String {
        return input
            .replace(Regex("[^a-zA-Z0-9 ]"), "")
            .trim()
    }
}