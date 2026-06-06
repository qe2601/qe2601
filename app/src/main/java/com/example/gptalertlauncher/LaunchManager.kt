package com.example.gptalertlauncher

import android.content.Context
import android.os.Handler
import android.os.Looper

object LaunchManager {
    private val handler = Handler(Looper.getMainLooper())

    fun handleChatGptNotification(context: Context) {
        val appContext = context.applicationContext
        AppSettings.markDetected(appContext)

        val skipReason = skipReason(appContext)
        if (skipReason != null) {
            AppSettings.recordLaunchResult(appContext, skipReason)
            AppSettings.recordFallbackResult(appContext, AppSettings.FALLBACK_DISABLED)
            return
        }

        AppSettings.setLastLaunchMs(appContext, System.currentTimeMillis())
        scheduleLaunch(appContext, AppSettings.launchDelayMs(appContext), includeRetry = AppSettings.retryEnabled(appContext), isRetry = false)
    }

    fun testSilentLaunch(context: Context) {
        val appContext = context.applicationContext
        AppSettings.recordFallbackResult(appContext, AppSettings.FALLBACK_DISABLED)
        performLaunch(appContext, isRetry = false, allowFallback = false)
    }

    fun testLaunchWithRetry(context: Context) {
        val appContext = context.applicationContext
        AppSettings.recordFallbackResult(appContext, AppSettings.FALLBACK_DISABLED)
        performLaunch(appContext, isRetry = false, allowFallback = false)
        scheduleLaunch(appContext, AppSettings.retryDelayMs(appContext), includeRetry = false, isRetry = true)
    }

    private fun skipReason(context: Context): String? {
        return when {
            !AppSettings.isEnabled(context) -> AppSettings.RESULT_SKIPPED_DISABLED
            AppSettings.isQuietNow(context) -> AppSettings.RESULT_SKIPPED_QUIET
            AppSettings.isInCooldown(context) -> AppSettings.RESULT_SKIPPED_COOLDOWN
            DeviceState.screenOnOnlyAndScreenOff(context) -> AppSettings.RESULT_SKIPPED_SCREEN_OFF
            DeviceState.unlockedOnlyAndLocked(context) -> AppSettings.RESULT_SKIPPED_LOCKED
            else -> null
        }
    }

    private fun scheduleLaunch(context: Context, delayMs: Int, includeRetry: Boolean, isRetry: Boolean) {
        handler.postDelayed({
            val result = performLaunch(context, isRetry, allowFallback = !isRetry)
            if (includeRetry && result.resultText != AppSettings.RESULT_FAILED_NOT_INSTALLED) {
                scheduleLaunch(context, AppSettings.retryDelayMs(context), includeRetry = false, isRetry = true)
            }
        }, delayMs.toLong())
    }

    private fun performLaunch(context: Context, isRetry: Boolean, allowFallback: Boolean): ChatGptLauncher.LaunchResult {
        val result = ChatGptLauncher.launchChatGpt(context)
        if (isRetry) {
            AppSettings.recordRetryResult(context, result.resultText)
        } else {
            AppSettings.recordLaunchResult(context, result.resultText)
        }

        if (allowFallback && AppSettings.fallbackEnabled(context)) {
            val reason = if (result.resultText == AppSettings.RESULT_ATTEMPTED) {
                "실행을 시도했습니다. Android 또는 Samsung One UI가 백그라운드 자동 전환을 차단했을 수 있습니다."
            } else {
                result.resultText
            }
            ChatGptLauncher.showFallbackNotification(context, reason)
        } else if (allowFallback && !AppSettings.fallbackEnabled(context)) {
            AppSettings.recordFallbackResult(context, AppSettings.FALLBACK_DISABLED)
        }
        return result
    }
}
