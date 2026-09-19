# Zentra

**Zentra** is an Android digital wellbeing and productivity app designed to help users build more intentional device habits through usage analytics, focus sessions, app limits, and lightweight mindfulness interventions.

Zentra turns Android usage data into a weighted **Focus Score**, combines it with historical trends and goal tracking, and provides tools for focused work without requiring a cloud backend.

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-34-3DDC84?logo=android&logoColor=white)](https://developer.android.com/about/versions/14)
[![Java](https://img.shields.io/badge/JDK-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## Overview

Zentra focuses on four core areas:

- **Understand** — analyze daily and weekly screen-time patterns.
- **Score** — convert usage into a weighted Focus Score.
- **Focus** — run Pomodoro and Deep Focus sessions that remain active in the background.
- **Control** — set app limits and introduce short mindfulness pauses when limits are reached.

The project follows a local-first architecture using Android system APIs, Room persistence, ViewModels, use cases, repositories, and dependency injection.

---

## Features

### Focus Score
Zentra calculates a weighted score from application usage:

| App Category | Weight |
| --- | ---: |
| Productive | 20% |
| Neutral | 100% |
| Distracting | 200% |

Higher-weight usage contributes more strongly to the score penalty, allowing the metric to distinguish between productive, neutral, and distracting screen time.

### Focus Sessions
- Pomodoro-style focus sessions
- Deep Focus mode
- Foreground-service based execution
- Persistent notifications while a session is active
- Quick Settings tile for fast session control

### Usage Analytics
- Today's usage breakdown
- 7-day usage trends
- Category-based usage visualization
- Focus Score history
- Goal tracking
- Focus streaks
- Daily focus-minute history

### App Limits & Mindfulness
- Per-app daily usage limits
- Background monitoring through WorkManager
- Short breathing/mindfulness intervention when configured limits are reached
- Dedicated mindfulness overlay activity

### Android System Integration
- Home-screen widget
- Quick Settings tile
- Android Usage Stats integration
- Notification support

### Local Data & Security
- Room database for local usage and focus history
- Encrypted SharedPreferences for protected preferences
- Offline-first data flow for locally available information

---

## Architecture

Zentra uses a layered **MVVM + Use Case + Repository** architecture.

```text
UI
│
├── Fragments / Activities
│
▼
ViewModels
│
▼
Use Cases
│
▼
Repository Contracts
│
▼
Repositories
├── UsageStats data source
└── Room/local data source
```

This separation keeps Android-specific data access behind repository/data-source boundaries while keeping presentation logic in ViewModels and domain operations in use cases.

### Main layers

| Layer | Responsibility |
| --- | --- |
| `ui/` | Screens, ViewModels, custom views, and mindfulness UI |
| `domain/` | Repository contracts and application use cases |
| `data/` | Room persistence, repositories, and data sources |
| `core/` | Core business logic such as Focus Score calculation and app limits |
| `service/` | Foreground service, WorkManager worker, and Quick Settings service |
| `widget/` | Android App Widget implementation |
| `di/` | Hilt dependency injection modules |
| `util/` | Shared helpers and formatting utilities |

---

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| UI | XML, ViewBinding, Material 3 |
| Architecture | MVVM, Use Cases, Repository pattern |
| Dependency Injection | Dagger Hilt |
| Local Database | Room |
| Async / Concurrency | Kotlin Coroutines |
| Lifecycle | LiveData, ViewModel |
| Background Work | WorkManager |
| Long-running Focus Sessions | Android Foreground Service |
| System Usage Data | `UsageStatsManager` |
| Serialization | Gson |
| Secure Preferences | AndroidX Security Crypto |
| Annotation Processing | KSP |
| Build Toolchain | Gradle, Android Gradle Plugin |
| Java Runtime | JDK 17 |

---

## Project Structure

```text
app/src/main/java/com/hsissa/zentra/
├── core/            # Core business logic and managers
├── data/
│   ├── local/       # Room database, DAOs, entities
│   ├── repository/  # Repository implementations
│   └── datasource/  # Local and Android usage data sources
├── di/              # Hilt modules
├── domain/
│   ├── repository/  # Repository contracts
│   └── usecase/     # Application use cases
├── service/         # Foreground service, worker, Quick Settings tile
├── ui/              # Screens, ViewModels, activities, custom views
├── util/            # Shared utilities and formatters
└── widget/          # Home-screen widget
```

---

## Requirements

- Android Studio with Android SDK 34
- JDK 17
- Android device or emulator
- Minimum Android version: **Android 8.0 (API 26)**

---

## Getting Started

### 1. Clone

```bash
git clone https://github.com/assishmoncs/zentra.git
cd zentra
```

### 2. Open in Android Studio

Open the project in Android Studio and allow Gradle to sync.

### 3. Run

Launch the `app` configuration on an emulator or physical Android device.

On first launch, Zentra may require system permissions such as:

- **Usage Access** — required to read Android usage statistics.
- **Notifications** — required for focus-session and related notifications.

The exact permission prompts depend on the Android version and device configuration.

---

## Gradle Commands

Run these from the project root.

### Unit tests

```bash
./gradlew test
```

### Lint

```bash
./gradlew lint
```

### Release build

```bash
./gradlew assembleRelease
```

On Windows PowerShell:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleRelease
```

---

## Testing

The project contains unit tests covering core logic and ViewModel behavior, including:

- Focus Score calculations
- Time formatting
- Dashboard state handling
- Insights state handling
- Coroutine-based ViewModel execution

The recommended validation sequence before merging changes is:

```text
./gradlew test
./gradlew lint
./gradlew assembleRelease
```

---

## Design & UX

Zentra's UI follows a shared design system with:

- Consistent spacing and typography tokens
- Material 3 components
- Large touch targets
- Explicit loading, empty, and error states
- Accessible content descriptions for important interactive elements
- Responsive layouts designed for different screen sizes

---

## Current Status

Zentra is under active development.

The current codebase includes:

- Core Focus Score logic
- Usage analytics and 7-day trends
- Focus sessions
- App limits and mindfulness interventions
- Local persistence
- Home-screen widget
- Quick Settings integration
- MVVM/use-case/repository separation
- Shared UI design system and accessibility refinements

---

## Roadmap

Potential future work includes:

- More detailed analytics and insights
- Expanded app categorization controls
- Additional focus-session modes
- More widget capabilities
- Broader automated test coverage
- Further performance and battery optimizations

---

## License

Distributed under the [MIT License](LICENSE).
