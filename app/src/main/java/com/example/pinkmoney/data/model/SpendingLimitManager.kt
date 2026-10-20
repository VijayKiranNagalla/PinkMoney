package com.example.pinkmoney.data.model

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.util.Calendar

enum class LimitPeriod(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
    CUSTOM("Custom")
}

data class SpendingLimitSettings(
    val isEnabled: Boolean = false,
    val period: LimitPeriod = LimitPeriod.MONTHLY,
    val overallLimitEnabled: Boolean = false,
    val overallLimitAmount: Double = 0.0,
    val bucketLimits: Map<String, Double> = emptyMap()
)

class SpendingLimitManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pink_money_spending_limits", Context.MODE_PRIVATE)

    fun getSettings(): SpendingLimitSettings {
        val isEnabled = prefs.getBoolean(KEY_IS_ENABLED, false)
        val periodStr = prefs.getString(KEY_PERIOD, LimitPeriod.MONTHLY.name) ?: LimitPeriod.MONTHLY.name
        val period = try {
            LimitPeriod.valueOf(periodStr)
        } catch (_: Exception) {
            LimitPeriod.MONTHLY
        }
        val overallLimitEnabled = prefs.getBoolean(KEY_OVERALL_ENABLED, false)
        val overallLimitAmount = prefs.getFloat(KEY_OVERALL_AMOUNT, 0f).toDouble()

        val bucketLimitsJsonStr = prefs.getString(KEY_BUCKET_LIMITS_JSON, "{}") ?: "{}"
        val bucketLimits = mutableMapOf<String, Double>()
        try {
            val json = JSONObject(bucketLimitsJsonStr)
            json.keys().forEach { key ->
                bucketLimits[key] = json.getDouble(key)
            }
        } catch (_: Exception) {}

        return SpendingLimitSettings(
            isEnabled = isEnabled,
            period = period,
            overallLimitEnabled = overallLimitEnabled,
            overallLimitAmount = overallLimitAmount,
            bucketLimits = bucketLimits
        )
    }

    fun saveSettings(settings: SpendingLimitSettings) {
        val json = JSONObject()
        settings.bucketLimits.forEach { (key, value) ->
            json.put(key, value)
        }

        prefs.edit().apply {
            putBoolean(KEY_IS_ENABLED, settings.isEnabled)
            putString(KEY_PERIOD, settings.period.name)
            putBoolean(KEY_OVERALL_ENABLED, settings.overallLimitEnabled)
            putFloat(KEY_OVERALL_AMOUNT, settings.overallLimitAmount.toFloat())
            putString(KEY_BUCKET_LIMITS_JSON, json.toString())
            apply()
        }
    }

    fun getPeriodStartTimestamp(period: LimitPeriod): Long {
        return when (period) {
            LimitPeriod.DAILY -> {
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            LimitPeriod.WEEKLY -> {
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -6)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            LimitPeriod.MONTHLY -> {
                Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            LimitPeriod.YEARLY -> {
                Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            LimitPeriod.CUSTOM -> {
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -29)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
        }
    }

    companion object {
        private const val KEY_IS_ENABLED = "key_is_enabled"
        private const val KEY_PERIOD = "key_period"
        private const val KEY_OVERALL_ENABLED = "key_overall_enabled"
        private const val KEY_OVERALL_AMOUNT = "key_overall_amount"
        private const val KEY_BUCKET_LIMITS_JSON = "key_bucket_limits_json"
    }
}
