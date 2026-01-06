package com.example.pinkmoney.utils

object SmsAppFilter {

    private val smsPackages = setOf(
        "com.google.android.apps.messaging", // Google Messages
        "com.android.mms",                   // AOSP
        "com.samsung.android.messaging",     // Samsung
        "com.miui.messaging",                // Xiaomi
        "com.truecaller"                     // Truecaller SMS
    )

    fun isSmsApp(packageName: String): Boolean {
        return smsPackages.contains(packageName)
    }
}