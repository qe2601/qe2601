package com.example.gptalertlauncher

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils

fun isAccessibilityAssistServiceEnabled(context: Context): Boolean {
    val expected = ComponentName(context, GptAccessibilityAssistService::class.java)
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false

    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabledServices)
    for (serviceName in splitter) {
        val enabledComponent = ComponentName.unflattenFromString(serviceName)
        if (enabledComponent != null &&
            enabledComponent.packageName == expected.packageName &&
            enabledComponent.className == expected.className
        ) {
            return true
        }
        if (serviceName.equals(expected.flattenToString(), ignoreCase = true) ||
            serviceName.equals(expected.flattenToShortString(), ignoreCase = true)
        ) {
            return true
        }
    }
    return false
}
