package com.example.gptalertlauncher

import android.Manifest
import android.app.Activity
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
    private lateinit var diagnosticsContainer: LinearLayout
    private lateinit var settingsSummaryContainer: LinearLayout
    private lateinit var cooldownEdit: EditText
    private lateinit var launchDelayEdit: EditText
    private lateinit var retryDelayEdit: EditText
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

        root.addView(title("GPT 알림 자동 실행"))
        root.addView(body("ChatGPT 알림이 오면 가능한 범위에서 ChatGPT 앱을 자동으로 열어 봅니다. Android/Samsung One UI 정책 때문에 자동 전환이 항상 보장되지는 않습니다."))
        root.addView(body("이 앱은 ChatGPT 앱의 알림을 감지하기 위해 알림 접근 권한을 요청합니다. 알림은 기기 안에서만 패키지명으로 필터링하며, 알림 내용은 외부로 전송하지 않습니다."))

        statusContainer = verticalContainer()
        root.addView(section("상태 요약"))
        root.addView(statusContainer)

        root.addView(section("자동 실행 설정"))
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

        root.addView(section("자동 실행 세부 설정"))
        cooldownEdit = editRow(root, "쿨다운(초)", AppSettings.cooldownSeconds(this).toString(), InputType.TYPE_CLASS_NUMBER)
        launchDelayEdit = editRow(root, "실행 지연(ms)", AppSettings.launchDelayMs(this).toString(), InputType.TYPE_CLASS_NUMBER)
        retryDelayEdit = editRow(root, "재시도 지연(ms)", AppSettings.retryDelayMs(this).toString(), InputType.TYPE_CLASS_NUMBER)
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
        root.addView(button("알림 접근 권한 설정 열기") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        root.addView(button("이 앱 알림 설정 열기") { openOwnNotificationSettings() })
        root.addView(button("배터리 최적화 설정 열기") {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        })
        root.addView(button("ChatGPT 앱 정보 열기") { openChatGptAppInfo() })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            root.addView(button("알림 표시 권한 요청") {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 200)
            })
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
        refreshStatus()
        refreshDiagnostics()
        refreshSettingsSummary()
    }

    private fun refreshStatus() {
        statusContainer.removeAllViews()
        statusContainer.addView(statusLine("ChatGPT 설치됨", LaunchManager.isChatGptInstalled(this)))
        statusContainer.addView(statusLine("알림 접근 권한 허용됨", isNotificationListenerEnabled()))
        statusContainer.addView(statusLine("알림 표시 권한 허용됨", hasPostNotificationPermission()))
        statusContainer.addView(statusLine("배터리 최적화 제외됨", isIgnoringBatteryOptimizations()))
    }

    private fun refreshDiagnostics() {
        val snapshot = AppSettings.diagnosticsSnapshot(this)
        diagnosticsContainer.removeAllViews()
        diagnosticsContainer.addView(body("마지막 ChatGPT 알림 감지 시간: ${snapshot.lastDetected}"))
        diagnosticsContainer.addView(body("마지막 실행 시도 시간: ${snapshot.lastAttempt}"))
        diagnosticsContainer.addView(body("마지막 실행 결과: ${AppSettings.koreanResult(snapshot.lastResult)}"))
        diagnosticsContainer.addView(body("마지막 재시도 시간: ${snapshot.lastRetryAttempt}"))
        diagnosticsContainer.addView(body("마지막 재시도 결과: ${AppSettings.koreanResult(snapshot.lastRetryResult)}"))
        diagnosticsContainer.addView(body("마지막 별도 알림 결과: ${AppSettings.koreanResult(snapshot.lastFallbackResult)}"))
        diagnosticsContainer.addView(body("현재 권한 상태: 알림 접근 ${yesNo(isNotificationListenerEnabled())}, 알림 표시 ${yesNo(hasPostNotificationPermission())}"))
    }

    private fun refreshSettingsSummary() {
        settingsSummaryContainer.removeAllViews()
        settingsSummaryContainer.addView(body(AppSettings.settingsSnapshot(this)))
    }

    private fun saveSettings() {
        val cooldown = cooldownEdit.text.toString().toIntOrNull() ?: AppSettings.DEFAULT_COOLDOWN_SECONDS
        val launchDelay = launchDelayEdit.text.toString().toIntOrNull() ?: AppSettings.DEFAULT_LAUNCH_DELAY_MS
        val retryDelay = retryDelayEdit.text.toString().toIntOrNull() ?: AppSettings.DEFAULT_RETRY_DELAY_MS
        AppSettings.setCooldownSeconds(this, cooldown)
        AppSettings.setLaunchDelayMs(this, launchDelay)
        AppSettings.setRetryDelayMs(this, retryDelay)
        AppSettings.setQuietStart(this, quietStartEdit.text.toString())
        AppSettings.setQuietEnd(this, quietEndEdit.text.toString())
        cooldownEdit.setText(AppSettings.cooldownSeconds(this).toString())
        launchDelayEdit.setText(AppSettings.launchDelayMs(this).toString())
        retryDelayEdit.setText(AppSettings.retryDelayMs(this).toString())
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
        if (!LaunchManager.isChatGptInstalled(this)) {
            Toast.makeText(this, "ChatGPT가 설치되어 있지 않습니다", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${AppSettings.CHATGPT_PACKAGE}")
        })
    }

    private fun yesNo(value: Boolean): String = if (value) "허용됨" else "필요"
}
