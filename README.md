# GPT Alert Launcher

GPT Alert Launcher is a standalone Android Kotlin app for Samsung Galaxy Z Flip5 / Samsung One UI devices. It listens locally for notifications from the official ChatGPT Android app and silently tries to open or bring ChatGPT to the foreground.

Automatic app switching is **not guaranteed**. Android and Samsung One UI can block background foreground launches. This app maximizes safe local launch attempts and provides local diagnostics so you can verify whether a ChatGPT notification was detected and whether a launch was attempted.

## Quick APK Download for ChatGPT Agent

1. Open this repository.
2. Go to the Actions tab.
3. Open the latest “Build Debug APK” workflow run.
4. If the run succeeded, download the artifact named “gpt-alert-launcher-debug-apk”.
5. Unzip the artifact.
6. The APK file should be named “app-debug.apk”.
7. Install “app-debug.apk” on the Android device.
8. If the workflow failed, open the failed job log and fix the first meaningful build error.

## Purpose

The primary purpose is silent automatic ChatGPT launch attempts when notifications arrive from exactly:

```text
com.openai.chatgpt
```

The app does not read, store, log, display, upload, or transmit ChatGPT notification title/body/text by default. The notification listener filters by package name before any other handling.

## Android / Samsung background-launch limitation

Modern Android and Samsung One UI often restrict apps from launching another app into the foreground from the background. Best-effort launch attempts may be ignored by the system even when diagnostics show that the ChatGPT notification was detected and the launch was attempted.

Best conditions for auto-launch success:

- Screen on.
- Device unlocked.
- You are using another app.
- GPT Alert Launcher has notification access.
- GPT Alert Launcher battery usage is unrestricted / not sleeping.
- ChatGPT is installed and ChatGPT notifications are enabled.

## Behavior

- Uses `NotificationListenerService`.
- Ignores every notification package except exactly `com.openai.chatgpt`.
- Applies local guards before launch: automation enabled, cooldown, quiet hours, screen-on-only, and unlocked-only.
- Uses Android package launch intent with `FLAG_ACTIVITY_NEW_TASK` and `FLAG_ACTIVITY_RESET_TASK_IF_NEEDED`.
- Supports configurable launch delay, default 300 ms.
- Supports one retry, default on, with retry delay default 700 ms.
- Uses cooldown default 10 seconds and clamps cooldown to 1..3600 seconds.
- Quiet hours support ranges that cross midnight.
- Handles missing ChatGPT install without crashing.
- Keeps fallback notification code available, but fallback notification is optional and **off by default**.

## Settings and diagnostics

The app screen shows:

- Automation enabled / disabled.
- Silent auto-launch mode, default on.
- Fallback notification enabled / disabled, default off.
- ChatGPT installed status.
- Notification access permission status.
- Post notification permission status.
- Battery optimization status.
- Cooldown seconds.
- Launch delay milliseconds.
- Retry enabled and retry delay milliseconds.
- Only run when screen is on.
- Only run when device is unlocked.
- Quiet hours start/end.

Local diagnostics show no notification contents. They include:

- Last ChatGPT notification detected time.
- Last launch attempt time.
- Last launch result.
- Last retry attempt time.
- Last retry result.
- Last fallback notification result.
- Current permission status.
- Current settings snapshot.

Diagnostic results can include `not attempted`, `attempted`, `failed: ChatGPT not installed`, `failed: <exception class only>`, `skipped: automation disabled`, `skipped: cooldown`, `skipped: quiet hours`, `skipped: screen off`, and `skipped: device locked`.

## Buttons

- Save local settings.
- Clear diagnostics.
- Test silent auto-launch.
- Test retry launch.
- Test fallback notification.
- Open Android notification listener access settings.
- Open this app's notification settings.
- Open battery optimization settings.
- Open ChatGPT app info.

The test buttons do not read notification contents. **Test silent auto-launch** attempts to open ChatGPT immediately without showing a GPT Alert Launcher notification. **Test retry launch** performs the same launch attempt and schedules the configured retry. **Test fallback notification** explicitly shows the fallback notification for manual verification.

## Required permissions

