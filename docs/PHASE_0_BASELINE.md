# Zentra Phase 0 — Baseline

## Baseline source

- Branch: `main`
- Commit: `45e2940b9912907d110719450d6a0fadf279cd69`
- Version: `2.2` (`versionCode 4`)
- Baseline commit message: `Minor fixes and improvements`
- Baseline captured: 2026-09-15

## Protected branches

- `main` — production/current baseline
- `develop` — integration branch for the improvement program
- `baseline/v2.2` — protected snapshot reference for the starting point
- `phase-0-baseline` — Phase 0 working branch

## Regression checklist

### Launch & onboarding

- [ ] App launches successfully
- [ ] Existing users can open the app without crashes
- [ ] First-launch permission flow works
- [ ] Usage Access permission state is handled correctly
- [ ] Notification permission state is handled correctly

### Dashboard

- [ ] Dashboard loads with valid usage data
- [ ] Dashboard handles no usage data
- [ ] Focus Score displays correctly
- [ ] Today's usage totals are correct
- [ ] Productive / neutral / distracting breakdown is correct

### Insights

- [ ] Weekly chart loads
- [ ] Historical days display correctly
- [ ] Chart item selection works
- [ ] Empty historical data is handled
- [ ] Missing cached day falls back correctly

### Focus sessions

- [ ] Focus session can start
- [ ] Timer continues when app is backgrounded
- [ ] Foreground notification appears correctly
- [ ] Session can complete
- [ ] Session data is persisted
- [ ] Session state survives app recreation where expected

### Limits & mindfulness

- [ ] App limits can be created/updated
- [ ] Limit warning/overlay triggers correctly
- [ ] Mindfulness overlay can be dismissed/continued
- [ ] Limits persist after restart

### System integrations

- [ ] Quick Settings tile appears
- [ ] Quick Settings tile starts/stops focus mode correctly
- [ ] Home-screen widget renders
- [ ] Widget launches the expected action
- [ ] Widget data refreshes correctly

### Persistence

- [ ] Settings persist after restart
- [ ] Room data can be read after restart
- [ ] Historical usage cache behaves correctly
- [ ] No data is lost during normal app restart

### Edge cases

- [ ] Missing Usage Access permission
- [ ] No usage data
- [ ] Empty Room database
- [ ] Corrupt cached JSON is handled
- [ ] App restarted during a focus session
- [ ] App moved between foreground/background repeatedly
- [ ] Midnight/day rollover is correct
- [ ] Large usage values do not break scoring

## Automated validation status

The repository contains unit tests for scoring and time formatting. The remote GitHub inspection confirms their presence, but this environment cannot clone the repository or execute the Android/Gradle build locally. Therefore, build/test execution is intentionally marked as pending rather than claimed as passed.

- [ ] `./gradlew test`
- [ ] `./gradlew lint`
- [ ] Release build
- [ ] Manual device/emulator regression pass

## Phase 0 exit criteria

Phase 0 is complete when:

1. The `main` baseline remains unchanged.
2. `develop` exists as the integration branch.
3. `baseline/v2.2` preserves the starting point.
4. The regression checklist is recorded.
5. Baseline Android build/test results are measured on a local Android environment before feature work begins.
6. All future changes are made from feature/fix branches and merged into `develop` before `main`.
