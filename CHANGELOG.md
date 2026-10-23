# Changelog

All notable changes to Zentra are documented here.

## Phase 4 — Production Readiness & Hardening
- Fixed Android 14 (API 34) foreground service permission crash by declaring `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, and `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` in `AndroidManifest.xml`.
- Replaced restricted `QUERY_ALL_PACKAGES` permission with scoped `<queries>` launcher intent filter.
- Added `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml` to exclude hardware-bound `EncryptedSharedPreferences` from cloud/device-transfer backups.
- Converted `SettingsManager` to a Hilt `@Singleton` with automatic Keystore corruption recovery and in-memory `ConcurrentHashMap` category caching.
- Fixed blank Daily Breakdown list in `InsightsFragment` by attaching `LinearLayoutManager` to `rvDailyBreakdown` in `fragment_insights.xml`.
- Upgraded `AppUsageAdapter` and `AppCategoryAdapter` to use `DiffUtil` and added bounded `LruCache` lazy icon loading in `AppCategoryAdapter`.
- Replaced per-second notification updates in `FocusSessionService` with native Android chronometer countdowns (`setUsesChronometer(true)`).
- Replaced `.fallbackToDestructiveMigration()` in `AppDatabase` with explicit `MIGRATION_1_2`, `.fallbackToDestructiveMigrationOnDowngrade()`, and `exportSchema = true`.
- Migrated `TimeFormatter`, `TrendChartView`, `UsageRepository`, and `UsageStatsHelper` from thread-unsafe `SimpleDateFormat` and `Calendar` to `java.time`.
- Scoped `app/proguard-rules.pro` for real R8 obfuscation and annotated `AppUsageInfo` and `AppCategory` with `@Keep` and `@SerializedName`.
- Added unit test suites (`AppLimitManagerTest`, `UsageLocalDataSourceTest`, `UsageRepositoryTest`, expanded `DashboardViewModelTest`), GitHub Actions CI (`.github/workflows/android-ci.yml`), `PRIVACY.md`, and `SECURITY.md`.

## Phase 3 — UI/UX and Design System
- Added shared spacing, typography, card, button, and touch-target tokens.
- Redesigned Dashboard, Insights, and Settings hierarchy.
- Standardized loading, empty, error, and retry presentation.
- Added accessibility semantics and responsive layout safeguards.
- Added Dashboard and Insights ViewModel regression tests.
- Polished widget and mindfulness overlay presentation.

## [2.2.0] - 2026-08-12

### Baseline
- Current stable project baseline before the Zentra quality and UX improvement program.
- Existing functionality includes usage analytics, Focus Score, focus sessions, app limits, mindfulness interventions, streaks, widgets, Quick Settings integration, Room persistence, and background processing.
