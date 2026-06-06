package com.example.gptalertlauncher

import android.app.KeyguardManager
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class ChatGptNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != AppSettings.CHATGPT_PACKAGE) return
        if (!AppSettings.isEnabled(this)) return
        if (AppSettings.isQuietNow(this)) return
        if (AppSettings.isInCooldown(this)) return
        if (AppSettings.screenOnOnly(this) && !isScreenOn()) return
        if (AppSettings.unlockedOnly(this) && isDeviceLocked()) return

        val now = System.currentTimeMillis()
        AppSettings.setLastLaunchMs(this, now)

        if (!ChatGptLauncher.isChatGptInstalled(this)) {
            ChatGptLauncher.showFallbackNotification(this, "ChatGPT is not installed or is not visible to this app.")
            return
        }

        val launched = ChatGptLauncher.launchChatGpt(this)
        val reason = if (launched) {
            "Android may block background app launches on some devices."
        } else {
            "Automatic launch was not possible."
        }
        ChatGptLauncher.showFallbackNotification(this, reason)
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
