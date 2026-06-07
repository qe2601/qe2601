package com.example.gptalertlauncher

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
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

class MainActivity : Activity() {
    private lateinit var statusContainer: LinearLayout
    private lateinit var accessibilitySetupContainer: LinearLayout
    private lateinit var diagnosticsContainer: LinearLayout
    private lateinit var settingsSummaryContainer: LinearLayout
    private lateinit var cooldownEdit: EditText
    private lateinit var launchDelayEdit: EditText
    private lateinit var retryDelayEdit: EditText
    private lateinit var accessibilityTimeoutEdit: EditText
    private lateinit var quietStartEdit: EditText
    private lateinit var quietEndEdit: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        refreshAll()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 36)
        }
        val scrollView = ScrollView(this).apply { addView(root) }

        root.addView(title("GPT 알림 자동 실행 v2"))
        root.addView(body("ChatGPT 알림이 오면 가능한 범위에서 ChatGPT 앱을 자동으로 열어 봅니다. Android/Samsung One UI 정책 때문에 자동 전환이 항상 보장되지는 않습니다."))
        root.addView(body("이 앱은 ChatGPT 앱의 알림을 감지하기 위해 알림 접근 권한을 요청합니다. 알림은 기기 안에서만 패키지명으로 필터링하며, 알림 내용은 외부로 전송하지 않습니다."))

        statusContainer = verticalContainer()
        root.addView(section("상태 요약"))
        root.addView(statusContainer)

        root.addView(section("자동 실행 설정"))
        root.addView(body("기본 전환 예고 방식은 노티바 카운트다운입니다. 예고 알림에는 전환까지 남은 숫자만 표시합니다."))
        root.addView(switchRow("자동 실행 사용", AppSettings.isEnabled(this)) { _, checked ->
            AppSettings.setEnabled(this, checked)
            refreshAll()
        })
        root.addView(switchRow("무음 자동 실행 모드", AppSettings.silentAutoLaunch(this)) { _, checked ->
            AppSettings.setSilentAutoLaunch(this, checked)
            refreshAll()
        })
        root.addView(switchRow("실패 시 별도 알림 표시", AppSettings.showFallbackOnFailure(this)) { _, checked ->
            AppSettings.setShowFallbackOnFailure(this, checked)
            refreshAll()
        })

        root.addView(section("접근성 보조 설정"))
        root.addView(body("접근성 보조 모드는 자동 전환 성공률을 높이기 위한 선택 기능입니다. 이 앱은 화면 텍스트를 수집하거나 외부로 전송하지 않으며, 금융/결제/비밀번호/OTP 화면 자동조작을 하지 않습니다."))
        root.addView(switchRow("접근성 보조 모드", AppSettings.accessibilityAssist(this)) { _, checked ->
            AppSettings.setAccessibilityAssist(this, checked)
            AppSettings.recordAccessibilityResult(this, if (checked) AppSettings.ACCESSIBILITY_SKIPPED_NO_PENDING else AppSettings.ACCESSIBILITY_DISABLED)
            refreshAll()
        })
        accessibilitySetupContainer = verticalContainer()
        root.addView(accessibilitySetupContainer)

        root.addView(section("자동 실행 세부 설정"))
        root.addView(switchRow("화면 켜짐 상태에서만 실행", AppSettings.screenOnOnly(this)) { _, checked ->
            AppSettings.setScreenOnOnly(this, checked)
            refreshAll()
        })
        root.addView(switchRow("잠금 해제 상태에서만 실행", AppSettings.unlockedOnly(this)) { _, checked ->
            AppSettings.setUnlockedOnly(this, checked)
            refreshAll()
        })
        root.addView(switchRow("재시도 사용", AppSettings.retryEnabled(this)) { _, checked ->
            AppSettings.setRetryEnabled(this, checked)
            refreshAll()
        })
        cooldownEdit = editRow(root, "쿨다운(초)", AppSettings.cooldownSeconds(this).toString(), InputType.TYPE_CLASS_NUMBER)
        launchDelayEdit = editRow(root, "실행 지연(ms)", AppSettings.launchDelayMs(this).toString(), InputType.TYPE_CLASS_NUMBER)
        retryDelayEdit = editRow(root, "재시도 지연(ms)", AppSettings.retryDelayMs(this).toString(), InputType.TYPE_CLASS_NUMBER)
        accessibilityTimeoutEdit = editRow(root, "접근성 보조 제한 시간(ms)", AppSettings.accessibilityTimeoutMs(this).toString(), InputType.TYPE_CLASS_NUMBER)
        quietStartEdit = editRow(root, "조용한 시간 시작", AppSettings.quietStart(this), InputType.TYPE_CLASS_DATETIME)
        quietEndEdit = editRow(root, "조용한 시간 종료", AppSettings.quietEnd(this), InputType.TYPE_CLASS_DATETIME)
        root.addView(button("설정 저장") { saveSettings() })

        root.addView(section("테스트"))
        root.addView(button("무음 자동 실행 테스트") {
            val result = LaunchManager.attemptLaunch(this)
            AppSettings.recordLaunchResult(this, result)
            Toast.makeText(this, AppSettings.koreanResult(result), Toast.LENGTH_SHORT).show()
            refreshAll()
        })
        root.addView(button("재시도 실행 테스트") {
            val first = LaunchManager.attemptLaunch(this)
            AppSettings.recordLaunchResult(this, first)
            val retry = LaunchManager.attemptLaunch(this)
            AppSettings.recordRetryResult(this, retry)
            Toast.makeText(this, "재시도 결과: ${AppSettings.koreanResult(retry)}", Toast.LENGTH_SHORT).show()
            refreshAll()
        })
        root.addView(button("접근성 보조 테스트") {
            AppSettings.markPendingLaunch(this)
            val result = LaunchManager.attemptLaunch(this)
            AppSettings.recordAccessibilityResult(this, if (result == AppSettings.RESULT_ATTEMPTED) AppSettings.ACCESSIBILITY_ATTEMPTED else result)
            Toast.makeText(this, AppSettings.koreanResult(if (result == AppSettings.RESULT_ATTEMPTED) AppSettings.ACCESSIBILITY_ATTEMPTED else result), Toast.LENGTH_SHORT).show()
            refreshAll()
        })
        root.addView(button("별도 알림 테스트") {
            val result = LaunchManager.showFallbackNotification(this)
            AppSettings.recordFallbackResult(this, result)
            Toast.makeText(this, AppSettings.koreanResult(result), Toast.LENGTH_SHORT).show()
            refreshAll()
        })
        root.addView(button("진단 기록 지우기") {
            AppSettings.clearDiagnostics(this)
            Toast.makeText(this, "진단 기록을 지웠습니다", Toast.LENGTH_SHORT).show()
            refreshAll()
        })

        root.addView(section("권한/설정 바로가기"))
        root.addView(button("알림 접근 권한 설정 열기") { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) })
        root.addView(button("접근성 설정 열기") { openAccessibilityAssistSettings() })
        root.addView(button("이 앱 알림 설정 열기") { openOwnNotificationSettings() })
        root.addView(button("배터리 최적화 설정 열기") { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) })
        root.addView(button("ChatGPT 앱 정보 열기") { openChatGptAppInfo() })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            root.addView(button("알림 표시 권한 요청") { requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 200) })
        }

        diagnosticsContainer = verticalContainer()
        root.addView(section("진단 정보"))
        root.addView(diagnosticsContainer)

        settingsSummaryContainer = verticalContainer()
        root.addView(section("현재 설정 요약"))
        root.addView(settingsSummaryContainer)

        setContentView(scrollView)
    }

    private fun refreshAll() {
        val accessibilityEnabled = isAccessibilityAssistServiceEnabled(this)
        AppSettings.recordAccessibilityPermissionStatus(this, accessibilityEnabled)
        refreshStatus(accessibilityEnabled)
        refreshAccessibilitySetup(accessibilityEnabled)
        refreshDiagnostics(accessibilityEnabled)
        refreshSettingsSummary()
    }

    private fun refreshStatus(accessibilityEnabled: Boolean) {
        statusContainer.removeAllViews()
        statusContainer.addView(statusLine("ChatGPT 설치됨", LaunchManager.isChatGptInstalled(this)))
        statusContainer.addView(statusLine("알림 접근 권한 허용됨", isNotificationListenerEnabled()))
        statusContainer.addView(statusLine("접근성 보조 권한 허용됨", accessibilityEnabled))
        statusContainer.addView(statusLine("알림 표시 권한 허용됨", hasPostNotificationPermission()))
        statusContainer.addView(statusLine("배터리 최적화 제외됨", isIgnoringBatteryOptimizations()))
        statusContainer.addView(firstRunAccessibilityCard(accessibilityEnabled))
    }

    private fun refreshAccessibilitySetup(accessibilityEnabled: Boolean) {
        accessibilitySetupContainer.removeAllViews()
        accessibilitySetupContainer.addView(body("접근성 보조 권한: ${onOff(accessibilityEnabled)}"))
        accessibilitySetupContainer.addView(body("앱 내 접근성 보조 모드: ${onOff(AppSettings.accessibilityAssist(this))}"))
        accessibilitySetupContainer.addView(body(if (accessibilityEnabled) "접근성 보조가 켜져 있습니다." else "접근성 보조가 꺼져 있습니다."))
        accessibilitySetupContainer.addView(body("접근성 보조를 켜려면 Android 설정 화면에서 ‘GPT 알림 접근성 보조’를 선택한 뒤 사용 중으로 바꿔 주세요."))
        accessibilitySetupContainer.addView(body("Samsung One UI에서는 접근성 > 설치된 앱 > GPT 알림 접근성 보조에서 켤 수 있습니다."))
        accessibilitySetupContainer.addView(button("접근성 보조 설정 열기") { openAccessibilityAssistSettings() })
        accessibilitySetupContainer.addView(button("접근성 보조 상태 다시 확인") { refreshAll() })
        accessibilitySetupContainer.addView(button("접근성 보조 사용 방법 보기") { showAccessibilityAssistGuide() })
    }

    private fun firstRunAccessibilityCard(accessibilityEnabled: Boolean): View {
        val notificationEnabled = isNotificationListenerEnabled()
        val inAppAssist = AppSettings.accessibilityAssist(this)
        val card = verticalContainer().apply { setPadding(0, 12, 0, 12) }
        when {
            notificationEnabled && !accessibilityEnabled -> {
                card.addView(body("자동 전환 성공률을 높이려면 접근성 보조를 켜세요."))
                card.addView(button("접근성 보조 설정 열기") { openAccessibilityAssistSettings() })
            }
            accessibilityEnabled && !inAppAssist -> {
                card.addView(body("Android 접근성 권한은 켜져 있지만 앱 내 접근성 보조 모드가 꺼져 있습니다."))
                card.addView(button("앱 내 접근성 보조 모드 켜기") {
                    AppSettings.setAccessibilityAssist(this, true)
                    AppSettings.recordAccessibilityResult(this, AppSettings.ACCESSIBILITY_SKIPPED_NO_PENDING)
                    refreshAll()
                })
            }
            accessibilityEnabled && inAppAssist -> card.addView(body("접근성 보조 준비 완료"))
        }
        return card
    }

    private fun refreshDiagnostics(accessibilityEnabled: Boolean) {
        val snapshot = AppSettings.diagnosticsSnapshot(this)
        diagnosticsContainer.removeAllViews()
        diagnosticsContainer.addView(body("마지막 ChatGPT 알림 감지: ${snapshot.lastDetected}"))
        diagnosticsContainer.addView(body("마지막 실행 시도: ${snapshot.lastAttempt}"))
        diagnosticsContainer.addView(body("마지막 실행 결과: ${AppSettings.koreanResult(snapshot.lastResult)}"))
        diagnosticsContainer.addView(body("마지막 재시도 시간: ${snapshot.lastRetryAttempt}"))
        diagnosticsContainer.addView(body("마지막 재시도 결과: ${AppSettings.koreanResult(snapshot.lastRetryResult)}"))
        diagnosticsContainer.addView(body("마지막 접근성 보조 시도: ${snapshot.lastAccessibilityAttempt}"))
        diagnosticsContainer.addView(body("마지막 접근성 보조 결과: ${AppSettings.koreanResult(snapshot.lastAccessibilityResult)}"))
        diagnosticsContainer.addView(body("접근성 권한 상태: ${AppSettings.koreanResult(snapshot.accessibilityPermissionStatus)}"))
        diagnosticsContainer.addView(body("접근성 설정 화면 열기 결과: ${AppSettings.koreanResult(snapshot.accessibilitySettingsOpenResult)}"))
        diagnosticsContainer.addView(body("마지막 접근성 상태 확인 시간: ${snapshot.lastAccessibilityStatusCheck}"))
        diagnosticsContainer.addView(body("마지막 별도 알림 결과: ${AppSettings.koreanResult(snapshot.lastFallbackResult)}"))
        diagnosticsContainer.addView(body("마지막 전환 예고 방식: ${snapshot.lastPreSwitchWarningMethod}"))
        diagnosticsContainer.addView(body("마지막 전환 예고 숫자: ${snapshot.lastPreSwitchWarningNumber}"))
        diagnosticsContainer.addView(body("마지막 전환 예고 결과: ${AppSettings.koreanResult(snapshot.lastPreSwitchWarningResult)}"))
        diagnosticsContainer.addView(body("현재 권한 상태: 알림 접근 ${yesNo(isNotificationListenerEnabled())}, 접근성 보조 ${yesNo(accessibilityEnabled)}, 알림 표시 ${yesNo(hasPostNotificationPermission())}"))
    }

    private fun refreshSettingsSummary() {
        settingsSummaryContainer.removeAllViews()
        settingsSummaryContainer.addView(body(AppSettings.settingsSnapshot(this)))
    }

    private fun saveSettings() {
        val cooldown = cooldownEdit.text.toString().toIntOrNull() ?: AppSettings.DEFAULT_COOLDOWN_SECONDS
        val launchDelay = launchDelayEdit.text.toString().toIntOrNull() ?: AppSettings.DEFAULT_LAUNCH_DELAY_MS
        val retryDelay = retryDelayEdit.text.toString().toIntOrNull() ?: AppSettings.DEFAULT_RETRY_DELAY_MS
        val accessibilityTimeout = accessibilityTimeoutEdit.text.toString().toIntOrNull() ?: AppSettings.DEFAULT_ACCESSIBILITY_TIMEOUT_MS
        AppSettings.setCooldownSeconds(this, cooldown)
        AppSettings.setLaunchDelayMs(this, launchDelay)
        AppSettings.setRetryDelayMs(this, retryDelay)
        AppSettings.setAccessibilityTimeoutMs(this, accessibilityTimeout)
        AppSettings.setQuietStart(this, quietStartEdit.text.toString())
        AppSettings.setQuietEnd(this, quietEndEdit.text.toString())
        cooldownEdit.setText(AppSettings.cooldownSeconds(this).toString())
        launchDelayEdit.setText(AppSettings.launchDelayMs(this).toString())
        retryDelayEdit.setText(AppSettings.retryDelayMs(this).toString())
        accessibilityTimeoutEdit.setText(AppSettings.accessibilityTimeoutMs(this).toString())
        quietStartEdit.setText(AppSettings.quietStart(this))
        quietEndEdit.setText(AppSettings.quietEnd(this))
        Toast.makeText(this, "설정을 저장했습니다", Toast.LENGTH_SHORT).show()
        refreshAll()
    }

    private fun title(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 27f
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, 0, 0, 14)
    }

    private fun section(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 20f
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, 24, 0, 8)
    }

    private fun body(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 15f
        setPadding(0, 0, 0, 10)
    }

    private fun statusLine(label: String, ok: Boolean): TextView = body("${if (ok) "✓" else "⚠"} $label")

    private fun switchRow(label: String, checked: Boolean, listener: CompoundButton.OnCheckedChangeListener): View {
        return Switch(this).apply {
            text = label
            isChecked = checked
            textSize = 17f
            minHeight = 64
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
            textSize = 18f
            minHeight = 64
            root.addView(this)
        }
    }

    private fun button(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 16f
            minHeight = 64
            setOnClickListener { onClick() }
        }
    }

    private fun verticalContainer(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return enabled.split(':').any { it.contains(packageName) }
    }

    private fun hasPostNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = getSystemService(PowerManager::class.java)
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun openAccessibilityAssistSettings() {
        val directIntent = Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").apply {
            putExtra("android.provider.extra.ACCESSIBILITY_SERVICE_COMPONENT_NAME", ComponentName(this@MainActivity, GptAccessibilityAssistService::class.java).flattenToString())
        }
        val fallbackIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        val opened = openFirstAvailableSettings(listOf(directIntent, fallbackIntent))
        val result = if (opened) AppSettings.ACCESSIBILITY_SETTINGS_OPENED else AppSettings.ACCESSIBILITY_SETTINGS_OPEN_FAILED
        AppSettings.recordAccessibilitySettingsOpenResult(this, result)
        if (!opened) {
            Toast.makeText(this, "접근성 설정을 열 수 없습니다. Android 설정에서 접근성 > 설치된 앱을 확인하세요.", Toast.LENGTH_LONG).show()
        }
        refreshAll()
    }

    private fun openFirstAvailableSettings(intents: List<Intent>): Boolean {
        for (intent in intents) {
            try {
                startActivity(intent)
                return true
            } catch (_: RuntimeException) {
                // Try the next safe settings shortcut.
            }
        }
        return false
    }

    private fun showAccessibilityAssistGuide() {
        AlertDialog.Builder(this)
            .setTitle("접근성 보조 사용 방법")
            .setMessage("접근성 보조를 켜려면 Android 설정 화면에서 ‘GPT 알림 접근성 보조’를 선택한 뒤 사용 중으로 바꿔 주세요.\n\nSamsung One UI에서는 접근성 > 설치된 앱 > GPT 알림 접근성 보조에서 켤 수 있습니다.")
            .setPositiveButton("확인", null)
            .show()
    }

    private fun openOwnNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply { putExtra(Settings.EXTRA_APP_PACKAGE, packageName) }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:$packageName") }
        }
        startActivity(intent)
    }

    private fun openChatGptAppInfo() {
        if (!LaunchManager.isChatGptInstalled(this)) {
            Toast.makeText(this, "ChatGPT가 설치되어 있지 않습니다", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${AppSettings.CHATGPT_PACKAGE}")
        })
    }

    private fun yesNo(value: Boolean): String = if (value) "허용됨" else "필요"
    private fun onOff(value: Boolean): String = if (value) "켜짐" else "꺼짐"
}
