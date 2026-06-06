# GPT Alert Launcher

GPT Alert Launcher는 공식 ChatGPT Android 앱(`com.openai.chatgpt`)의 알림을 기기 안에서 감지하고, 가능한 범위에서 ChatGPT 앱을 **무음으로 자동 실행/전환**하려고 시도하는 Android Kotlin 앱입니다. 기본 목적은 별도 알림을 다시 띄우는 것이 아니라, ChatGPT 알림이 도착했을 때 ChatGPT 앱을 조용히 여는 시도를 최대화하는 것입니다.

## Quick APK Download for ChatGPT Agent

1. Open this repository.
2. Go to the Actions tab.
3. Open the latest “Build Debug APK” workflow run.
4. If the run succeeded, download the artifact named “gpt-alert-launcher-debug-apk”.
5. Unzip the artifact.
6. The APK file should be named “app-debug.apk”.
7. Install “app-debug.apk” on the Android device.
8. If the workflow failed, open the failed job log and fix the first meaningful build error.

## 핵심 동작

- `NotificationListenerService`로 알림 콜백을 받습니다.
- 패키지명이 정확히 `com.openai.chatgpt`인 알림만 처리합니다.
- 패키지명 필터를 통과하기 전에는 다른 처리를 하지 않습니다.
- ChatGPT 알림 제목/본문/텍스트를 기본적으로 읽거나 저장하거나 표시하거나 로그로 남기지 않습니다.
- 알림이 감지되면 로컬 조건을 확인합니다.
  - 자동 실행 사용
  - 쿨다운
  - 조용한 시간
  - 화면 켜짐 상태에서만 실행
  - 잠금 해제 상태에서만 실행
- 조건을 통과하면 `packageManager.getLaunchIntentForPackage("com.openai.chatgpt")`로 ChatGPT 실행을 시도합니다.
- 실행 지연과 1회 재시도를 설정할 수 있습니다.
- 실패 시 별도 알림은 선택 기능이며 기본값은 꺼짐입니다.

## 중요한 Android / Samsung One UI 제한

Android와 Samsung One UI는 보안, 개인정보 보호, 배터리 정책 때문에 백그라운드 앱 실행을 차단할 수 있습니다. 이 앱은 루트, ADB, AccessibilityService 없이 안전하고 정책을 준수하는 범위에서 자동 실행 가능성을 높이지만, 자동 전환이 항상 보장되지는 않습니다.

자동 실행이 차단되면 기본적으로 조용히 실패합니다. 사용자가 **실패 시 별도 알림 표시**를 켠 경우에만 ChatGPT를 직접 열 수 있는 별도 알림을 표시합니다.

## 기본 설정

- 자동 실행 사용: 켜짐
- 무음 자동 실행 모드: 켜짐
- 실패 시 별도 알림 표시: 꺼짐
- 쿨다운(초): 10초, 1..3600 범위로 제한
- 화면 켜짐 상태에서만 실행: 켜짐
- 잠금 해제 상태에서만 실행: 켜짐
- 실행 지연(ms): 300ms, 0..5000 범위로 제한
- 재시도 사용: 켜짐
- 재시도 지연(ms): 700ms, 100..5000 범위로 제한
- 조용한 시간: 시작/종료 시간이 자정을 넘는 범위도 처리

## 개인정보 보호

> 이 앱은 ChatGPT 앱의 알림을 감지하기 위해 알림 접근 권한을 요청합니다. 알림은 기기 안에서만 패키지명으로 필터링하며, 알림 내용은 외부로 전송하지 않습니다.

모든 처리는 기기 안에서만 수행됩니다. 이 앱은 `INTERNET` 권한을 요청하지 않으며, 분석/텔레메트리/광고/외부 서비스를 포함하지 않습니다. 진단 정보에도 ChatGPT 알림 제목, 본문, 텍스트는 포함되지 않습니다.

## 진단 정보

앱 화면의 **진단 정보** 섹션은 로컬에만 저장되는 내용 없는 상태값을 보여 줍니다.

- 마지막 ChatGPT 알림 감지 시간
- 마지막 실행 시도 시간
- 마지막 실행 결과
  - 시도 안 함
  - 시도함
  - 실패: ChatGPT 미설치
  - 실패: 예외 클래스명만 표시
  - 건너뜀: 자동 실행 꺼짐
  - 건너뜀: 쿨다운
  - 건너뜀: 조용한 시간
  - 건너뜀: 화면 꺼짐
  - 건너뜀: 기기 잠김
- 마지막 재시도 시간과 결과
- 마지막 별도 알림 결과
  - 꺼짐
  - 표시됨
  - 건너뜀: 알림 권한 없음
- 현재 권한 상태
- 현재 설정 요약

진단 정보는 Samsung Galaxy Z Flip5 / One UI에서 실제 자동 전환이 차단되는지 확인하기 위한 로컬 확인 도구입니다.

