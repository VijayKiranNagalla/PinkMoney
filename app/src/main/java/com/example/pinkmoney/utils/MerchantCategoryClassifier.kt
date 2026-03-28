package com.example.pinkmoney.utils

object MerchantCategoryClassifier {

    private val categoryMap = mapOf(

        // 🍔 FOOD
        "Food" to listOf(
            "zomato", "swiggy", "dominos", "pizza", "kfc",
            "burger", "restaurant", "cafe", "starbucks",
            "eatclub", "faasos", "box8", "mcd", "subway"
        ),

        // 🛒 GROCERIES
        "Groceries" to listOf(
            "dmart", "reliance smart", "bigbasket", "grofers",
            "blinkit", "zepto", "jiomart", "more store",
            "spencer", "fresh", "easyday"
        ),

        // 🛍 SHOPPING
        "Shopping" to listOf(
            "amazon", "flipkart", "myntra", "ajio",
            "meesho", "tatacliq", "shopclues", "nykaa"
        ),

        // 💡 BILLS
        "Bills" to listOf(
            "electric", "electricity", "bescom", "tneb",
            "water", "gas", "broadband", "wifi",
            "jio", "airtel", "vi", "bsnl",
            "recharge", "postpaid"
        ),

        // 📱 ELECTRONICS
        "Electronics" to listOf(
            "croma", "reliance digital", "vijay sales",
            "apple", "samsung", "oneplus", "mi store"
        ),

        // 🚕 TRANSPORT
        "Transport" to listOf(
            "uber", "ola", "rapido", "metro",
            "irctc", "redbus", "makemytrip", "yatra"
        ),

        // 🎬 ENTERTAINMENT
        "Entertainment" to listOf(
            "netflix", "amazon prime", "primevideo",
            "hotstar", "spotify", "youtube", "bookmyshow"
        ),

        // 🏦 FINANCE / TRANSFERS
        "Transfer" to listOf(
            "upi", "transfer", "neft", "imps",
            "rtgs", "paytm", "phonepe", "gpay"
        )
    )

    fun classify(merchantRaw: String?): String {

        if (merchantRaw.isNullOrBlank()) return "Others"

        val merchant = merchantRaw.lowercase()

        for ((category, keywords) in categoryMap) {
            if (keywords.any { merchant.contains(it) }) {
                return category
            }
        }

        return "Others"
    }
}