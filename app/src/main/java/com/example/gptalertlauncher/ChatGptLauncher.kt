package com.example.gptalertlauncher

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

object ChatGptLauncher {
    private const val FALLBACK_CHANNEL_ID = "chatgpt_fallback"
    private const val FALLBACK_NOTIFICATION_ID = 1001

    data class LaunchResult(val resultText: String, val attempted: Boolean)

    fun isChatGptInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(AppSettings.CHATGPT_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun launchChatGpt(context: Context): LaunchResult {
        if (!isChatGptInstalled(context)) return LaunchResult(AppSettings.RESULT_FAILED_NOT_INSTALLED, false)
        val intent = chatGptLaunchIntent(context) ?: return LaunchResult(AppSettings.RESULT_FAILED_NOT_INSTALLED, false)
        return try {
            context.startActivity(intent)
            LaunchResult(AppSettings.RESULT_ATTEMPTED, true)
        } catch (exception: RuntimeException) {
            LaunchResult("실패: 예외 발생 (${exception.javaClass.simpleName})", false)
        }
    }

    fun chatGptLaunchIntent(context: Context): Intent? {
        return context.packageManager.getLaunchIntentForPackage(AppSettings.CHATGPT_PACKAGE)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
    }

    fun showFallbackNotification(context: Context, reason: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            AppSettings.recordFallbackResult(context, AppSettings.FALLBACK_PERMISSION_MISSING)
            return AppSettings.FALLBACK_PERMISSION_MISSING
        }
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        ensureFallbackChannel(notificationManager)

        val launchIntent = chatGptLaunchIntent(context) ?: Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = android.app.Notification.Builder(context, FALLBACK_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("ChatGPT 열기")
            .setContentText(reason)
            .setStyle(android.app.Notification.BigTextStyle().bigText("$reason 탭하면 ChatGPT를 엽니다."))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(android.app.Notification.PRIORITY_HIGH)
            .setCategory(android.app.Notification.CATEGORY_REMINDER)
            .addAction(android.R.drawable.ic_menu_send, "ChatGPT 열기", pendingIntent)
            .build()

        return try {
            notificationManager.notify(FALLBACK_NOTIFICATION_ID, notification)
            AppSettings.recordFallbackResult(context, AppSettings.FALLBACK_SHOWN)
            AppSettings.FALLBACK_SHOWN
        } catch (exception: RuntimeException) {
            val result = "실패: 예외 발생 (${exception.javaClass.simpleName})"
            AppSettings.recordFallbackResult(context, result)
            result
        }
    }

    private fun ensureFallbackChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FALLBACK_CHANNEL_ID,
                "ChatGPT 실행 보조 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "ChatGPT 자동 실행이 차단되거나 실패했을 때 선택적으로 표시하는 알림입니다."
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
