package com.example.gptalertlauncher

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AppSettings {
    private const val PREFS = "gpt_alert_launcher_settings"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_SILENT_AUTO_LAUNCH = "silent_auto_launch"
    private const val KEY_ACCESSIBILITY_ASSIST = "accessibility_assist"
    private const val KEY_SHOW_FALLBACK_ON_FAILURE = "show_fallback_on_failure"
    private const val KEY_COOLDOWN_SECONDS = "cooldown_seconds"
    private const val KEY_SCREEN_ON_ONLY = "screen_on_only"
    private const val KEY_UNLOCKED_ONLY = "unlocked_only"
    private const val KEY_QUIET_START = "quiet_start"
    private const val KEY_QUIET_END = "quiet_end"
    private const val KEY_LAUNCH_DELAY_MS = "launch_delay_ms"
    private const val KEY_RETRY_ENABLED = "retry_enabled"
    private const val KEY_RETRY_DELAY_MS = "retry_delay_ms"
    private const val KEY_ACCESSIBILITY_TIMEOUT_MS = "accessibility_timeout_ms"
    private const val KEY_LAST_LAUNCH_MS = "last_launch_ms"
    private const val KEY_PENDING_LAUNCH_MS = "pending_launch_ms"

    private const val KEY_LAST_DETECTED_MS = "diag_last_detected_ms"
    private const val KEY_LAST_ATTEMPT_MS = "diag_last_attempt_ms"
    private const val KEY_LAST_RESULT = "diag_last_result"
    private const val KEY_LAST_RETRY_MS = "diag_last_retry_ms"
    private const val KEY_LAST_RETRY_RESULT = "diag_last_retry_result"
    private const val KEY_LAST_ACCESSIBILITY_ATTEMPT_MS = "diag_last_accessibility_attempt_ms"
    private const val KEY_LAST_ACCESSIBILITY_RESULT = "diag_last_accessibility_result"
    private const val KEY_ACCESSIBILITY_PERMISSION_STATUS = "diag_accessibility_permission_status"
    private const val KEY_ACCESSIBILITY_SETTINGS_OPEN_RESULT = "diag_accessibility_settings_open_result"
    private const val KEY_LAST_ACCESSIBILITY_STATUS_CHECK_MS = "diag_last_accessibility_status_check_ms"
    private const val KEY_LAST_FALLBACK_RESULT = "diag_last_fallback_result"
    private const val KEY_LAST_PRE_SWITCH_WARNING_METHOD = "diag_last_pre_switch_warning_method"
    private const val KEY_LAST_PRE_SWITCH_WARNING_NUMBER = "diag_last_pre_switch_warning_number"
    private const val KEY_LAST_PRE_SWITCH_WARNING_RESULT = "diag_last_pre_switch_warning_result"

    const val CHATGPT_PACKAGE = "com.openai.chatgpt"
    const val DEFAULT_COOLDOWN_SECONDS = 10
    const val DEFAULT_QUIET_START = "22:00"
    const val DEFAULT_QUIET_END = "07:00"
    const val DEFAULT_LAUNCH_DELAY_MS = 300
    const val DEFAULT_RETRY_DELAY_MS = 700
    const val DEFAULT_ACCESSIBILITY_TIMEOUT_MS = 5000
    const val PRE_SWITCH_WARNING_MODE_COUNTDOWN_NOTIFICATION = "노티바 카운트다운"

    const val RESULT_NOT_ATTEMPTED = "not attempted"
    const val RESULT_ATTEMPTED = "attempted"
    const val RESULT_FAILED_NOT_INSTALLED = "failed: ChatGPT not installed"
    const val RESULT_FAILED_EXCEPTION_PREFIX = "failed: exception "
    const val RESULT_SKIPPED_DISABLED = "skipped: automation disabled"
    const val RESULT_SKIPPED_COOLDOWN = "skipped: cooldown"
    const val RESULT_SKIPPED_QUIET_HOURS = "skipped: quiet hours"
    const val RESULT_SKIPPED_SCREEN_OFF = "skipped: screen off"
    const val RESULT_SKIPPED_DEVICE_LOCKED = "skipped: device locked"

    const val ACCESSIBILITY_DISABLED = "accessibility disabled"
    const val ACCESSIBILITY_ATTEMPTED = "accessibility attempted"
    const val ACCESSIBILITY_SKIPPED_NO_PENDING = "skipped: no pending launch"
    const val ACCESSIBILITY_SKIPPED_LOCKED = "skipped: accessibility device locked"
    const val ACCESSIBILITY_SKIPPED_UNSAFE_PACKAGE = "skipped: unsafe package"
    const val ACCESSIBILITY_SKIPPED_TIMEOUT = "skipped: timeout"
    const val ACCESSIBILITY_SUCCESS_FOREGROUND = "success: ChatGPT foreground detected"
    const val ACCESSIBILITY_PERMISSION_ON = "accessibility permission on"
    const val ACCESSIBILITY_PERMISSION_OFF = "accessibility permission off"
    const val ACCESSIBILITY_SETTINGS_OPENED = "accessibility settings opened"
    const val ACCESSIBILITY_SETTINGS_OPEN_FAILED = "accessibility settings open failed"

    const val FALLBACK_DISABLED = "disabled"
    const val FALLBACK_SHOWN = "shown"
    const val FALLBACK_PERMISSION_MISSING = "skipped: notification permission missing"

    const val PRE_SWITCH_WARNING_NOT_ATTEMPTED = "not attempted"
    const val PRE_SWITCH_WARNING_SHOWN = "shown"
    const val PRE_SWITCH_WARNING_CANCELLED_FOR_LAUNCH = "cancelled: launch starting"
    const val PRE_SWITCH_WARNING_SKIPPED_IMMEDIATE = "skipped: immediate launch"
    const val PRE_SWITCH_WARNING_PERMISSION_MISSING = "skipped: notification permission missing"
    const val PRE_SWITCH_WARNING_FAILED = "failed: notification error"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)
    fun setEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()

    fun silentAutoLaunch(context: Context): Boolean = prefs(context).getBoolean(KEY_SILENT_AUTO_LAUNCH, true)
    fun setSilentAutoLaunch(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_SILENT_AUTO_LAUNCH, value).apply()

    fun accessibilityAssist(context: Context): Boolean = prefs(context).getBoolean(KEY_ACCESSIBILITY_ASSIST, false)
    fun setAccessibilityAssist(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_ACCESSIBILITY_ASSIST, value).apply()

    fun showFallbackOnFailure(context: Context): Boolean = prefs(context).getBoolean(KEY_SHOW_FALLBACK_ON_FAILURE, false)
    fun setShowFallbackOnFailure(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_SHOW_FALLBACK_ON_FAILURE, value).apply()

    fun preSwitchWarningMode(): String = PRE_SWITCH_WARNING_MODE_COUNTDOWN_NOTIFICATION

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

    fun accessibilityTimeoutMs(context: Context): Int = prefs(context).getInt(KEY_ACCESSIBILITY_TIMEOUT_MS, DEFAULT_ACCESSIBILITY_TIMEOUT_MS).coerceIn(1000, 15000)
    fun setAccessibilityTimeoutMs(context: Context, value: Int) = prefs(context).edit().putInt(KEY_ACCESSIBILITY_TIMEOUT_MS, value.coerceIn(1000, 15000)).apply()

    fun lastLaunchMs(context: Context): Long = prefs(context).getLong(KEY_LAST_LAUNCH_MS, 0L)
    fun setLastLaunchMs(context: Context, value: Long) = prefs(context).edit().putLong(KEY_LAST_LAUNCH_MS, value).apply()

    fun pendingLaunchMs(context: Context): Long = prefs(context).getLong(KEY_PENDING_LAUNCH_MS, 0L)
    fun markPendingLaunch(context: Context, value: Long = System.currentTimeMillis()) = prefs(context).edit().putLong(KEY_PENDING_LAUNCH_MS, value).apply()
    fun clearPendingLaunch(context: Context) = prefs(context).edit().putLong(KEY_PENDING_LAUNCH_MS, 0L).apply()

    fun isPendingLaunchActive(context: Context, nowMs: Long = System.currentTimeMillis()): Boolean {
        val pendingMs = pendingLaunchMs(context)
        return pendingMs > 0L && nowMs - pendingMs <= accessibilityTimeoutMs(context)
    }

    fun isInCooldown(context: Context, nowMs: Long = System.currentTimeMillis()): Boolean {
        return nowMs - lastLaunchMs(context) < cooldownSeconds(context) * 1000L
    }

    fun isQuietNow(context: Context, nowMinutes: Int = currentMinutes()): Boolean {
        val start = parseMinutes(quietStart(context)) ?: return false
        val end = parseMinutes(quietEnd(context)) ?: return false
        if (start == end) return false
        return if (start < end) {
            nowMinutes in start until end
        } else {
            nowMinutes >= start || nowMinutes < end
        }
    }

    fun recordDetected(context: Context, timeMs: Long = System.currentTimeMillis()) {
        prefs(context).edit().putLong(KEY_LAST_DETECTED_MS, timeMs).apply()
    }

    fun recordLaunchResult(context: Context, result: String, timeMs: Long = System.currentTimeMillis()) {
        prefs(context).edit()
            .putLong(KEY_LAST_ATTEMPT_MS, timeMs)
            .putString(KEY_LAST_RESULT, result)
            .apply()
    }

    fun recordRetryResult(context: Context, result: String, timeMs: Long = System.currentTimeMillis()) {
        prefs(context).edit()
            .putLong(KEY_LAST_RETRY_MS, timeMs)
            .putString(KEY_LAST_RETRY_RESULT, result)
            .apply()
    }

    fun recordAccessibilityResult(context: Context, result: String, timeMs: Long = System.currentTimeMillis()) {
        prefs(context).edit()
            .putLong(KEY_LAST_ACCESSIBILITY_ATTEMPT_MS, timeMs)
            .putString(KEY_LAST_ACCESSIBILITY_RESULT, result)
            .apply()
    }

    fun recordAccessibilityPermissionStatus(context: Context, enabled: Boolean, timeMs: Long = System.currentTimeMillis()) {
        prefs(context).edit()
            .putString(KEY_ACCESSIBILITY_PERMISSION_STATUS, if (enabled) ACCESSIBILITY_PERMISSION_ON else ACCESSIBILITY_PERMISSION_OFF)
            .putLong(KEY_LAST_ACCESSIBILITY_STATUS_CHECK_MS, timeMs)
            .apply()
    }

    fun recordAccessibilitySettingsOpenResult(context: Context, result: String) {
        prefs(context).edit().putString(KEY_ACCESSIBILITY_SETTINGS_OPEN_RESULT, result).apply()
    }

    fun recordFallbackResult(context: Context, result: String) {
        prefs(context).edit().putString(KEY_LAST_FALLBACK_RESULT, result).apply()
    }

    fun recordPreSwitchWarning(context: Context, number: String, result: String) {
        prefs(context).edit()
            .putString(KEY_LAST_PRE_SWITCH_WARNING_METHOD, preSwitchWarningMode())
            .putString(KEY_LAST_PRE_SWITCH_WARNING_NUMBER, number)
            .putString(KEY_LAST_PRE_SWITCH_WARNING_RESULT, result)
            .apply()
    }

    fun clearDiagnostics(context: Context) {
        prefs(context).edit()
            .remove(KEY_LAST_DETECTED_MS)
            .remove(KEY_LAST_ATTEMPT_MS)
            .remove(KEY_LAST_RESULT)
            .remove(KEY_LAST_RETRY_MS)
            .remove(KEY_LAST_RETRY_RESULT)
            .remove(KEY_LAST_ACCESSIBILITY_ATTEMPT_MS)
            .remove(KEY_LAST_ACCESSIBILITY_RESULT)
            .remove(KEY_ACCESSIBILITY_PERMISSION_STATUS)
            .remove(KEY_ACCESSIBILITY_SETTINGS_OPEN_RESULT)
            .remove(KEY_LAST_ACCESSIBILITY_STATUS_CHECK_MS)
            .remove(KEY_LAST_FALLBACK_RESULT)
            .remove(KEY_LAST_PRE_SWITCH_WARNING_METHOD)
            .remove(KEY_LAST_PRE_SWITCH_WARNING_NUMBER)
            .remove(KEY_LAST_PRE_SWITCH_WARNING_RESULT)
            .remove(KEY_PENDING_LAUNCH_MS)
            .apply()
    }

    fun diagnosticsSnapshot(context: Context): DiagnosticsSnapshot {
        val preferences = prefs(context)
        return DiagnosticsSnapshot(
            lastDetected = formatMs(preferences.getLong(KEY_LAST_DETECTED_MS, 0L)),
            lastAttempt = formatMs(preferences.getLong(KEY_LAST_ATTEMPT_MS, 0L)),
            lastResult = preferences.getString(KEY_LAST_RESULT, RESULT_NOT_ATTEMPTED) ?: RESULT_NOT_ATTEMPTED,
            lastRetryAttempt = formatMs(preferences.getLong(KEY_LAST_RETRY_MS, 0L)),
            lastRetryResult = preferences.getString(KEY_LAST_RETRY_RESULT, RESULT_NOT_ATTEMPTED) ?: RESULT_NOT_ATTEMPTED,
            lastAccessibilityAttempt = formatMs(preferences.getLong(KEY_LAST_ACCESSIBILITY_ATTEMPT_MS, 0L)),
            lastAccessibilityResult = preferences.getString(KEY_LAST_ACCESSIBILITY_RESULT, ACCESSIBILITY_DISABLED) ?: ACCESSIBILITY_DISABLED,
            accessibilityPermissionStatus = preferences.getString(KEY_ACCESSIBILITY_PERMISSION_STATUS, ACCESSIBILITY_PERMISSION_OFF) ?: ACCESSIBILITY_PERMISSION_OFF,
            accessibilitySettingsOpenResult = preferences.getString(KEY_ACCESSIBILITY_SETTINGS_OPEN_RESULT, RESULT_NOT_ATTEMPTED) ?: RESULT_NOT_ATTEMPTED,
            lastAccessibilityStatusCheck = formatMs(preferences.getLong(KEY_LAST_ACCESSIBILITY_STATUS_CHECK_MS, 0L)),
            lastFallbackResult = preferences.getString(KEY_LAST_FALLBACK_RESULT, FALLBACK_DISABLED) ?: FALLBACK_DISABLED,
            lastPreSwitchWarningMethod = preferences.getString(KEY_LAST_PRE_SWITCH_WARNING_METHOD, preSwitchWarningMode()) ?: preSwitchWarningMode(),
            lastPreSwitchWarningNumber = preferences.getString(KEY_LAST_PRE_SWITCH_WARNING_NUMBER, "없음") ?: "없음",
            lastPreSwitchWarningResult = preferences.getString(KEY_LAST_PRE_SWITCH_WARNING_RESULT, PRE_SWITCH_WARNING_NOT_ATTEMPTED) ?: PRE_SWITCH_WARNING_NOT_ATTEMPTED,
        )
    }

    fun settingsSnapshot(context: Context): String {
        return listOf(
            "자동 실행 사용: ${onOff(isEnabled(context))}",
            "무음 자동 실행 모드: ${onOff(silentAutoLaunch(context))}",
            "접근성 보조 모드: ${onOff(accessibilityAssist(context))}",
            "전환 예고 방식: ${preSwitchWarningMode()}",
            "전환 예고 표시: 전환까지 남은 숫자만 표시",
            "실패 시 별도 알림 표시: ${onOff(showFallbackOnFailure(context))}",
            "화면 켜짐 상태에서만 실행: ${onOff(screenOnOnly(context))}",
            "잠금 해제 상태에서만 실행: ${onOff(unlockedOnly(context))}",
            "재시도 사용: ${onOff(retryEnabled(context))}",
            "쿨다운(초): ${cooldownSeconds(context)}",
            "실행 지연(ms): ${launchDelayMs(context)}",
            "재시도 지연(ms): ${retryDelayMs(context)}",
            "접근성 보조 제한 시간(ms): ${accessibilityTimeoutMs(context)}",
            "조용한 시간: ${quietStart(context)} ~ ${quietEnd(context)}",
        ).joinToString("\n")
    }

    fun koreanResult(result: String): String {
        return when {
            result == RESULT_NOT_ATTEMPTED -> "아직 없음"
            result == RESULT_ATTEMPTED -> "실행 시도함"
            result == RESULT_FAILED_NOT_INSTALLED -> "실패: ChatGPT 미설치"
            result.startsWith(RESULT_FAILED_EXCEPTION_PREFIX) -> "실패: 예외 발생"
            result == RESULT_SKIPPED_DISABLED -> "건너뜀: 자동 실행 꺼짐"
            result == RESULT_SKIPPED_COOLDOWN -> "건너뜀: 쿨다운 중"
            result == RESULT_SKIPPED_QUIET_HOURS -> "건너뜀: 조용한 시간"
            result == RESULT_SKIPPED_SCREEN_OFF -> "건너뜀: 화면 꺼짐"
            result == RESULT_SKIPPED_DEVICE_LOCKED -> "건너뜀: 기기 잠김"
            result == ACCESSIBILITY_DISABLED -> "접근성 보조 꺼짐"
            result == ACCESSIBILITY_ATTEMPTED -> "접근성 보조 시도함"
            result == ACCESSIBILITY_SKIPPED_NO_PENDING -> "건너뜀: 대기 중인 실행 요청 없음"
            result == ACCESSIBILITY_SKIPPED_LOCKED -> "건너뜀: 기기 잠김"
            result == ACCESSIBILITY_SKIPPED_UNSAFE_PACKAGE -> "건너뜀: 안전하지 않은 앱"
            result == ACCESSIBILITY_SKIPPED_TIMEOUT -> "건너뜀: 제한 시간 초과"
            result == ACCESSIBILITY_SUCCESS_FOREGROUND -> "성공: ChatGPT 전면 감지"
            result == ACCESSIBILITY_PERMISSION_ON -> "켜짐"
            result == ACCESSIBILITY_PERMISSION_OFF -> "꺼짐"
            result == ACCESSIBILITY_SETTINGS_OPENED -> "설정 화면 열림"
            result == ACCESSIBILITY_SETTINGS_OPEN_FAILED -> "설정 화면 열기 실패"
            result == FALLBACK_DISABLED -> "별도 알림 꺼짐"
            result == FALLBACK_SHOWN -> "별도 알림 표시됨"
            result == FALLBACK_PERMISSION_MISSING -> "건너뜀: 알림 권한 없음"
            result == PRE_SWITCH_WARNING_NOT_ATTEMPTED -> "아직 없음"
            result == PRE_SWITCH_WARNING_SHOWN -> "전환 예고 표시됨"
            result == PRE_SWITCH_WARNING_CANCELLED_FOR_LAUNCH -> "실행 시작으로 예고 취소됨"
            result == PRE_SWITCH_WARNING_SKIPPED_IMMEDIATE -> "건너뜀: 즉시 전환"
            result == PRE_SWITCH_WARNING_PERMISSION_MISSING -> "건너뜀: 알림 권한 없음"
            result == PRE_SWITCH_WARNING_FAILED -> "실패: 예고 알림 오류"
            else -> "실패: 예외 발생"
        }
    }

    private fun sanitizeTime(value: String, fallback: String): String {
        val minutes = parseMinutes(value.trim()) ?: return fallback
        return "%02d:%02d".format(Locale.US, minutes / 60, minutes % 60)
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
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
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
        val lastAccessibilityAttempt: String,
        val lastAccessibilityResult: String,
        val accessibilityPermissionStatus: String,
        val accessibilitySettingsOpenResult: String,
        val lastAccessibilityStatusCheck: String,
        val lastFallbackResult: String,
        val lastPreSwitchWarningMethod: String,
        val lastPreSwitchWarningNumber: String,
        val lastPreSwitchWarningResult: String,
    )
}
