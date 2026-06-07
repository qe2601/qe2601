# GPT Alert Launcher v2

GPT Alert Launcher v2는 공식 ChatGPT Android 앱(`com.openai.chatgpt`)의 알림이 도착했을 때 가능한 범위에서 ChatGPT 앱을 조용히 자동 실행/전면 전환하도록 시도하는 로컬 전용 Android 앱입니다. 기존 NotificationListenerService-only 방식은 ChatGPT 알림 감지와 실행 시도/재시도 기록까지는 성공했지만, Samsung One UI / Android 정책으로 백그라운드 전면 전환이 차단될 수 있습니다. v2는 선택 사항인 접근성 보조 모드를 추가해 자동 전환 가능성을 높입니다.

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

1. `NotificationListenerService`가 알림을 받으면 가장 먼저 패키지명을 `com.openai.chatgpt`와 정확히 비교합니다.
2. ChatGPT 알림이 아니면 즉시 반환하고, 알림 제목/본문/텍스트를 읽거나 저장하지 않습니다.
3. 자동 실행 사용, 쿨다운, 조용한 시간, 화면 켜짐 상태, 잠금 해제 상태 가드를 로컬에서 확인합니다.
4. 알림으로 인한 자동 전환은 반드시 `LaunchManager.schedulePreSwitchThenLaunch(...)` 단일 경로를 통과합니다. 이 경로에서 전환 예고가 끝난 뒤에만 기존 ChatGPT 실행 flow를 시작합니다.
5. `LaunchManager`가 `packageManager.getLaunchIntentForPackage("com.openai.chatgpt")`로 실행 intent를 만들고 `FLAG_ACTIVITY_NEW_TASK`, `FLAG_ACTIVITY_RESET_TASK_IF_NEEDED`를 붙여 실행합니다.
6. 기본 전환 예고 방식은 **노티바 카운트다운**입니다. 예고 알림은 `ChatGPT 자동 전환` 제목과 전환까지 남은 숫자(`3`, `2`, `1`, `0.5` 등)만 표시하며, 원본 ChatGPT 알림 제목/본문/텍스트는 읽거나 표시하지 않습니다. 즉시 전환 설정에서는 예고 알림을 표시하지 않습니다.
7. 기본 1초 전환 예고가 완료된 뒤 기본 300ms 실행 지연을 적용하고, 설정이 켜져 있으면 기본 700ms 뒤 한 번 재시도합니다. 실행 시도가 시작되면 예고 알림은 자동으로 취소됩니다.
8. 접근성 보조 모드가 켜져 있고 Android 설정에서도 사용자가 직접 접근성 서비스를 허용한 경우, 전환 예고가 완료된 뒤 만들어진 대기 요청 안에서만 ChatGPT 전면 감지 또는 추가 실행 intent 시도를 보조합니다.
9. 실패 시 별도 알림은 선택 기능이며 기본값은 꺼짐입니다.

자동 전환은 Android/Samsung One UI 정책 때문에 여전히 보장되지 않습니다.

## 접근성 보조 모드

> 접근성 보조 모드는 자동 전환 성공률을 높이기 위한 선택 기능입니다. 이 앱은 화면 텍스트를 수집하거나 외부로 전송하지 않으며, 금융/결제/비밀번호/OTP 화면 자동조작을 하지 않습니다.

접근성 보조 모드는 기본값이 꺼짐입니다. 사용자가 앱 설정에서 **접근성 보조 모드**를 켜고, Android 접근성 설정에서 **GPT 알림 접근성 보조** 서비스를 직접 허용해야 동작합니다.

안전 경계:

- 화면 텍스트를 읽거나 저장하거나 로그로 남기지 않습니다.
- 알림 내용이나 앱 내용을 업로드/전송하지 않습니다.
- `INTERNET` 권한이 없습니다.
- 금융, 결제, 비밀번호, OTP, 보안 관련 앱으로 보이는 패키지에서는 보조 동작을 건너뜁니다.
- 임의 UI 요소 클릭, 랜덤 좌표 탭, 하드코딩 좌표 탭을 하지 않습니다.
- 잠금 화면 우회를 시도하지 않습니다.
- `canRetrieveWindowContent="false"`로 구성되어 화면 콘텐츠 조회 권한을 요청하지 않습니다.

접근성 보조 서비스는 제한된 창 상태 변경 이벤트와 패키지명 메타데이터만 사용해 현재 전면 앱이 ChatGPT인지 확인하고, 전환 예고가 완료된 뒤 만들어진 대기 중 ChatGPT 실행 요청이 있을 때만 안전한 범위에서 실행 intent를 다시 시도합니다.

## 접근성 제한 해제 안내

