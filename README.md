# GPT Alert Launcher

GPT Alert Launcher is a standalone Android Kotlin app for Samsung Galaxy Z Flip5 / Samsung One UI devices. It listens locally for notifications from the official ChatGPT Android app and tries to open ChatGPT automatically.

If Android or One UI blocks background launching, or if launching is not possible, GPT Alert Launcher shows a high-priority fallback notification with a clear **Open ChatGPT** action.

## Quick APK Download for ChatGPT Agent

1. Open this repository.
2. Go to the Actions tab.
3. Open the latest “Build Debug APK” workflow run.
4. If the run succeeded, download the artifact named “gpt-alert-launcher-debug-apk”.
5. Unzip the artifact.
6. The APK file should be named “app-debug.apk”.
7. Install “app-debug.apk” on the Android device.
8. If the workflow failed, open the failed job log and fix the first meaningful build error.

## Important Android limitation

Android background app launches are intentionally restricted for privacy, security, and battery reasons. This app uses the package launch intent for `com.openai.chatgpt`, but automatic launch is **not guaranteed**. The fallback notification is the reliable path when Android blocks a direct launch.

This project deliberately prefers safe fallback notification behavior over forced UI control.

## What it does

- Uses `NotificationListenerService`.
- Ignores every notification package except exactly `com.openai.chatgpt`.
- Does not read, display, or store ChatGPT notification text by default.
- Stores only local app settings in `SharedPreferences`.
- Tries to open ChatGPT with Android's package launch intent.
- Uses a configurable cooldown, defaulting to 10 seconds, to avoid repeated launches.
- Shows a high-priority local fallback notification with a `PendingIntent` to open ChatGPT.
- Provides a simple settings screen for permissions, battery status, cooldown, quiet hours, and testing.

## Non-goals and constraints

GPT Alert Launcher does **not**:

- Use MacroDroid.
- Require root.
- Require ADB.
- Request the `INTERNET` permission.
- Add analytics, telemetry, crash reporting, ads, or external services.
- Upload or transmit notification contents.
- Implement an `AccessibilityService` in v1.
- Perform financial, password, OTP, or payment-screen automation.

## Required permissions

| Permission / access | Why it is needed |
| --- | --- |
| Notification access | Lets the app receive notification callbacks and filter for `com.openai.chatgpt`. |
| `POST_NOTIFICATIONS` on Android 13+ | Lets the app show the high-priority fallback notification. |

The app does **not** request `INTERNET`.

## Settings screen

The app includes controls and status for:

- Automation enabled / disabled.
- ChatGPT installed status.
- Notification access permission status.
- Post notification permission status.
- Battery optimization status.
- Cooldown seconds.
- Only run when screen is on.
- Only run when device is unlocked.
- Quiet hours start/end.

It also includes buttons to:

- Open Android notification listener access settings.
- Open this app's notification settings.
- Open battery optimization settings.
- Open ChatGPT app info if installed.
- Test the fallback notification.
- Test launching ChatGPT.

## Privacy notice

> This app requests notification access so it can detect notifications from the ChatGPT app. It filters locally by package name and does not upload notification contents anywhere.

All processing is local on the device. The listener immediately ignores packages other than `com.openai.chatgpt`. Local storage is limited to app settings such as enabled state, cooldown, quiet hours, screen-on/unlocked preferences, and last launch timestamp.

## Samsung One UI setup for Galaxy Z Flip5

1. Install the APK.
2. Allow notification access for GPT Alert Launcher.
3. Allow notifications for GPT Alert Launcher.
4. Set battery usage to unrestricted or exclude the app from battery optimization.
5. Add GPT Alert Launcher to Never sleeping apps if available.
6. Make sure ChatGPT notifications are enabled.
7. Send or receive a ChatGPT notification to test.
8. If automatic launch is blocked by Android, use the high-priority fallback notification.

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

## How to find the APK artifact in GitHub Actions

1. Open the repository on GitHub.
2. Select the **Actions** tab.
3. Open the latest **Build Debug APK** workflow run.
4. Scroll to **Artifacts**.
5. Download `gpt-alert-launcher-debug-apk`.
6. Extract the ZIP to find `app-debug.apk`.

## Troubleshooting

### Automatic launch does not happen

This is expected on many modern Android / One UI builds. Background app launch restrictions may block the direct launch. Use the fallback notification's **Open ChatGPT** action.

### No fallback notification appears

- Open the app and confirm **Post notification permission granted** is shown.
- Tap **Open this app's notification settings** and allow notifications.
- Confirm the fallback notification channel is not muted.

### Notifications are not detected

- Tap **Open notification listener access settings**.
- Enable notification access for GPT Alert Launcher.
- Confirm ChatGPT notifications are enabled in ChatGPT and in Android app settings.
- Confirm the notification comes from the official package `com.openai.chatgpt`.

### Repeated alerts are suppressed

The cooldown defaults to 10 seconds. Increase or decrease **Cooldown seconds** in the app settings.

### Battery restrictions stop detection

Samsung One UI may stop background services aggressively. Set GPT Alert Launcher battery usage to unrestricted, exclude it from battery optimization, and add it to Never sleeping apps if available.
