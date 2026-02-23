package com.example.pinkmoney.utils

object MerchantNormalizer {

    // Canonical merchant → list of known aliases
    private val merchantMap: Map<String, List<String>> = mapOf(

        "AMAZON" to listOf(
            "amazon",
            "amazon pay",
            "amazon pay india",
            "amazon.in",
            "amazon marketplace"
        ),

        "FLIPKART" to listOf(
            "flipkart",
            "flipkart internet",
            "flipkart pvt ltd"
        ),

        "ZOMATO" to listOf(
            "zomato",
            "zomato limited"
        ),

        "SWIGGY" to listOf(
            "swiggy",
            "swiggy instamart"
        ),

        "UBER" to listOf(
            "uber",
            "uber trip",
            "uber india"
        ),

        "OLA" to listOf(
            "ola",
            "ola cabs"
        )
    )

    /**
     * Returns a canonical merchant name if matched,
     * otherwise returns the cleaned original merchant.
     */
    fun normalize(merchant: String?): String? {
        if (merchant.isNullOrBlank()) return null

        val cleaned = merchant
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .trim()

        for ((canonical, aliases) in merchantMap) {
            if (aliases.any { cleaned.contains(it) }) {
                return canonical
            }
        }

        // Fallback: title‑case cleaned merchant
        return cleaned.split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}
