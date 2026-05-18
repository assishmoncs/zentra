# Zentra

Zentra is an Android MVP app that calculates a daily **Life Score (0–100)** from device screen usage and shows concise feedback to help users reduce distraction.

- **Package:** `com.hsissa.zentra`
- **Platform:** Android (API 26+)
- **UI:** Kotlin + XML (no Jetpack Compose)

## What the app does

- Requests and validates Usage Access permission
- Reads today’s foreground usage from `UsageStatsManager`
- Calculates a Life Score using threshold-based penalties
- Displays:
  - Daily screen time
  - Top 3 most-used apps
  - Score-based feedback (high / mid / low)
- Handles operational states in the main screen:
  - Loading state while usage data is fetched
  - Retryable error state when usage data cannot be read
  - Unexpected-empty state handling for OEM/device inconsistencies

## Score model

`Score = 100 - penalty`, clamped to `0..100`.

Penalty rules:
- No penalty up to **90 minutes**
- Moderate penalty from **90–240 minutes**
- Higher penalty above **240 minutes**

Feedback bands:
- **High:** `>= 80`
- **Mid:** `50..79`
- **Low:** `< 50`

## Project structure

```text
app/src/main/java/com/hsissa/zentra/
├── core/        # score calculation logic
├── service/     # UsageStats + permission + summary state handling
├── ui/          # MainActivity and UI rendering
└── util/        # shared formatting helpers
```

## Setup

1. Open the project in Android Studio.
2. Sync Gradle.
3. Run on an Android 8.0+ device or emulator.
4. In Zentra, tap **Open Settings** and enable **Usage access** for the app.

## Testing

Unit tests cover score behavior and time formatting in:
- `app/src/test/java/com/hsissa/zentra/core/ScoreManagerTest.kt`
- `app/src/test/java/com/hsissa/zentra/util/TimeFormatterTest.kt`

Run tests with:

```bash
gradle test
```

## Current MVP scope

- Data shown is for **today only**
- No local persistence/history storage
- No notifications or goals system
- Usage quality is based on duration, not app category/context

## License

This repository is licensed under [LICENSE](./LICENSE).