Samsung/Android에서 접근성 보조가 제한되어 있으면, 먼저 이 앱의 애플리케이션 정보 화면에서 제한된 설정 허용을 켠 뒤 접근성 보조를 다시 켜야 합니다. 앱 안의 **접근성 제한 해제 안내** 섹션에서 다음 버튼을 제공합니다.

1. **이 앱 애플리케이션 정보 열기**: 현재 앱의 Android 애플리케이션 정보 화면을 엽니다.
2. **접근성 보조 설정 열기**: Android 접근성 설정 화면을 엽니다.
3. **접근성 보조 상태 다시 확인**: 사용자가 설정을 바꾼 뒤 접근성 보조 권한 상태를 다시 확인합니다.

앱은 접근성 서비스를 프로그래밍 방식으로 켜지 않으며, 시스템 설정 화면을 속이거나 오버레이하지 않습니다.

## 기본 설정

- 자동 실행 사용: 켜짐
- 무음 자동 실행 모드: 켜짐
- 접근성 보조 모드: 꺼짐
- 전환 예고 방식: 노티바 카운트다운
- 전환 예고 시간: 1초
- 예고 알림 표시: 전환까지 남은 숫자만 표시
- 실패 시 별도 알림 표시: 꺼짐
- 쿨다운: 10초, 휠 선택 UI 사용
- 화면 켜짐 상태에서만 실행: 켜짐
- 잠금 해제 상태에서만 실행: 켜짐
- 실행 지연: 0.3초, 휠 선택 UI 사용
- 재시도 사용: 켜짐
- 재시도 지연: 0.7초, 휠 선택 UI 사용
- 접근성 보조 제한 시간: 5초, 휠 선택 UI 사용
- 조용한 시간: 시작/종료 시간이 자정을 넘는 범위도 처리

## 개인정보 보호

> 이 앱은 ChatGPT 앱의 알림을 감지하기 위해 알림 접근 권한을 요청합니다. 알림은 기기 안에서만 패키지명으로 필터링하며, 알림 내용은 외부로 전송하지 않습니다.

모든 처리는 기기 안에서만 수행됩니다. 이 앱은 `INTERNET` 권한을 요청하지 않으며, 분석/텔레메트리/크래시 리포팅/광고/외부 서비스를 포함하지 않습니다. 예고 알림과 진단 정보에도 ChatGPT 알림 제목, 본문, 텍스트, 접근성 화면 텍스트는 포함되지 않습니다.

## 진단 정보 해석

앱 화면의 **진단 정보** 섹션은 로컬에만 저장되는 내용 없는 상태값을 보여 줍니다.

- 마지막 ChatGPT 알림 감지
- 마지막 실행 시도
- 마지막 실행 결과
- 마지막 재시도 시간
- 마지막 재시도 결과
- 마지막 접근성 보조 시도
- 마지막 접근성 보조 결과
- 마지막 별도 알림 결과
- 마지막 전환 예고 방식
- 마지막 전환 예고 숫자
- 마지막 전환 예고 결과
- 마지막 전환 예고 생략 사유
- 현재 권한 상태
- 현재 설정 요약

주요 결과값:

- 아직 없음
- 실행 시도함
- 건너뜀: 자동 실행 꺼짐
- 건너뜀: 쿨다운 중
- 건너뜀: 조용한 시간
- 건너뜀: 화면 꺼짐
- 건너뜀: 기기 잠김
- 실패: ChatGPT 미설치
- 실패: 예외 발생
- 접근성 보조 꺼짐
- 접근성 보조 시도함
- 건너뜀: 대기 중인 실행 요청 없음
- 건너뜀: 안전하지 않은 앱
- 건너뜀: 제한 시간 초과
- 성공: ChatGPT 전면 감지
- 별도 알림 꺼짐
- 별도 알림 표시됨
- 전환 예고 시작
- 전환 예고 숫자 표시: 1
- 전환 예고 완료
- 전환 예고 후 실행 시작
- 전환 예고 꺼짐
- 전환 예고 시간 0ms
- 전환 예고 알림 권한 없음
- 오류: 전환 예고 우회됨

진단 정보는 Samsung Galaxy Z Flip5 / One UI에서 실제 자동 전환이 차단되는지 확인하기 위한 로컬 확인 도구입니다.

## Samsung Galaxy Z Flip5 권장 설정

1. APK를 설치합니다.
2. GPT Alert Launcher의 알림 접근 권한을 허용합니다.
3. 배터리 사용량을 제한 없음으로 설정하거나 배터리 최적화에서 제외합니다.
4. Samsung 설정에서 가능한 경우 절전 예외 또는 절전 안 함 앱에 추가합니다.
5. 최대 자동 전환을 원할 때만 앱의 접근성 보조 모드를 켜고 Android 접근성 설정에서 서비스를 허용합니다.
6. 최상의 결과를 위해 화면을 켜고 기기를 잠금 해제 상태로 유지합니다.
7. 공식 ChatGPT 앱의 알림이 켜져 있는지 확인합니다.

