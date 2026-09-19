# Phase 2 — Architecture Refactor

## Goal

Improve internal separation without changing Zentra's existing MVVM, Hilt, Room, repository, service, and View-based architecture.

## Implemented

- Dashboard now depends on `GetTodayUsageUseCase` and `GetWeeklyTrendUseCase`.
- Added `UsageRepositoryContract` so presentation/domain layers depend on an abstraction.
- Split usage retrieval into `UsageStatsDataSource`.
- Split Room/cache and JSON serialization into `UsageLocalDataSource`.
- Refactored `UsageRepository` to coordinate data sources and retain the existing seven-day behavior.
- Updated Hilt wiring for the repository contract and shared Gson instance.
- Added unit tests for the new use-case boundary.

## Preserved behavior

- Existing dashboard LiveData API.
- Existing `TodayUsageResult` and `DailyUsageSummary` models.
- Existing UsageStatsManager logic.
- Existing Room schema and cache behavior.
- Existing navigation and UI.

## Validation

Run locally before merging to `main`:

```text
./gradlew test
./gradlew lint
./gradlew assembleRelease
```

Then manually verify the dashboard, weekly insights, usage permission flow, cached historical data, and app restart behavior.
