package com.example.gptalertlauncher

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlin.math.ceil

object LaunchManager {
    private const val FALLBACK_CHANNEL_ID = "chatgpt_fallback"
    private const val FALLBACK_NOTIFICATION_ID = 1001
    private const val PRE_SWITCH_WARNING_CHANNEL_ID = "chatgpt_pre_switch_warning"
    private const val PRE_SWITCH_WARNING_NOTIFICATION_ID = 1002
    private const val PRE_SWITCH_WARNING_TITLE = "ChatGPT 자동 전환"
    private var lastPreSwitchWarningNumber = "없음"
    private var preSwitchWarningNotificationShown = false
    private val handler = Handler(Looper.getMainLooper())

    fun isChatGptInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(AppSettings.CHATGPT_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun schedulePreSwitchThenLaunch(context: Context) {
        val appContext = context.applicationContext
        AppSettings.recordFallbackResult(appContext, AppSettings.FALLBACK_DISABLED)
        val warningMs = AppSettings.preSwitchWarningMs(appContext)
        val mode = AppSettings.preSwitchWarningMode()

        if (mode == AppSettings.PRE_SWITCH_WARNING_MODE_COUNTDOWN_NOTIFICATION && warningMs > 0) {
            AppSettings.recordPreSwitchWarning(appContext, "없음", AppSettings.PRE_SWITCH_WARNING_STARTED)
            val warningResult = schedulePreSwitchCountdown(appContext, warningMs)
            handler.postDelayed({
                AppSettings.recordPreSwitchWarning(appContext, lastPreSwitchWarningNumber, AppSettings.PRE_SWITCH_WARNING_COMPLETED)
                startLaunchFlowAfterPreSwitch(appContext)
            }, warningMs.toLong())
            if (warningResult == AppSettings.PRE_SWITCH_WARNING_PERMISSION_MISSING) {
                return
            }
            return
        }

        lastPreSwitchWarningNumber = "없음"
        val skippedResult = if (warningMs <= 0) {
            AppSettings.PRE_SWITCH_WARNING_ZERO_MS
        } else {
            AppSettings.PRE_SWITCH_WARNING_OFF
        }
        AppSettings.recordPreSwitchWarning(appContext, lastPreSwitchWarningNumber, skippedResult)
        startLaunchFlowAfterPreSwitch(appContext)
    }

    private fun startLaunchFlowAfterPreSwitch(context: Context) {
        cancelPreSwitchWarning(context)
        AppSettings.recordPreSwitchWarning(context, lastPreSwitchWarningNumber, AppSettings.PRE_SWITCH_WARNING_LAUNCH_STARTING)
        AppSettings.setLastLaunchMs(context, System.currentTimeMillis())
        AppSettings.markPendingLaunch(context)
        if (!AppSettings.accessibilityAssist(context)) {
            AppSettings.recordAccessibilityResult(context, AppSettings.ACCESSIBILITY_DISABLED)
        }
        handler.postDelayed({
            val result = attemptLaunch(context)
            AppSettings.recordLaunchResult(context, result)
            if (AppSettings.retryEnabled(context)) {
                handler.postDelayed({
                    val retryResult = attemptLaunch(context)
                    AppSettings.recordRetryResult(context, retryResult)
                    if (result != AppSettings.RESULT_ATTEMPTED && retryResult != AppSettings.RESULT_ATTEMPTED) {
                        maybeShowFallback(context)
                    }
                }, AppSettings.retryDelayMs(context).toLong())
            } else if (result != AppSettings.RESULT_ATTEMPTED) {
                maybeShowFallback(context)
            }
        }, AppSettings.launchDelayMs(context).toLong())
    }

    fun attemptLaunch(context: Context): String {
        val intent = chatGptLaunchIntent(context) ?: return AppSettings.RESULT_FAILED_NOT_INSTALLED
        return try {
            context.startActivity(intent)
            AppSettings.RESULT_ATTEMPTED
        } catch (exception: RuntimeException) {
            AppSettings.RESULT_FAILED_EXCEPTION_PREFIX + exception.javaClass.simpleName
        }
    }

    fun chatGptLaunchIntent(context: Context): Intent? {
        return context.packageManager.getLaunchIntentForPackage(AppSettings.CHATGPT_PACKAGE)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
    }

    fun maybeShowFallback(context: Context) {
        if (!AppSettings.showFallbackOnFailure(context)) {
            AppSettings.recordFallbackResult(context, AppSettings.FALLBACK_DISABLED)
            return
        }
        val result = showFallbackNotification(context)
        AppSettings.recordFallbackResult(context, result)
    }

    fun showFallbackNotification(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
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
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = Notification.Builder(context, FALLBACK_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("ChatGPT 열기")
            .setContentText("자동 실행이 차단되었을 수 있습니다. 탭하여 ChatGPT를 여세요.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(Notification.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_REMINDER)
            .addAction(android.R.drawable.ic_menu_send, "ChatGPT 열기", pendingIntent)
            .build()

        return try {
            notificationManager.notify(FALLBACK_NOTIFICATION_ID, notification)
            AppSettings.FALLBACK_SHOWN
        } catch (_: RuntimeException) {
            AppSettings.RESULT_FAILED_EXCEPTION_PREFIX + "RuntimeException"
        }
    }

    private fun schedulePreSwitchCountdown(context: Context, warningMs: Int): String {
        preSwitchWarningNotificationShown = false
        lastPreSwitchWarningNumber = countdownNumber(warningMs)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            AppSettings.recordPreSwitchWarning(context, lastPreSwitchWarningNumber, AppSettings.PRE_SWITCH_WARNING_PERMISSION_MISSING)
            return AppSettings.PRE_SWITCH_WARNING_PERMISSION_MISSING
        }
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        ensurePreSwitchWarningChannel(notificationManager)
        for ((delay, number) in countdownTicks(warningMs)) {
            handler.postDelayed({ showPreSwitchWarningNumber(context, notificationManager, number, warningMs) }, delay)
        }
        return AppSettings.PRE_SWITCH_WARNING_STARTED
    }

    private fun showPreSwitchWarningNumber(
        context: Context,
        notificationManager: NotificationManager,
        number: String,
        warningMs: Int,
    ) {
        val notification = Notification.Builder(context, PRE_SWITCH_WARNING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(PRE_SWITCH_WARNING_TITLE)
            .setContentText(number)
            .setAutoCancel(true)
            .setTimeoutAfter(warningMs.toLong())
            .setPriority(Notification.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_STATUS)
            .build()

        try {
            notificationManager.notify(PRE_SWITCH_WARNING_NOTIFICATION_ID, notification)
            lastPreSwitchWarningNumber = number
            preSwitchWarningNotificationShown = true
            AppSettings.recordPreSwitchWarning(context, number, "${AppSettings.PRE_SWITCH_WARNING_NUMBER_PREFIX}: $number")
        } catch (_: RuntimeException) {
            AppSettings.recordPreSwitchWarning(context, number, AppSettings.PRE_SWITCH_WARNING_FAILED)
        }
    }

    private fun cancelPreSwitchWarning(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(PRE_SWITCH_WARNING_NOTIFICATION_ID)
        preSwitchWarningNotificationShown = false
    }

    private fun countdownTicks(warningMs: Int): List<Pair<Long, String>> {
        if (warningMs <= 1000) return listOf(0L to countdownNumber(warningMs))
        val tickCount = ceil(warningMs / 500.0).toInt()
        return (tickCount downTo 2).map { tick ->
            val remainingMs = tick * 500
            val delay = (warningMs - remainingMs.toLong()).coerceAtLeast(0L)
            delay to countdownNumber(remainingMs)
        }.distinctBy { it.second }
    }

    private fun countdownNumber(remainingMs: Int): String {
        return if (remainingMs % 1000 == 0) {
            (remainingMs / 1000).toString()
        } else {
            String.format(java.util.Locale.US, "%.1f", remainingMs / 1000.0)
        }
    }

    private fun ensureFallbackChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FALLBACK_CHANNEL_ID,
                "ChatGPT 별도 알림",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "자동 실행 실패 시 사용자가 직접 ChatGPT를 열 수 있는 별도 알림입니다."
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun ensurePreSwitchWarningChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PRE_SWITCH_WARNING_CHANNEL_ID,
                AppSettings.PRE_SWITCH_WARNING_MODE_COUNTDOWN_NOTIFICATION,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "전환까지 남은 숫자만 표시하는 짧은 예고 알림입니다."
                enableVibration(false)
                enableLights(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
