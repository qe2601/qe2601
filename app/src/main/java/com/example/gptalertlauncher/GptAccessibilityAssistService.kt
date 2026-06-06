package com.example.gptalertlauncher

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.view.accessibility.AccessibilityEvent

class GptAccessibilityAssistService : AccessibilityService() {
    private var lastAssistAttemptMs: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        val now = System.currentTimeMillis()

        if (packageName == AppSettings.CHATGPT_PACKAGE) {
            if (AppSettings.pendingLaunchMs(this) > 0L) {
                AppSettings.recordAccessibilityResult(this, AppSettings.ACCESSIBILITY_SUCCESS_FOREGROUND, now)
                AppSettings.clearPendingLaunch(this)
            }
            return
        }

        if (!AppSettings.accessibilityAssist(this)) {
            AppSettings.recordAccessibilityResult(this, AppSettings.ACCESSIBILITY_DISABLED, now)
            return
        }

        val pendingMs = AppSettings.pendingLaunchMs(this)
        if (pendingMs <= 0L) {
            AppSettings.recordAccessibilityResult(this, AppSettings.ACCESSIBILITY_SKIPPED_NO_PENDING, now)
            return
        }

        if (!AppSettings.isPendingLaunchActive(this, now)) {
            AppSettings.recordAccessibilityResult(this, AppSettings.ACCESSIBILITY_SKIPPED_TIMEOUT, now)
            AppSettings.clearPendingLaunch(this)
            return
        }

        if (AppSettings.unlockedOnly(this) && isDeviceLocked()) {
            AppSettings.recordAccessibilityResult(this, AppSettings.ACCESSIBILITY_SKIPPED_LOCKED, now)
            return
        }

        if (isSensitivePackageName(packageName)) {
            AppSettings.recordAccessibilityResult(this, AppSettings.ACCESSIBILITY_SKIPPED_UNSAFE_PACKAGE, now)
            return
        }

        val timeoutMs = AppSettings.accessibilityTimeoutMs(this).toLong()
        if (now - lastAssistAttemptMs < timeoutMs) return
        lastAssistAttemptMs = now

        val result = LaunchManager.attemptLaunch(this)
        if (result == AppSettings.RESULT_ATTEMPTED) {
            AppSettings.recordAccessibilityResult(this, AppSettings.ACCESSIBILITY_ATTEMPTED, now)
        } else {
            AppSettings.recordAccessibilityResult(this, result, now)
        }
    }

    override fun onInterrupt() = Unit

    private fun isDeviceLocked(): Boolean {
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        return keyguardManager.isDeviceLocked
    }

    private fun isSensitivePackageName(packageName: String): Boolean {
        val lower = packageName.lowercase()
        val sensitiveTokens = listOf(
            "bank", "banking", "card", "pay", "payment", "wallet", "finance", "securities",
            "invest", "trading", "stock", "crypto", "auth", "otp", "password", "passkey", "secure",
            "security", "keyguard", "samsungpass", "biometric", "settings"
        )
        return sensitiveTokens.any { lower.contains(it) }
    }
}
