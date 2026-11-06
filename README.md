# SimpleTODO

가볍고 단순한 개인 일정 관리 Android 앱입니다. 일정을 등록하고, 홈 화면 위젯으로 한눈에 확인하고, 필요한 때에 알림을 받을 수 있습니다.

## 주요 기능

### 일정 관리
- **일정 추가/수정**: 내용, 날짜(오늘·내일 빠른 선택 또는 달력), 선택적인 시간, 중요 표시를 지정할 수 있습니다. 목록에서 일정을 탭하면 수정 화면이 열립니다.
- **현재 일정 탭**: 완료하지 않은 일정을 날짜 구간별 헤더(기한 지남 / 오늘 / 내일 / 7일 이내 / 나중에)로 묶어 보여줍니다.
  - 정렬 순서: 날짜 → 시간(시간이 없는 일정은 그날의 뒤쪽) → 중요 일정 → 생성 시각
  - 기한이 지난 일정은 "기한 지남" 구간에 붉은색 날짜로 표시됩니다.
  - 체크박스를 누르면 완료 처리되어 완료한 일정 탭으로 이동합니다.
- **중요 일정**: 추가/수정 화면의 "중요" 토글로 지정하며, 앱과 위젯에서 보라색 제목과 별 아이콘으로 강조됩니다.
- **완료한 일정 탭**: 완료한 일정을 키워드와 시작일/종료일로 검색할 수 있습니다. 체크를 해제하면 현재 일정으로 복원되고, 휴지통 버튼으로 삭제합니다.

### 반복 알림
일정 목록과는 완전히 별개로 관리되는 주간 반복 알림입니다. 위젯이나 아침 브리핑에는 나타나지 않고, 별도의 알림 채널로 발송됩니다.
- 요일을 여러 개 선택하고 시간을 지정할 수 있습니다. 시간을 지정하지 않으면 설정의 아침 브리핑 시각에 알림이 옵니다.
- "반복 알림" 탭에서 추가, 수정, On/Off, 삭제가 가능합니다.
- 예: 월·화·수·목·금 07:00 "게임 출석체크", 월·수(시간 미지정) "강아지 산책"

### 홈 화면 위젯
- 현재 일정을 날짜 구간별로 묶어 스크롤 목록으로 보여줍니다.
- 위젯에서 체크박스를 눌러 바로 일정을 완료할 수 있습니다.
- `+` 버튼으로 앱의 일정 추가 화면을 바로 열고, 그 외 영역을 누르면 앱이 열립니다.
- 크기를 자유롭게 조절할 수 있습니다(최소 2×2, 최대 크기는 런처 그리드에 따름).
- 앱에서 일정을 변경하면 즉시 갱신되며, 30분 주기로 한 번 더 갱신됩니다.

### 알림
- **아침 브리핑**: 매일 설정한 시각에 그날의 모든 일정을 하나의 알림으로 모아 보여줍니다.
- **시간 지정 일정 알림**: 시간이 있는 일정은 브리핑과 별도로, 설정한 분만큼 미리 한 번 더 알려줍니다.
- 기기를 재부팅해도 알람이 다시 등록됩니다.
- 정확한 알람 권한(Android 12+)이 없으면 다소 부정확한 알람으로 대신 발송합니다.

### 설정
- **테마**: 시스템 기본 / 라이트 / 다크 (앱과 위젯에 함께 적용)
- **아침 브리핑 시각**
- **시간 지정 일정의 사전 알림 시점** (몇 분 전)
- **데이터 백업**: 일정, 반복 알림, 설정을 JSON 파일로 내보내고 다시 복원합니다. 복원하면 현재 데이터는 백업 내용으로 바뀝니다. 서명 키가 달라 덮어 설치할 수 없을 때(디버그 빌드 → 릴리스 빌드 등) 데이터를 옮기는 용도로도 쓸 수 있습니다.
- **앱 정보**: 버전, 소스 코드·오류 신고 링크, 사용한 오픈소스 라이선스

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| 언어 | Kotlin 2.3.10 |
| UI | Jetpack Compose (Material 3) |
| 데이터베이스 | Room 2.8.4 (KSP) |
| 위젯 | AppWidgetProvider + RemoteViews / RemoteViewsService |
| 알림 | AlarmManager, NotificationCompat |
| 빌드 | Gradle 9.5.0, Android Gradle Plugin 9.3.1 |

