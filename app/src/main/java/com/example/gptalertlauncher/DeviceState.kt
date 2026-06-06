package com.example.gptalertlauncher

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager

object DeviceState {
    fun isScreenOn(context: Context): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java)
        return powerManager.isInteractive
    }

    fun isDeviceLocked(context: Context): Boolean {
        val keyguardManager = context.getSystemService(KeyguardManager::class.java)
        return keyguardManager.isDeviceLocked
    }

    fun screenOnOnlyAndScreenOff(context: Context): Boolean = AppSettings.screenOnOnly(context) && !isScreenOn(context)
    fun unlockedOnlyAndLocked(context: Context): Boolean = AppSettings.unlockedOnly(context) && isDeviceLocked(context)
}
