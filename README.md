# Zentra 🌿

**Zentra** is a state-of-the-art Android digital wellbeing and productivity platform designed to foster intentional device usage through data-driven analytics, real-time mindfulness interventions, and background-resilient focus sessions.

Built with **Modern Android Development (MAD)** standards, Zentra transforms Android's system usage statistics into a weighted **Focus Score**, empowering users to regain control of their digital habits.

---

## ✨ Key Features

- 🎯 **Weighted Focus Scoring Engine**: Evaluates screen time using a penalty-decay formula that differentiates between *Productive* (20% weight), *Neutral* (100% weight), and *Distracting* (200% weight) applications.
- ⏱️ **Background-Resilient Focus Sessions**: Integrated Pomodoro & Deep Focus timer backed by a dedicated Android **Foreground Service** with persistent status notifications.
- 🧘 **Mindfulness Interventions & App Quotas**: Custom daily app limits that trigger a 5-second breathing pause overlay (`MindfulnessOverlayActivity`) when thresholds are reached.
- 📊 **Visual Analytics**: Custom-rendered `CategoryPieChartView` donut distribution and 7-day historical `TrendChartView` bar graphs with goal target indicators.
- 🔥 **Focus Streaks & History**: Persisted tracking of daily goal achievements, active focus streaks, and total focus minutes logged.
- 📱 **System UI Integration**:
  - **Home Screen Widget**: Live widget displaying real-time Focus Score and 1-tap launcher.
  - **Quick Settings Tile**: Android System Shade tile for instant Focus Session toggling.
- 🔒 **Encrypted Preferences & Offline-First Caching**: Sensitive user preferences encrypted via `EncryptedSharedPreferences` and local data cached in **Room Database**.

---

## 🛠 Architectural Stack & Technical Overview

Zentra is architected following Google's **Clean Architecture & MVVM** guidelines:

| Layer / Concern | Component / Library |
| :--- | :--- |
| **Language & Concurrency** | Kotlin, Coroutines, StateFlow / SharedFlow |
| **Dependency Injection** | **Dagger Hilt** (`@HiltAndroidApp`, `@HiltViewModel`, `@AndroidEntryPoint`) |
| **Local Persistence** | **Room Database (v2)** with KSP & Gson JSON converters |
| **Compiler Toolchain** | **KSP (Kotlin Symbol Processing)** for 2x faster build compilation |
| **Background Processing** | Foreground Service (`FocusSessionService`), WorkManager (`MindfulnessWorker`) |
| **System Integrations** | AppWidget Framework (`ZentraWidgetProvider`), Quick Settings Tile (`FocusTileService`) |
| **UI Framework** | Android ViewBinding, Material 3 Design Components, Custom Canvas Views |

---

## 📂 Project Structure

```text
app/src/main/java/com/hsissa/zentra/
├── core/            # Scoring logic (ScoreManager), AppLimitManager, SettingsManager
├── data/
│   ├── local/       # Room DB, DAOs, and Entities (UsageRecord, FocusStreakEntity, AppLimitEntity, FocusSessionEntity)
│   └── repository/  # UsageRepository (Offline-first caching layer)
├── di/              # Dagger Hilt Dependency Injection Modules (AppModule)
├── service/         # FocusSessionService (Foreground), MindfulnessWorker, FocusTileService
├── ui/              # Fragments, ViewModels, MindfulnessOverlayActivity, Custom Views (TrendChartView, CategoryPieChartView)
├── util/            # Helper utilities, NotificationHelper, TimeFormatter
└── widget/          # ZentraWidgetProvider (Android AppWidget)
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Iguana (2023.2.1) or newer
- **JDK**: Java 17
- **Target SDK**: Android 14 (API 34)
- **Minimum SDK**: Android 8.0 (API 26)

### Build & Run

1. **Clone the repository**:
   ```bash
   git clone https://github.com/hsissa/zentra.git
   cd zentra
   ```

2. **Build the Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Deploy to Device / Emulator**:
   - Deploy via Android Studio or `adb install app/build/outputs/apk/debug/app-debug.apk`.
   - **Permissions**: Grant **Usage Access** permission when prompted on first launch to allow Zentra to analyze session events.

---

## 🧪 Testing

The codebase includes unit test coverage for scoring logic and repository operations:

```bash
./gradlew test
```

---

## 📜 License

Distributed under the [MIT License](./LICENSE).
