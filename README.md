# Zentra

Zentra is a Kotlin/XML Android app that reads daily usage stats and converts them into a **Life Score (0–100)** with short, actionable feedback.

Package: `com.hsissa.zentra`

## Current MVP Features

- Usage Access permission flow
- Daily total screen time (from midnight to now)
- Top 3 most-used apps today
- Life Score calculation with feedback states (high / mid / low)
- Minimal dark UI focused on quick reading

## Tech Stack

- Kotlin
- XML layouts (no Jetpack Compose)
- Android Usage Stats API (`UsageStatsManager`)
- Material Components

## Project Structure

```
app/src/main/java/com/hsissa/zentra/
├── core/        # score calculation logic
├── service/     # UsageStats + permission helpers
├── ui/          # activity and UI rendering
└── util/        # shared formatting helpers
```

## How the Score Works

Current score model uses threshold-based penalties:

- No penalty until healthy usage limit
- Moderate penalty in caution range
- Steeper penalty after excessive usage

Score is always clamped to `0..100`.

## Setup

1. Open in Android Studio (Hedgehog+ recommended).
2. Sync Gradle project.
3. Run app on Android 8.0+ device/emulator.
4. In app, tap **Open Settings** and enable **Usage access** for Zentra.

## Known MVP Limitations

- No historical charts yet (today only)
- No local persistence layer yet
- No notifications or usage goals yet
- Usage quality is based on duration only (not category/context)

## Roadmap

### Immediate fixes (must-do)
- Stabilize permission + empty-data states on more OEM devices
- Add unit tests for score thresholds and time formatting
- Add loading/error state when usage query returns empty unexpectedly

### Short-term improvements
- Add 7-day trend section (average score + total time)
- Add configurable daily target (e.g., 2h, 3h, 4h)
- Add app-level category tagging (productive/neutral/distracting)

### Future enhancements
- Weekly summary screen with streaks and milestones
- Local persistence for score history
- Smart nudges (optional notifications) based on trend drops

## License

This repository is licensed under the terms in [LICENSE](./LICENSE).