## 앱 화면

앱 화면은 Samsung Galaxy Z Flip5 세로 화면, tall/narrow 화면, 한 손 사용을 고려해 큰 버튼과 짧은 섹션 중심으로 구성되어 있습니다.

- 상태 요약
- 자동 실행 설정
- 접근성 보조 설정
- 자동 실행 세부 설정(전환 예고 시간, 쿨다운, 실행 지연, 재시도 지연, 접근성 보조 제한 시간을 키보드 입력 없이 휠로 선택)
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
- 분석, 텔레메트리, 크래시 리포팅, 광고, 외부 서비스
- 알림 내용 또는 접근성 화면 내용 업로드/전송
- 광범위한 화면 자동화

## 필요한 권한

| 권한 / 접근 | 이유 |
| --- | --- |
| 알림 접근 권한 | ChatGPT 앱 알림 콜백을 받고 패키지명으로 먼저 필터링하기 위해 필요합니다. |
| 선택 접근성 서비스 | 사용자가 명시적으로 켠 경우 ChatGPT 알림 뒤 전면 전환 가능성을 높이기 위해 필요합니다. |
| `POST_NOTIFICATIONS` on Android 13+ | 숫자만 표시하는 전환 예고 알림과, 사용자가 선택적으로 켠 실패 시 별도 알림을 표시하기 위해 필요합니다. |

이 앱은 `INTERNET` 권한을 요청하지 않습니다.


## 업데이트 설치와 권한 유지

기존 앱을 삭제하지 않고 APK를 업데이트 설치해야 알림 접근 권한과 사용자가 직접 켠 접근성 보조 설정을 최대한 유지할 수 있습니다. Android는 `applicationId`와 서명키가 같고 `versionCode`가 올라간 APK를 같은 앱의 업데이트로 인정합니다.

- 이 앱의 `applicationId`는 `com.example.gptalertlauncher`로 유지됩니다.
- 이번 버전은 `versionCode`를 올려 Android가 이전 APK 위에 업데이트 설치할 수 있게 했습니다.
- 서명키가 바뀌면 Android가 업데이트로 인정하지 않을 수 있으며, 이 경우 기존 앱 삭제/재설치가 필요해 권한 설정을 다시 해야 할 수 있습니다.
- 안정적인 업데이트를 위해 같은 서명키를 계속 사용해야 합니다.
- GitHub Actions는 `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` Secrets가 모두 준비되면 해당 키로 debug APK를 서명할 수 있습니다. keystore와 비밀번호는 저장소에 커밋하지 마세요.
- Secrets가 없으면 일반 debug signing을 사용합니다. 이 경우 APK 빌드는 계속 가능하지만, 다른 서명키로 빌드된 APK 위에 업데이트 설치가 되지 않을 수 있습니다.

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

Android/Samsung One UI가 백그라운드 앱 실행을 차단했을 수 있습니다. 화면 켜짐, 잠금 해제, 다른 앱 사용 중, 조용한 시간 밖, 쿨다운 종료 상태에서 다시 테스트하고 진단 정보를 확인하세요. 최대 자동 전환을 원하면 선택 접근성 보조 모드를 직접 켤 수 있지만, 그래도 자동 전환이 보장되지는 않습니다.

### 접근성 보조가 동작하지 않음

앱에서 **접근성 보조 모드**를 켜고 **접근성 설정 열기**로 이동해 Android 설정에서도 **GPT 알림 접근성 보조**를 허용했는지 확인하세요. 대기 중인 ChatGPT 실행 요청이 없거나 제한 시간이 지나면 보조 동작은 건너뜁니다.

### 별도 알림이 보이지 않음

별도 알림은 기본값이 꺼짐입니다. 앱에서 **실패 시 별도 알림 표시**를 켜고, Android 13+에서는 알림 표시 권한을 허용하세요.

### 알림이 감지되지 않음

앱의 **알림 접근 권한 설정 열기** 버튼을 눌러 GPT Alert Launcher의 알림 접근 권한을 허용하세요. 공식 ChatGPT 앱 알림이 켜져 있고 패키지명이 `com.openai.chatgpt`인지 확인하세요.

### 반복 실행이 억제됨

쿨다운 기본값은 10초입니다. 앱의 **쿨다운** 행을 누른 뒤 휠에서 값을 선택하고 **적용**을 누르세요.
