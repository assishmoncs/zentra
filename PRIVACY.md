# Privacy Policy for Zentra

**Effective Date:** October 23, 2026

Zentra ("we", "our", or "the app") is a local-first digital wellbeing and productivity application for Android. Protecting your privacy is foundational to Zentra's design.

## 1. Data Accessed on Your Device

To calculate your Focus Score, display daily and 7-day screen-time trends, and provide mindfulness alerts, Zentra requests the following Android system permissions:

* **Usage Access (`android.permission.PACKAGE_USAGE_STATS`):** Used strictly on-device to read foreground application usage durations via Android's `UsageStatsManager`.
* **Launchable App Visibility (`<queries>` for `ACTION_MAIN` / `CATEGORY_LAUNCHER`):** Used strictly on-device to resolve human-readable app labels, icons, and user-configured categories (Productive, Neutral, Distracting) for installed launchable apps.
* **Notifications (`android.permission.POST_NOTIFICATIONS`):** Used to display active Focus Session countdown timers and optional local mindfulness reminders.

## 2. Local-Only Storage & Zero Off-Device Transmission

* **No Cloud Servers or Analytics SDKs:** Zentra does not request the `android.permission.INTERNET` permission. No usage statistics, installed app lists, Focus Scores, or personal identifiers ever leave your device.
* **On-Device Persistence:** Historical daily summaries, focus streaks, and app limits are stored locally in the app's private SQLite/Room database (`zentra_database`). User preferences and custom app categorizations are encrypted at rest using Android Keystore-backed `EncryptedSharedPreferences` (`zentra_settings`).

## 3. Data Retention & Deletion

You can clear all locally stored usage history, custom categories, and goals at any time by clearing Zentra's storage in **Android Settings → Apps → Zentra → Storage & cache → Clear storage**, or by uninstalling the application.