| Permission / access | Why it is needed |
| --- | --- |
| Notification access | Lets the app receive notification callbacks and filter locally for `com.openai.chatgpt`. |
| `POST_NOTIFICATIONS` on Android 13+ | Only needed for optional fallback notification testing/use. Silent auto-launch diagnostics work without this notification permission. |

The app does **not** request `INTERNET`.

## Non-goals and constraints

GPT Alert Launcher does **not**:

- Use MacroDroid.
- Require root.
- Require ADB.
- Request the `INTERNET` permission.
- Add analytics, telemetry, crash reporting, ads, or external services.
- Upload or transmit notification contents.
- Read, store, log, or display ChatGPT notification text by default.
- Implement an `AccessibilityService` in v1.
- Perform financial, password, OTP, or payment-screen automation.

## Privacy notice

> This app requests notification access so it can detect notifications from the ChatGPT app. It filters locally by package name and does not upload notification contents anywhere.

All processing is local on the device. Local storage is limited to app settings and diagnostics such as timestamps, skipped/attempted status, exception class names, and fallback notification status. Diagnostics do not include notification title, body, or text.

## Samsung Galaxy Z Flip5 / One UI setup and real-device test checklist

1. Install APK.
2. Grant notification access.
3. Grant notification permission only if fallback testing is needed.
4. Set battery usage to unrestricted.
5. Add GPT Alert Launcher to Never sleeping apps if available.
6. Make sure ChatGPT notifications are enabled.
7. Best test condition:
   - screen on
   - device unlocked
   - using another app
   - receive a ChatGPT notification
8. Reopen GPT Alert Launcher and check diagnostics:
   - Last ChatGPT notification detected time
   - Last launch attempt time
   - Last launch result
9. If notification is detected and launch was attempted but ChatGPT did not open, Samsung/Android likely blocked background foreground launch.

## Build instructions

### Local build

Requirements:

- JDK 17.
- Android SDK with platform 35 installed.
- Gradle 8.10.2 or compatible for local builds. GitHub Actions installs and uses Gradle 8.10.2 automatically.

Run:

```bash
gradle assembleDebug --no-daemon
```

The debug APK will be created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### GitHub Actions build

The **Build Debug APK** workflow at `.github/workflows/build-debug-apk.yml` builds the debug APK on every push and on manual `workflow_dispatch` runs. It installs/uses Gradle 8.10.2 in GitHub Actions and runs `gradle assembleDebug --no-daemon`.

## Install instructions

1. Build or download the debug APK.
2. Transfer it to the Galaxy Z Flip5.
3. Open the APK and allow installation from the chosen source if prompted.
4. Open GPT Alert Launcher and follow the setup buttons.

## How to download the APK artifact from GitHub Actions

1. Open the repository on GitHub.
2. Select the **Actions** tab.
3. Open the latest **Build Debug APK** workflow run.
4. Scroll to **Artifacts**.
5. Download `gpt-alert-launcher-debug-apk`.
6. Extract the ZIP to find `app-debug.apk`.

## Troubleshooting

### Notification detected, launch attempted, but ChatGPT did not open

This is expected on many modern Android / One UI builds. Background foreground launch restrictions likely blocked the automatic switch. Keep the screen on and device unlocked for the best chance of success.

### No diagnostics update after a ChatGPT notification

- Tap **Open notification listener access settings**.
- Enable notification access for GPT Alert Launcher.
- Confirm ChatGPT notifications are enabled in ChatGPT and Android app settings.
- Confirm the notification comes from the official package `com.openai.chatgpt`.

### Repeated alerts are suppressed

The cooldown defaults to 10 seconds and is clamped to 1..3600 seconds. Adjust **Cooldown seconds** in settings.

### Retry did not open ChatGPT

The retry is another safe launch intent attempt after the configured delay. Android/Samsung may block both the first attempt and retry.

### No fallback notification appears

Fallback notifications are off by default. Enable **Show fallback notification if auto-launch fails**, grant Android 13+ notification permission if needed, and use **Test fallback notification**.

### Battery restrictions stop detection or launch attempts

Samsung One UI may stop background services aggressively. Set GPT Alert Launcher battery usage to unrestricted, exclude it from battery optimization, and add it to Never sleeping apps if available.
