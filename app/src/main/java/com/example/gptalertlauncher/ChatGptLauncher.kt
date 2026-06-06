package com.example.gptalertlauncher

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

object ChatGptLauncher {
    private const val FALLBACK_CHANNEL_ID = "chatgpt_fallback"
    private const val FALLBACK_NOTIFICATION_ID = 1001

    fun isChatGptInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(AppSettings.CHATGPT_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun launchChatGpt(context: Context): Boolean {
        val intent = chatGptLaunchIntent(context) ?: return false
        return try {
            context.startActivity(intent)
            true
        } catch (_: RuntimeException) {
            false
        }
    }

    fun chatGptLaunchIntent(context: Context): Intent? {
        return context.packageManager.getLaunchIntentForPackage(AppSettings.CHATGPT_PACKAGE)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    fun showFallbackNotification(context: Context, reason: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        ensureFallbackChannel(notificationManager)

        val launchIntent = chatGptLaunchIntent(context) ?: Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = android.app.Notification.Builder(context, FALLBACK_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("ChatGPT notification detected")
            .setContentText(reason)
            .setStyle(android.app.Notification.BigTextStyle().bigText("$reason Tap to open ChatGPT."))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(android.app.Notification.PRIORITY_HIGH)
            .setCategory(android.app.Notification.CATEGORY_REMINDER)
            .addAction(android.R.drawable.ic_menu_send, "Open ChatGPT", pendingIntent)
            .build()

        try {
            notificationManager.notify(FALLBACK_NOTIFICATION_ID, notification)
        } catch (_: RuntimeException) {
            // Android may reject notifications if the user disabled them; keep all handling local and fail safely.
        }
    }

    private fun ensureFallbackChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FALLBACK_CHANNEL_ID,
                "ChatGPT launch fallback",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority fallback alerts when automatic ChatGPT launch is blocked or unavailable."
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
