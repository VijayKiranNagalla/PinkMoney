package com.example.pinkmoney.utils

object UpiAppFilter {

    private val supportedPackages = setOf(
        "com.google.android.apps.nbu.paisa.user", // GPay
        "com.phonepe.app",                        // PhonePe
        "net.one97.paytm",                        // Paytm
        "com.amazon.mShop.android.shopping",     // Amazon Pay
        "com.sbi.lotusintouch",                  // SBI YONO
        "com.hdfcbank.mobilebanking"             // HDFC
    )

    fun isUpiApp(packageName: String): Boolean {
        return supportedPackages.contains(packageName)
    }
}