- `minSdk` 26 (Android 8.0) / `targetSdk` 37
- 별도의 화면 이동 라이브러리 없이 단일 Activity에서 Compose 상태로 화면을 전환합니다.

## 빌드 및 실행

### 요구 사항
- Android Studio (내장 JDK 사용)
- Android SDK Platform 37

### Android Studio
프로젝트 폴더를 열고 기기 또는 에뮬레이터를 선택한 뒤 **Run ▶** 을 누릅니다.

### 명령줄
```bash
./gradlew :app:assembleDebug
```

생성된 APK는 `app/build/outputs/apk/debug/app-debug.apk` 에 있으며, 다음 명령으로 설치할 수 있습니다.

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 릴리스 빌드
1. 서명 키를 만듭니다(최초 1회). 키 파일은 저장소 밖에 두고 반드시 백업하세요. 키를 잃어버리면 기존 설치본 위에 업데이트를 설치할 수 없습니다.
   ```bash
   keytool -genkeypair -v -keystore ../../keys/simpletodo-release.jks -alias simpletodo -keyalg RSA -keysize 2048 -validity 10000
   ```
2. `keystore.properties.example` 을 `keystore.properties` 로 복사하고 키 경로와 비밀번호를 채웁니다. 이 파일은 git에서 제외됩니다.
3. 서명된 APK를 빌드합니다.
   ```bash
   ./gradlew :app:assembleRelease
   ```
   결과물은 `app/build/outputs/apk/release/app-release.apk` 입니다. `keystore.properties` 가 없으면 서명되지 않은 `app-release-unsigned.apk` 가 생성됩니다.

> 디버그 빌드와 릴리스 빌드는 서명 키가 달라서 서로 덮어 설치할 수 없습니다. 디버그 빌드를 지우고 릴리스 빌드를 설치하면 기존 데이터가 삭제됩니다.

### 테스트
DAO 계측 테스트는 연결된 기기 또는 에뮬레이터가 필요합니다.

```bash
./gradlew :app:connectedDebugAndroidTest
```

## 프로젝트 구조

```
app/src/main/java/com/simpletodo/simpletodo/
├── MainActivity.kt          # 단일 Activity, 화면 전환
├── data/                    # Room 엔티티·DAO·마이그레이션, SharedPreferences 설정
├── notification/            # 알람 예약, 알림 표시, 부팅 시 재등록
├── ui/                      # Compose 화면, ViewModel
│   └── theme/               # 색상, 테마
└── widget/                  # 홈 화면 위젯 (RemoteViews)
app/schemas/                 # Room이 내보낸 버전별 DB 스키마
```

## 데이터 저장

- 모든 일정과 반복 알림은 기기 내부의 앱 전용 저장소에 Room 데이터베이스(`schedule.db`)로 저장됩니다. 외부 서버로 전송되지 않습니다.
- 앱 설정(테마, 알림 시각 등)은 SharedPreferences에 저장됩니다.
- 현재 DB 버전은 3이며, 버전마다 명시적인 마이그레이션(`data/Migrations.kt`)을 두어 앱을 업데이트해도 기존 데이터가 유지됩니다.
  - v1 → v2: 일정에 중요 표시(`isImportant`) 추가
  - v2 → v3: 반복 알림 테이블(`recurring_reminder`) 추가
- 스키마를 변경할 때는 `@Database` 의 `version` 을 올리고 해당 `Migration` 을 추가해야 합니다. 빌드 시 `app/schemas/` 에 새 스키마 JSON이 생성되며, 이 파일도 함께 커밋합니다.

## 권한

| 권한 | 용도 |
| --- | --- |
| `POST_NOTIFICATIONS` | 알림 표시 (Android 13+, 첫 실행 시 요청) |
| `SCHEDULE_EXACT_ALARM` | 정확한 시각의 알림 (Android 12+, 설정 화면에서 허용 안내) |
| `RECEIVE_BOOT_COMPLETED` | 재부팅 후 알람 재등록 |