## Samsung Galaxy Z Flip5 권장 설정

1. APK를 설치합니다.
2. GPT Alert Launcher의 알림 접근 권한을 허용합니다.
3. 별도 알림 기능을 사용할 경우 Android 13+ 알림 표시 권한도 허용합니다.
4. 배터리 사용량을 제한 없음으로 설정하거나 배터리 최적화에서 제외합니다.
5. Samsung 설정에서 가능한 경우 절전 예외 또는 절전 안 함 앱에 추가합니다.
6. 공식 ChatGPT 앱의 알림이 켜져 있는지 확인합니다.
7. 화면이 켜져 있고 기기가 잠금 해제된 상태에서 다른 앱을 사용 중일 때 ChatGPT 알림을 받아 테스트합니다.
8. 자동 전환이 되지 않으면 앱의 진단 정보에서 건너뜀/실패 사유를 확인합니다.

## 가장 좋은 테스트 조건

- 화면 켜짐
- 기기 잠금 해제됨
- GPT Alert Launcher가 아닌 다른 앱 사용 중
- 공식 ChatGPT 앱에서 실제 알림 수신
- 쿨다운 시간이 지나 있음
- 현재 시간이 조용한 시간 범위 밖임

이 조건에서도 Android/Samsung One UI 정책에 따라 자동 실행이 차단될 수 있습니다.

## 앱 화면

앱 화면은 Galaxy Z Flip5 세로 화면과 한 손 사용을 고려해 큰 버튼과 짧은 섹션 중심으로 구성되어 있습니다.

- 상태 요약
- 자동 실행 설정
- 자동 실행 세부 설정
- 테스트
- 권한/설정 바로가기
- 진단 정보
- 현재 설정 요약

## 사용하지 않는 것

GPT Alert Launcher는 다음을 사용하지 않습니다.

- MacroDroid
- root
- ADB 필수 설정
- `INTERNET` 권한
- AccessibilityService v1
- 분석, 텔레메트리, 크래시 리포팅, 광고, 외부 서비스
- 알림 내용 업로드 또는 전송

## 필요한 권한

| 권한 / 접근 | 이유 |
| --- | --- |
| 알림 접근 권한 | ChatGPT 앱 알림 콜백을 받고 패키지명으로 필터링하기 위해 필요합니다. |
| `POST_NOTIFICATIONS` on Android 13+ | 사용자가 선택적으로 켠 실패 시 별도 알림을 표시하기 위해 필요합니다. |

이 앱은 `INTERNET` 권한을 요청하지 않습니다.

## 빌드 방법

### 로컬 빌드

필요한 도구:

- JDK 17
- Android SDK platform 35
- Gradle 8.10.2 또는 호환 버전

실행:

```bash
gradle assembleDebug --no-daemon
```

디버그 APK 위치:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### GitHub Actions 빌드

`.github/workflows/build-debug-apk.yml`의 **Build Debug APK** 워크플로는 push와 `workflow_dispatch`에서 실행됩니다. GitHub Actions에서 JDK 17과 Gradle 8.10.2를 사용해 다음 명령을 실행합니다.

```bash
gradle assembleDebug --no-daemon
```

성공하면 artifact 이름은 `gpt-alert-launcher-debug-apk`이고, 포함되는 APK 경로는 `app/build/outputs/apk/debug/app-debug.apk`입니다.

## GitHub Actions APK artifact 다운로드

1. GitHub 저장소에서 **Actions** 탭을 엽니다.
2. 최신 **Build Debug APK** 워크플로 실행을 엽니다.
3. 실행이 성공했는지 확인합니다.
4. **Artifacts** 영역에서 `gpt-alert-launcher-debug-apk`를 다운로드합니다.
5. ZIP 파일을 풀고 `app-debug.apk`를 설치합니다.

## 문제 해결

### 자동 실행이 되지 않음

Android/Samsung One UI가 백그라운드 앱 실행을 차단했을 수 있습니다. 화면 켜짐, 잠금 해제, 다른 앱 사용 중, 조용한 시간 밖, 쿨다운 종료 상태에서 다시 테스트하고 진단 정보를 확인하세요.

### 별도 알림이 보이지 않음

별도 알림은 기본값이 꺼짐입니다. 앱에서 **실패 시 별도 알림 표시**를 켜고, Android 13+에서는 알림 표시 권한을 허용하세요.

### 알림이 감지되지 않음

앱의 **알림 접근 권한 설정 열기** 버튼을 눌러 GPT Alert Launcher의 알림 접근 권한을 허용하세요. 공식 ChatGPT 앱 알림이 켜져 있고 패키지명이 `com.openai.chatgpt`인지 확인하세요.

### 반복 실행이 억제됨

쿨다운 기본값은 10초입니다. 앱의 **쿨다운(초)** 값을 조정하고 **설정 저장**을 누르세요.
