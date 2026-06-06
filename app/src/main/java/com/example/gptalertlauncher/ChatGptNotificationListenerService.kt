package com.example.gptalertlauncher

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class ChatGptNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != AppSettings.CHATGPT_PACKAGE) return
        LaunchManager.handleChatGptNotification(this)
    }
}
