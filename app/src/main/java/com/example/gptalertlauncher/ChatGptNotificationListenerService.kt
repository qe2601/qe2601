package com.example.gptalertlauncher

import android.app.KeyguardManager
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class ChatGptNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != AppSettings.CHATGPT_PACKAGE) return

        val now = System.currentTimeMillis()
        AppSettings.recordDetected(this, now)

        if (!AppSettings.isEnabled(this) || !AppSettings.silentAutoLaunch(this)) {
            AppSettings.recordLaunchResult(this, AppSettings.RESULT_SKIPPED_DISABLED, now)
            AppSettings.recordFallbackResult(this, AppSettings.FALLBACK_DISABLED)
            return
        }
        if (AppSettings.isInCooldown(this, now)) {
            AppSettings.recordLaunchResult(this, AppSettings.RESULT_SKIPPED_COOLDOWN, now)
            AppSettings.recordFallbackResult(this, AppSettings.FALLBACK_DISABLED)
            return
        }
        if (AppSettings.isQuietNow(this)) {
            AppSettings.recordLaunchResult(this, AppSettings.RESULT_SKIPPED_QUIET_HOURS, now)
            AppSettings.recordFallbackResult(this, AppSettings.FALLBACK_DISABLED)
            return
        }
        if (AppSettings.screenOnOnly(this) && !isScreenOn()) {
            AppSettings.recordLaunchResult(this, AppSettings.RESULT_SKIPPED_SCREEN_OFF, now)
            AppSettings.recordFallbackResult(this, AppSettings.FALLBACK_DISABLED)
            return
        }
        if (AppSettings.unlockedOnly(this) && isDeviceLocked()) {
            AppSettings.recordLaunchResult(this, AppSettings.RESULT_SKIPPED_DEVICE_LOCKED, now)
            AppSettings.recordFallbackResult(this, AppSettings.FALLBACK_DISABLED)
            return
        }

        AppSettings.setLastLaunchMs(this, now)
        LaunchManager.scheduleAutoLaunch(this)
    }

    private fun isScreenOn(): Boolean {
        val powerManager = getSystemService(PowerManager::class.java)
        return powerManager.isInteractive
    }

    private fun isDeviceLocked(): Boolean {
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        return keyguardManager.isDeviceLocked
    }
}
