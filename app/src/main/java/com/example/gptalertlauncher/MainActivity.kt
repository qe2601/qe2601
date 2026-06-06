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
import java.text.DateFormat
import java.util.Date

class MainActivity : Activity() {
    private lateinit var statusContainer: LinearLayout
    private lateinit var diagnosticsContainer: LinearLayout
    private lateinit var settingsSnapshotContainer: LinearLayout
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
        refreshPanels()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(24))
        }
        val scrollView = ScrollView(this).apply { addView(root) }

        root.addView(title("GPT Alert Launcher"))
        root.addView(body("ChatGPT 알림이 오면 가능한 범위에서 ChatGPT 앱을 자동으로 열어 봅니다. Android/Samsung One UI 정책 때문에 자동 전환이 항상 보장되지는 않습니다."))
        root.addView(body("이 앱은 ChatGPT 앱의 알림을 감지하기 위해 알림 접근 권한을 요청합니다. 알림은 기기 안에서만 패키지명으로 필터링하며, 알림 내용은 외부로 전송하지 않습니다."))

        statusContainer = cardContainer()
        root.addView(section("상태 요약"))
        root.addView(statusContainer)

        root.addView(section("자동 실행 설정"))
        root.addView(cardContainer().apply {
            addView(switchRow("자동 실행 사용", AppSettings.isEnabled(this@MainActivity)) { _, checked -> AppSettings.setEnabled(this@MainActivity, checked); refreshPanels() })
            addView(switchRow("무음 자동 실행 모드", AppSettings.silentMode(this@MainActivity)) { _, checked -> AppSettings.setSilentMode(this@MainActivity, checked); refreshPanels() })
            addView(switchRow("실패 시 별도 알림 표시", AppSettings.fallbackEnabled(this@MainActivity)) { _, checked -> AppSettings.setFallbackEnabled(this@MainActivity, checked); refreshPanels() })
            addView(switchRow("화면 켜짐 상태에서만 실행", AppSettings.screenOnOnly(this@MainActivity)) { _, checked -> AppSettings.setScreenOnOnly(this@MainActivity, checked); refreshPanels() })
            addView(switchRow("잠금 해제 상태에서만 실행", AppSettings.unlockedOnly(this@MainActivity)) { _, checked -> AppSettings.setUnlockedOnly(this@MainActivity, checked); refreshPanels() })
        })

        root.addView(section("자동 실행 세부 설정"))
        root.addView(cardContainer().apply {
            addView(switchRow("재시도 사용", AppSettings.retryEnabled(this@MainActivity)) { _, checked -> AppSettings.setRetryEnabled(this@MainActivity, checked); refreshPanels() })
            cooldownEdit = compactEditRow(this, "쿨다운(초)", AppSettings.cooldownSeconds(this@MainActivity).toString(), InputType.TYPE_CLASS_NUMBER)
            launchDelayEdit = compactEditRow(this, "실행 지연(ms)", AppSettings.launchDelayMs(this@MainActivity).toString(), InputType.TYPE_CLASS_NUMBER)
            retryDelayEdit = compactEditRow(this, "재시도 지연(ms)", AppSettings.retryDelayMs(this@MainActivity).toString(), InputType.TYPE_CLASS_NUMBER)
            quietStartEdit = compactEditRow(this, "조용한 시간 시작", AppSettings.quietStart(this@MainActivity), InputType.TYPE_CLASS_DATETIME)
            quietEndEdit = compactEditRow(this, "조용한 시간 종료", AppSettings.quietEnd(this@MainActivity), InputType.TYPE_CLASS_DATETIME)
        })

        root.addView(section("테스트 버튼"))
        root.addView(buttonGrid(
            button("설정 저장") { saveSettings() },
            button("무음 자동 실행 테스트") {
                LaunchManager.testSilentLaunch(this)
                refreshPanels()
            },
            button("재시도 실행 테스트") {
                LaunchManager.testLaunchWithRetry(this)
                refreshPanels()
                Toast.makeText(this, "설정된 지연 시간으로 재시도를 예약했습니다", Toast.LENGTH_SHORT).show()
            },
            button("별도 알림 테스트") {
                ChatGptLauncher.showFallbackNotification(this, "별도 알림 수동 테스트입니다.")
                refreshPanels()
            },
            button("진단 기록 지우기") {
                AppSettings.clearDiagnostics(this)
                refreshPanels()
            }
        ))

        root.addView(section("권한/설정 바로가기"))
        val permissionButtons = mutableListOf(
            button("알림 접근 권한 설정 열기") { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            button("이 앱 알림 설정 열기") { openOwnNotificationSettings() },
            button("배터리 최적화 설정 열기") { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
            button("ChatGPT 앱 정보 열기") { openChatGptAppInfo() }
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionButtons.add(button("앱 알림 권한 요청") { requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 200) })
        }
        root.addView(buttonGrid(*permissionButtons.toTypedArray()))

        diagnosticsContainer = cardContainer()
        root.addView(section("진단 정보"))
        root.addView(body("자동 전환이 되지 않을 때, 아래 진단 정보로 알림 감지 여부와 실행 시도 여부를 확인할 수 있습니다. 알림 내용은 표시하거나 저장하지 않습니다."))
        root.addView(diagnosticsContainer)

        settingsSnapshotContainer = cardContainer()
        root.addView(section("현재 설정 요약"))
        root.addView(settingsSnapshotContainer)

        setContentView(scrollView)
    }

    private fun refreshPanels() {
        refreshStatus()
        refreshDiagnostics()
        refreshSettingsSnapshot()
    }

    private fun refreshStatus() {
        statusContainer.removeAllViews()
        statusContainer.addView(statusRow("ChatGPT", if (ChatGptLauncher.isChatGptInstalled(this)) "설치됨" else "설치 안 됨", ChatGptLauncher.isChatGptInstalled(this)))
        statusContainer.addView(statusRow("알림 접근 권한", if (isNotificationListenerEnabled()) "허용됨" else "필요함", isNotificationListenerEnabled()))
        statusContainer.addView(statusRow("앱 알림 권한", if (hasPostNotificationPermission()) "허용됨" else "필요함", hasPostNotificationPermission()))
        statusContainer.addView(statusRow("배터리 최적화", if (isIgnoringBatteryOptimizations()) "제외됨" else "필요함", isIgnoringBatteryOptimizations()))
    }

    private fun refreshDiagnostics() {
        diagnosticsContainer.removeAllViews()
        diagnosticsContainer.addView(infoRow("마지막 ChatGPT 알림 감지", formatTime(AppSettings.lastDetectedMs(this))))
        diagnosticsContainer.addView(infoRow("마지막 실행 시도", formatTime(AppSettings.lastAttemptMs(this))))
        diagnosticsContainer.addView(infoRow("마지막 실행 결과", AppSettings.lastResult(this)))
        diagnosticsContainer.addView(infoRow("마지막 재시도 시각", formatTime(AppSettings.lastRetryMs(this))))
        diagnosticsContainer.addView(infoRow("마지막 재시도 결과", AppSettings.lastRetryResult(this)))
        diagnosticsContainer.addView(infoRow("마지막 별도 알림 결과", AppSettings.lastFallbackResult(this)))
    }

    private fun refreshSettingsSnapshot() {
        settingsSnapshotContainer.removeAllViews()
        settingsSnapshotContainer.addView(infoRow("자동 실행 사용", onOff(AppSettings.isEnabled(this))))
        settingsSnapshotContainer.addView(infoRow("무음 자동 실행 모드", onOff(AppSettings.silentMode(this))))
        settingsSnapshotContainer.addView(infoRow("실패 시 별도 알림 표시", onOff(AppSettings.fallbackEnabled(this))))
        settingsSnapshotContainer.addView(infoRow("쿨다운(초)", AppSettings.cooldownSeconds(this).toString()))
        settingsSnapshotContainer.addView(infoRow("실행 지연(ms)", AppSettings.launchDelayMs(this).toString()))
        settingsSnapshotContainer.addView(infoRow("재시도 사용", onOff(AppSettings.retryEnabled(this))))
        settingsSnapshotContainer.addView(infoRow("재시도 지연(ms)", AppSettings.retryDelayMs(this).toString()))
        settingsSnapshotContainer.addView(infoRow("화면 켜짐 상태에서만 실행", onOff(AppSettings.screenOnOnly(this))))
        settingsSnapshotContainer.addView(infoRow("잠금 해제 상태에서만 실행", onOff(AppSettings.unlockedOnly(this))))
        settingsSnapshotContainer.addView(infoRow("조용한 시간", "${AppSettings.quietStart(this)} ~ ${AppSettings.quietEnd(this)}"))
    }

    private fun saveSettings() {
        AppSettings.setCooldownSeconds(this, cooldownEdit.text.toString().toIntOrNull() ?: AppSettings.DEFAULT_COOLDOWN_SECONDS)
        AppSettings.setLaunchDelayMs(this, launchDelayEdit.text.toString().toIntOrNull() ?: AppSettings.DEFAULT_LAUNCH_DELAY_MS)
        AppSettings.setRetryDelayMs(this, retryDelayEdit.text.toString().toIntOrNull() ?: AppSettings.DEFAULT_RETRY_DELAY_MS)
        AppSettings.setQuietStart(this, quietStartEdit.text.toString())
        AppSettings.setQuietEnd(this, quietEndEdit.text.toString())
        cooldownEdit.setText(AppSettings.cooldownSeconds(this).toString())
        launchDelayEdit.setText(AppSettings.launchDelayMs(this).toString())
        retryDelayEdit.setText(AppSettings.retryDelayMs(this).toString())
        quietStartEdit.setText(AppSettings.quietStart(this))
        quietEndEdit.setText(AppSettings.quietEnd(this))
        Toast.makeText(this, "설정을 저장했습니다", Toast.LENGTH_SHORT).show()
        refreshPanels()
    }

    private fun title(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 28f
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, 0, 0, dp(12))
    }

    private fun section(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 20f
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(18), 0, dp(8))
    }

    private fun body(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 15f
        setLineSpacing(0f, 1.08f)
        setPadding(0, 0, 0, dp(8))
    }

    private fun cardContainer(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(10), dp(12), dp(10))
    }

    private fun statusRow(label: String, value: String, ok: Boolean): TextView {
        val state = if (ok) "정상" else "필요"
        return body("$label: $value · $state")
    }

    private fun infoRow(label: String, value: String): TextView = body("$label: $value")

    private fun switchRow(label: String, checked: Boolean, listener: CompoundButton.OnCheckedChangeListener): View {
        return Switch(this).apply {
            text = label
            isChecked = checked
            textSize = 16f
            minHeight = dp(52)
            setPadding(0, dp(6), 0, dp(6))
            setOnCheckedChangeListener(listener)
        }
    }

    private fun compactEditRow(root: LinearLayout, label: String, value: String, inputType: Int): EditText {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            minimumHeight = dp(56)
            setPadding(0, dp(4), 0, dp(4))
        }
        row.addView(TextView(this).apply {
            text = label
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
        })
        val editText = EditText(this).apply {
            setText(value)
            this.inputType = inputType
            textSize = 16f
            setSingleLine(true)
            selectAllOnFocus = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f)
        }
        row.addView(editText)
        root.addView(row)
        return editText
    }

    private fun button(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 15f
            minHeight = dp(56)
            setPadding(dp(6), dp(8), dp(6), dp(8))
            setOnClickListener { onClick() }
        }
    }

    private fun buttonGrid(vararg buttons: Button): LinearLayout {
        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(2), 0, dp(2))
        }
        buttons.toList().chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEach { button ->
                row.addView(button.apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(dp(3), dp(3), dp(3), dp(3))
                    }
                })
            }
            if (pair.size == 1) {
                row.addView(TextView(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
            }
            grid.addView(row)
        }
        return grid
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
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply { putExtra(Settings.EXTRA_APP_PACKAGE, packageName) }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:$packageName") }
        }
        startActivity(intent)
    }

    private fun openChatGptAppInfo() {
        if (!ChatGptLauncher.isChatGptInstalled(this)) {
            Toast.makeText(this, "ChatGPT가 설치되어 있지 않습니다", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:${AppSettings.CHATGPT_PACKAGE}") })
    }

    private fun formatTime(value: Long): String {
        if (value <= 0L) return "아직 없음"
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(value))
    }

    private fun onOff(value: Boolean): String = if (value) "켜짐" else "꺼짐"

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
