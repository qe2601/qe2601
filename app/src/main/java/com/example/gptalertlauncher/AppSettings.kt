package com.example.gptalertlauncher

import android.content.Context

object AppSettings {
    private const val PREFS = "gpt_alert_launcher_settings"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_SILENT_MODE = "silent_mode"
    private const val KEY_FALLBACK_ENABLED = "fallback_enabled"
    private const val KEY_COOLDOWN_SECONDS = "cooldown_seconds"
    private const val KEY_SCREEN_ON_ONLY = "screen_on_only"
    private const val KEY_UNLOCKED_ONLY = "unlocked_only"
    private const val KEY_QUIET_START = "quiet_start"
    private const val KEY_QUIET_END = "quiet_end"
    private const val KEY_LAST_LAUNCH_MS = "last_launch_ms"
    private const val KEY_LAUNCH_DELAY_MS = "launch_delay_ms"
    private const val KEY_RETRY_ENABLED = "retry_enabled"
    private const val KEY_RETRY_DELAY_MS = "retry_delay_ms"

    private const val KEY_LAST_DETECTED_MS = "last_detected_ms"
    private const val KEY_LAST_ATTEMPT_MS = "last_attempt_ms"
    private const val KEY_LAST_RESULT = "last_result"
    private const val KEY_LAST_RETRY_MS = "last_retry_ms"
    private const val KEY_LAST_RETRY_RESULT = "last_retry_result"
    private const val KEY_LAST_FALLBACK_RESULT = "last_fallback_result"

    const val CHATGPT_PACKAGE = "com.openai.chatgpt"
    const val DEFAULT_COOLDOWN_SECONDS = 10
    const val DEFAULT_QUIET_START = "22:00"
    const val DEFAULT_QUIET_END = "07:00"
    const val DEFAULT_LAUNCH_DELAY_MS = 300
    const val DEFAULT_RETRY_DELAY_MS = 700

    const val RESULT_NOT_ATTEMPTED = "아직 없음"
    const val RESULT_ATTEMPTED = "실행 시도함"
    const val RESULT_FAILED_NOT_INSTALLED = "실패: ChatGPT 미설치"
    const val RESULT_SKIPPED_DISABLED = "건너뜀: 자동 실행 꺼짐"
    const val RESULT_SKIPPED_COOLDOWN = "건너뜀: 쿨다운 중"
    const val RESULT_SKIPPED_QUIET = "건너뜀: 조용한 시간"
    const val RESULT_SKIPPED_SCREEN_OFF = "건너뜀: 화면 꺼짐"
    const val RESULT_SKIPPED_LOCKED = "건너뜀: 기기 잠김"
    const val FALLBACK_DISABLED = "별도 알림 꺼짐"
    const val FALLBACK_SHOWN = "별도 알림 표시됨"
    const val FALLBACK_PERMISSION_MISSING = "건너뜀: 알림 권한 없음"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)
    fun setEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()

    fun silentMode(context: Context): Boolean = prefs(context).getBoolean(KEY_SILENT_MODE, true)
    fun setSilentMode(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_SILENT_MODE, value).apply()

    fun fallbackEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_FALLBACK_ENABLED, false)
    fun setFallbackEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_FALLBACK_ENABLED, value).apply()

    fun cooldownSeconds(context: Context): Int = prefs(context).getInt(KEY_COOLDOWN_SECONDS, DEFAULT_COOLDOWN_SECONDS).coerceIn(1, 3600)
    fun setCooldownSeconds(context: Context, value: Int) = prefs(context).edit().putInt(KEY_COOLDOWN_SECONDS, value.coerceIn(1, 3600)).apply()

    fun launchDelayMs(context: Context): Int = prefs(context).getInt(KEY_LAUNCH_DELAY_MS, DEFAULT_LAUNCH_DELAY_MS).coerceIn(0, 5000)
    fun setLaunchDelayMs(context: Context, value: Int) = prefs(context).edit().putInt(KEY_LAUNCH_DELAY_MS, value.coerceIn(0, 5000)).apply()

    fun retryEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_RETRY_ENABLED, true)
    fun setRetryEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_RETRY_ENABLED, value).apply()

    fun retryDelayMs(context: Context): Int = prefs(context).getInt(KEY_RETRY_DELAY_MS, DEFAULT_RETRY_DELAY_MS).coerceIn(100, 5000)
    fun setRetryDelayMs(context: Context, value: Int) = prefs(context).edit().putInt(KEY_RETRY_DELAY_MS, value.coerceIn(100, 5000)).apply()

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

    fun markDetected(context: Context, nowMs: Long = System.currentTimeMillis()) = prefs(context).edit().putLong(KEY_LAST_DETECTED_MS, nowMs).apply()
    fun recordLaunchResult(context: Context, result: String, nowMs: Long = System.currentTimeMillis()) = prefs(context).edit().putLong(KEY_LAST_ATTEMPT_MS, nowMs).putString(KEY_LAST_RESULT, result).apply()
    fun recordRetryResult(context: Context, result: String, nowMs: Long = System.currentTimeMillis()) = prefs(context).edit().putLong(KEY_LAST_RETRY_MS, nowMs).putString(KEY_LAST_RETRY_RESULT, result).apply()
    fun recordFallbackResult(context: Context, result: String) = prefs(context).edit().putString(KEY_LAST_FALLBACK_RESULT, result).apply()

    fun lastDetectedMs(context: Context): Long = prefs(context).getLong(KEY_LAST_DETECTED_MS, 0L)
    fun lastAttemptMs(context: Context): Long = prefs(context).getLong(KEY_LAST_ATTEMPT_MS, 0L)
    fun lastResult(context: Context): String = prefs(context).getString(KEY_LAST_RESULT, RESULT_NOT_ATTEMPTED) ?: RESULT_NOT_ATTEMPTED
    fun lastRetryMs(context: Context): Long = prefs(context).getLong(KEY_LAST_RETRY_MS, 0L)
    fun lastRetryResult(context: Context): String = prefs(context).getString(KEY_LAST_RETRY_RESULT, RESULT_NOT_ATTEMPTED) ?: RESULT_NOT_ATTEMPTED
    fun lastFallbackResult(context: Context): String = prefs(context).getString(KEY_LAST_FALLBACK_RESULT, FALLBACK_DISABLED) ?: FALLBACK_DISABLED

    fun clearDiagnostics(context: Context) {
        prefs(context).edit()
            .remove(KEY_LAST_DETECTED_MS)
            .remove(KEY_LAST_ATTEMPT_MS)
            .remove(KEY_LAST_RESULT)
            .remove(KEY_LAST_RETRY_MS)
            .remove(KEY_LAST_RETRY_RESULT)
            .remove(KEY_LAST_FALLBACK_RESULT)
            .apply()
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
