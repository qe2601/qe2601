package com.example.gptalertlauncher

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppSettings {
    private const val PREFS = "gpt_alert_launcher_settings"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_SILENT_AUTO_LAUNCH = "silent_auto_launch"
    private const val KEY_SHOW_FALLBACK_ON_FAILURE = "show_fallback_on_failure"
    private const val KEY_COOLDOWN_SECONDS = "cooldown_seconds"
    private const val KEY_SCREEN_ON_ONLY = "screen_on_only"
    private const val KEY_UNLOCKED_ONLY = "unlocked_only"
    private const val KEY_QUIET_START = "quiet_start"
    private const val KEY_QUIET_END = "quiet_end"
    private const val KEY_LAUNCH_DELAY_MS = "launch_delay_ms"
    private const val KEY_RETRY_ENABLED = "retry_enabled"
    private const val KEY_RETRY_DELAY_MS = "retry_delay_ms"
    private const val KEY_LAST_LAUNCH_MS = "last_launch_ms"

    private const val KEY_LAST_DETECTED_MS = "diag_last_detected_ms"
    private const val KEY_LAST_ATTEMPT_MS = "diag_last_attempt_ms"
    private const val KEY_LAST_RESULT = "diag_last_result"
    private const val KEY_LAST_RETRY_MS = "diag_last_retry_ms"
    private const val KEY_LAST_RETRY_RESULT = "diag_last_retry_result"
    private const val KEY_LAST_FALLBACK_RESULT = "diag_last_fallback_result"

    const val CHATGPT_PACKAGE = "com.openai.chatgpt"
    const val DEFAULT_COOLDOWN_SECONDS = 10
    const val DEFAULT_QUIET_START = "22:00"
    const val DEFAULT_QUIET_END = "07:00"
    const val DEFAULT_LAUNCH_DELAY_MS = 300
    const val DEFAULT_RETRY_DELAY_MS = 700

    const val RESULT_NOT_ATTEMPTED = "not attempted"
    const val RESULT_ATTEMPTED = "attempted"
    const val RESULT_FAILED_NOT_INSTALLED = "failed: ChatGPT not installed"
    const val RESULT_FAILED_EXCEPTION_PREFIX = "failed: exception "
    const val RESULT_SKIPPED_DISABLED = "skipped: automation disabled"
    const val RESULT_SKIPPED_COOLDOWN = "skipped: cooldown"
    const val RESULT_SKIPPED_QUIET_HOURS = "skipped: quiet hours"
    const val RESULT_SKIPPED_SCREEN_OFF = "skipped: screen off"
    const val RESULT_SKIPPED_DEVICE_LOCKED = "skipped: device locked"

    const val FALLBACK_DISABLED = "disabled"
    const val FALLBACK_SHOWN = "shown"
    const val FALLBACK_PERMISSION_MISSING = "skipped: notification permission missing"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)
    fun setEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()

    fun silentAutoLaunch(context: Context): Boolean = prefs(context).getBoolean(KEY_SILENT_AUTO_LAUNCH, true)
    fun setSilentAutoLaunch(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_SILENT_AUTO_LAUNCH, value).apply()

    fun showFallbackOnFailure(context: Context): Boolean = prefs(context).getBoolean(KEY_SHOW_FALLBACK_ON_FAILURE, false)
    fun setShowFallbackOnFailure(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_SHOW_FALLBACK_ON_FAILURE, value).apply()

    fun cooldownSeconds(context: Context): Int = prefs(context).getInt(KEY_COOLDOWN_SECONDS, DEFAULT_COOLDOWN_SECONDS).coerceIn(1, 3600)
    fun setCooldownSeconds(context: Context, value: Int) = prefs(context).edit().putInt(KEY_COOLDOWN_SECONDS, value.coerceIn(1, 3600)).apply()

    fun screenOnOnly(context: Context): Boolean = prefs(context).getBoolean(KEY_SCREEN_ON_ONLY, true)
    fun setScreenOnOnly(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_SCREEN_ON_ONLY, value).apply()

    fun unlockedOnly(context: Context): Boolean = prefs(context).getBoolean(KEY_UNLOCKED_ONLY, true)
    fun setUnlockedOnly(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_UNLOCKED_ONLY, value).apply()

    fun quietStart(context: Context): String = prefs(context).getString(KEY_QUIET_START, DEFAULT_QUIET_START) ?: DEFAULT_QUIET_START
    fun setQuietStart(context: Context, value: String) = prefs(context).edit().putString(KEY_QUIET_START, sanitizeTime(value, DEFAULT_QUIET_START)).apply()

    fun quietEnd(context: Context): String = prefs(context).getString(KEY_QUIET_END, DEFAULT_QUIET_END) ?: DEFAULT_QUIET_END
    fun setQuietEnd(context: Context, value: String) = prefs(context).edit().putString(KEY_QUIET_END, sanitizeTime(value, DEFAULT_QUIET_END)).apply()

    fun launchDelayMs(context: Context): Int = prefs(context).getInt(KEY_LAUNCH_DELAY_MS, DEFAULT_LAUNCH_DELAY_MS).coerceIn(0, 5000)
    fun setLaunchDelayMs(context: Context, value: Int) = prefs(context).edit().putInt(KEY_LAUNCH_DELAY_MS, value.coerceIn(0, 5000)).apply()

    fun retryEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_RETRY_ENABLED, true)
    fun setRetryEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_RETRY_ENABLED, value).apply()

    fun retryDelayMs(context: Context): Int = prefs(context).getInt(KEY_RETRY_DELAY_MS, DEFAULT_RETRY_DELAY_MS).coerceIn(100, 5000)
    fun setRetryDelayMs(context: Context, value: Int) = prefs(context).edit().putInt(KEY_RETRY_DELAY_MS, value.coerceIn(100, 5000)).apply()

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

    fun recordDetected(context: Context, value: Long = System.currentTimeMillis()) = prefs(context).edit().putLong(KEY_LAST_DETECTED_MS, value).apply()
    fun recordLaunchResult(context: Context, result: String, value: Long = System.currentTimeMillis()) = prefs(context).edit().putLong(KEY_LAST_ATTEMPT_MS, value).putString(KEY_LAST_RESULT, result).apply()
    fun recordRetryResult(context: Context, result: String, value: Long = System.currentTimeMillis()) = prefs(context).edit().putLong(KEY_LAST_RETRY_MS, value).putString(KEY_LAST_RETRY_RESULT, result).apply()
    fun recordFallbackResult(context: Context, result: String) = prefs(context).edit().putString(KEY_LAST_FALLBACK_RESULT, result).apply()

    fun clearDiagnostics(context: Context) = prefs(context).edit()
        .remove(KEY_LAST_DETECTED_MS)
        .remove(KEY_LAST_ATTEMPT_MS)
        .remove(KEY_LAST_RESULT)
        .remove(KEY_LAST_RETRY_MS)
        .remove(KEY_LAST_RETRY_RESULT)
        .remove(KEY_LAST_FALLBACK_RESULT)
        .apply()

    fun diagnosticsSnapshot(context: Context): DiagnosticsSnapshot {
        val p = prefs(context)
        return DiagnosticsSnapshot(
            lastDetected = formatMs(p.getLong(KEY_LAST_DETECTED_MS, 0L)),
            lastAttempt = formatMs(p.getLong(KEY_LAST_ATTEMPT_MS, 0L)),
            lastResult = p.getString(KEY_LAST_RESULT, RESULT_NOT_ATTEMPTED) ?: RESULT_NOT_ATTEMPTED,
            lastRetryAttempt = formatMs(p.getLong(KEY_LAST_RETRY_MS, 0L)),
            lastRetryResult = p.getString(KEY_LAST_RETRY_RESULT, RESULT_NOT_ATTEMPTED) ?: RESULT_NOT_ATTEMPTED,
            lastFallbackResult = p.getString(KEY_LAST_FALLBACK_RESULT, FALLBACK_DISABLED) ?: FALLBACK_DISABLED,
        )
    }

    fun settingsSnapshot(context: Context): String = listOf(
        "자동 실행 사용: ${onOff(isEnabled(context))}",
        "무음 자동 실행 모드: ${onOff(silentAutoLaunch(context))}",
        "실패 시 별도 알림 표시: ${onOff(showFallbackOnFailure(context))}",
        "쿨다운(초): ${cooldownSeconds(context)}",
        "화면 켜짐 상태에서만 실행: ${onOff(screenOnOnly(context))}",
        "잠금 해제 상태에서만 실행: ${onOff(unlockedOnly(context))}",
        "조용한 시간 시작: ${quietStart(context)}",
        "조용한 시간 종료: ${quietEnd(context)}",
        "실행 지연(ms): ${launchDelayMs(context)}",
        "재시도 사용: ${onOff(retryEnabled(context))}",
        "재시도 지연(ms): ${retryDelayMs(context)}",
    ).joinToString("\n")

    fun koreanResult(value: String): String = when {
        value == RESULT_NOT_ATTEMPTED -> "시도 안 함"
        value == RESULT_ATTEMPTED -> "시도함"
        value == RESULT_FAILED_NOT_INSTALLED -> "실패: ChatGPT 미설치"
        value.startsWith(RESULT_FAILED_EXCEPTION_PREFIX) -> "실패: 예외 ${value.removePrefix(RESULT_FAILED_EXCEPTION_PREFIX)}"
        value == RESULT_SKIPPED_DISABLED -> "건너뜀: 자동 실행 꺼짐"
        value == RESULT_SKIPPED_COOLDOWN -> "건너뜀: 쿨다운"
        value == RESULT_SKIPPED_QUIET_HOURS -> "건너뜀: 조용한 시간"
        value == RESULT_SKIPPED_SCREEN_OFF -> "건너뜀: 화면 꺼짐"
        value == RESULT_SKIPPED_DEVICE_LOCKED -> "건너뜀: 기기 잠김"
        value == FALLBACK_DISABLED -> "꺼짐"
        value == FALLBACK_SHOWN -> "표시됨"
        value == FALLBACK_PERMISSION_MISSING -> "건너뜀: 알림 권한 없음"
        else -> value
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

    private fun formatMs(value: Long): String = if (value <= 0L) "없음" else TIME_FORMAT.format(Date(value))
    private fun onOff(value: Boolean): String = if (value) "켜짐" else "꺼짐"

    private val TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)

    data class DiagnosticsSnapshot(
        val lastDetected: String,
        val lastAttempt: String,
        val lastResult: String,
        val lastRetryAttempt: String,
        val lastRetryResult: String,
        val lastFallbackResult: String,
    )
}
