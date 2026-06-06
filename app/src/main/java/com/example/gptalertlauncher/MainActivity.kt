package com.example.gptalertlauncher

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import android.app.Activity

class MainActivity : Activity() {
    private lateinit var statusContainer: LinearLayout
    private lateinit var cooldownEdit: EditText
    private lateinit var quietStartEdit: EditText
    private lateinit var quietEndEdit: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        val scrollView = ScrollView(this).apply { addView(root) }

        root.addView(title("GPT Alert Launcher"))
        root.addView(body("Detects official ChatGPT notifications locally and tries to open ChatGPT. If Android blocks the launch, a high-priority fallback notification lets you open ChatGPT immediately."))
        root.addView(body("Privacy: This app requests notification access so it can detect notifications from the ChatGPT app. It filters locally by package name and does not upload notification contents anywhere."))

        statusContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(section("Status"))
        root.addView(statusContainer)

        root.addView(section("Settings"))
        root.addView(switchRow("Automation enabled", AppSettings.isEnabled(this)) { _, checked ->
            AppSettings.setEnabled(this, checked)
        })
        root.addView(switchRow("Only run when screen is on", AppSettings.screenOnOnly(this)) { _, checked ->
            AppSettings.setScreenOnOnly(this, checked)
        })
        root.addView(switchRow("Only run when device is unlocked", AppSettings.unlockedOnly(this)) { _, checked ->
            AppSettings.setUnlockedOnly(this, checked)
        })

        cooldownEdit = editRow(root, "Cooldown seconds", AppSettings.cooldownSeconds(this).toString(), InputType.TYPE_CLASS_NUMBER)
        quietStartEdit = editRow(root, "Quiet hours start (HH:mm)", AppSettings.quietStart(this), InputType.TYPE_CLASS_DATETIME)
        quietEndEdit = editRow(root, "Quiet hours end (HH:mm)", AppSettings.quietEnd(this), InputType.TYPE_CLASS_DATETIME)
        root.addView(button("Save local settings") { saveSettings() })

        root.addView(section("Help and testing"))
        root.addView(button("Open notification listener access settings") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        root.addView(button("Open this app's notification settings") { openOwnNotificationSettings() })
        root.addView(button("Open battery optimization settings") {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        })
        root.addView(button("Open ChatGPT app info") { openChatGptAppInfo() })
        root.addView(button("Test fallback notification") {
            ChatGptLauncher.showFallbackNotification(this, "This is a test fallback notification.")
            Toast.makeText(this, "Fallback notification requested", Toast.LENGTH_SHORT).show()
        })
        root.addView(button("Test launching ChatGPT") {
            if (!ChatGptLauncher.launchChatGpt(this)) {
                ChatGptLauncher.showFallbackNotification(this, "ChatGPT could not be launched directly.")
            }
        })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            root.addView(button("Request notification permission") {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 200)
            })
        }

        setContentView(scrollView)
    }

    private fun refreshStatus() {
        statusContainer.removeAllViews()
        statusContainer.addView(statusLine("ChatGPT installed", ChatGptLauncher.isChatGptInstalled(this)))
        statusContainer.addView(statusLine("Notification access enabled", isNotificationListenerEnabled()))
        statusContainer.addView(statusLine("Post notification permission granted", hasPostNotificationPermission()))
        statusContainer.addView(statusLine("Battery optimization ignored", isIgnoringBatteryOptimizations()))
    }

    private fun saveSettings() {
        val cooldown = cooldownEdit.text.toString().toIntOrNull() ?: AppSettings.DEFAULT_COOLDOWN_SECONDS
        AppSettings.setCooldownSeconds(this, cooldown)
        AppSettings.setQuietStart(this, quietStartEdit.text.toString())
        AppSettings.setQuietEnd(this, quietEndEdit.text.toString())
        cooldownEdit.setText(AppSettings.cooldownSeconds(this).toString())
        quietStartEdit.setText(AppSettings.quietStart(this))
        quietEndEdit.setText(AppSettings.quietEnd(this))
        Toast.makeText(this, "Settings saved locally", Toast.LENGTH_SHORT).show()
    }

    private fun title(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 28f
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, 0, 0, 16)
    }

    private fun section(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 20f
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, 28, 0, 8)
    }

    private fun body(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 15f
        setPadding(0, 0, 0, 12)
    }

    private fun statusLine(label: String, ok: Boolean): TextView = body("${if (ok) "✓" else "⚠"} $label")

    private fun switchRow(label: String, checked: Boolean, listener: CompoundButton.OnCheckedChangeListener): View {
        return Switch(this).apply {
            text = label
            isChecked = checked
            textSize = 16f
            setPadding(0, 8, 0, 8)
            setOnCheckedChangeListener(listener)
        }
    }

    private fun editRow(root: LinearLayout, label: String, value: String, inputType: Int): EditText {
        root.addView(body(label))
        return EditText(this).apply {
            setText(value)
            this.inputType = inputType
            setSingleLine(true)
            root.addView(this)
        }
    }

    private fun button(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            setOnClickListener { onClick() }
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return enabled.contains(packageName)
    }

    private fun hasPostNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = getSystemService(PowerManager::class.java)
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun openOwnNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:$packageName") }
        }
        startActivity(intent)
    }

    private fun openChatGptAppInfo() {
        if (!ChatGptLauncher.isChatGptInstalled(this)) {
            Toast.makeText(this, "ChatGPT is not installed", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${AppSettings.CHATGPT_PACKAGE}")
        })
    }

}
