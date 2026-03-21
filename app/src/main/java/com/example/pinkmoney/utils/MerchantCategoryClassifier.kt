package com.example.pinkmoney.utils

object MerchantCategoryClassifier {

    fun classify(merchantRaw: String?): String {

        if (merchantRaw == null) return "Others"

        val merchant = merchantRaw.lowercase()

        return when {

            // 🍔 FOOD
            merchant.contains("zomato") ||
                    merchant.contains("swiggy") ||
                    merchant.contains("eat") ||
                    merchant.contains("restaurant") ||
                    merchant.contains("cafe") ->
                "Food"

            // 🛒 GROCERIES
            merchant.contains("dmart") ||
                    merchant.contains("reliance") ||
                    merchant.contains("bigbasket") ||
                    merchant.contains("grofers") ||
                    merchant.contains("jiomart") ->
                "Groceries"

            // 🛍 SHOPPING
            merchant.contains("amazon") ||
                    merchant.contains("flipkart") ||
                    merchant.contains("myntra") ||
                    merchant.contains("ajio") ->
                "Shopping"

            // 💡 BILLS
            merchant.contains("electric") ||
                    merchant.contains("bescom") ||
                    merchant.contains("water") ||
                    merchant.contains("gas") ||
                    merchant.contains("broadband") ||
                    merchant.contains("jiofiber") ->
                "Bills"

            // 📱 ELECTRONICS
            merchant.contains("croma") ||
                    merchant.contains("vijay sales") ||
                    merchant.contains("reliance digital") ->
                "Electronics"

            // 🚕 TRANSPORT
            merchant.contains("uber") ||
                    merchant.contains("ola") ||
                    merchant.contains("rapido") ->
                "Transport"

            else -> "Others"
        }
    }
}