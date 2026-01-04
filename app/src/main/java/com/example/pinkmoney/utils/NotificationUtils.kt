package com.example.pinkmoney.utils
import android.provider.Settings
import android.content.Context


fun isNotificationAccessEnabled(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        return enabledListeners.contains(context.packageName)
    }
