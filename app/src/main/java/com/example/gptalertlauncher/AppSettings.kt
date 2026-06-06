package com.example.gptalertlauncher

import android.content.Context

object AppSettings {
    private const val PREFS = "gpt_alert_launcher_settings"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_COOLDOWN_SECONDS = "cooldown_seconds"
    private const val KEY_SCREEN_ON_ONLY = "screen_on_only"
    private const val KEY_UNLOCKED_ONLY = "unlocked_only"
    private const val KEY_QUIET_START = "quiet_start"
    private const val KEY_QUIET_END = "quiet_end"
    private const val KEY_LAST_LAUNCH_MS = "last_launch_ms"

    const val CHATGPT_PACKAGE = "com.openai.chatgpt"
    const val DEFAULT_COOLDOWN_SECONDS = 10
    const val DEFAULT_QUIET_START = "22:00"
    const val DEFAULT_QUIET_END = "07:00"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)
    fun setEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()

    fun cooldownSeconds(context: Context): Int = prefs(context).getInt(KEY_COOLDOWN_SECONDS, DEFAULT_COOLDOWN_SECONDS)
    fun setCooldownSeconds(context: Context, value: Int) = prefs(context).edit().putInt(KEY_COOLDOWN_SECONDS, value.coerceIn(1, 3600)).apply()

    fun screenOnOnly(context: Context): Boolean = prefs(context).getBoolean(KEY_SCREEN_ON_ONLY, true)
    fun setScreenOnOnly(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_SCREEN_ON_ONLY, value).apply()

    fun unlockedOnly(context: Context): Boolean = prefs(context).getBoolean(KEY_UNLOCKED_ONLY, true)
    fun setUnlockedOnly(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_UNLOCKED_ONLY, value).apply()

    fun quietStart(context: Context): String = prefs(context).getString(KEY_QUIET_START, DEFAULT_QUIET_START) ?: DEFAULT_QUIET_START
    fun setQuietStart(context: Context, value: String) = prefs(context).edit().putString(KEY_QUIET_START, sanitizeTime(value, DEFAULT_QUIET_START)).apply()

    fun quietEnd(context: Context): String = prefs(context).getString(KEY_QUIET_END, DEFAULT_QUIET_END) ?: DEFAULT_QUIET_END
    fun setQuietEnd(context: Context, value: String) = prefs(context).edit().putString(KEY_QUIET_END, sanitizeTime(value, DEFAULT_QUIET_END)).apply()

    fun lastLaunchMs(context: Context): Long = prefs(context).getLong(KEY_LAST_LAUNCH_MS, 0L)
    fun setLastLaunchMs(context: Context, value: Long) = prefs(context).edit().putLong(KEY_LAST_LAUNCH_MS, value).apply()

    fun isInCooldown(context: Context, nowMs: Long = System.currentTimeMillis()): Boolean {
        return nowMs - lastLaunchMs(context) < cooldownSeconds(context) * 1000L
    }

    fun isQuietNow(context: Context, nowMinutes: Int = currentMinutes()): Boolean {
        val start = parseMinutes(quietStart(context)) ?: return false
        val end = parseMinutes(quietEnd(context)) ?: return false
        if (start == end) return false
        return if (start < end) nowMinutes in start until end else nowMinutes >= start || nowMinutes < end
    }

    private fun sanitizeTime(value: String, fallback: String): String {
        val minutes = parseMinutes(value.trim()) ?: return fallback
        return "%02d:%02d".format(minutes / 60, minutes % 60)
    }

    private fun parseMinutes(value: String): Int? {
        val parts = value.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    private fun currentMinutes(): Int {
        val calendar = java.util.Calendar.getInstance()
        return calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + calendar.get(java.util.Calendar.MINUTE)
    }
}
