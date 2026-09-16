# Phase 1 — Repository and Build Hygiene

## Completed
- [x] Remove tracked Android Studio `.idea` metadata
- [x] Tighten `.gitignore`
- [x] Add `.editorconfig`
- [x] Add Gradle version catalog with existing dependency versions
- [x] Update app dependencies to use the version catalog
- [x] Add `CHANGELOG.md`
- [x] Add `CONTRIBUTING.md`

## Local validation required
- [ ] `./gradlew test`
- [ ] `./gradlew lint`
- [ ] `./gradlew assembleRelease`
- [ ] Open project successfully in Android Studio
- [ ] Confirm Gradle sync succeeds
- [ ] Confirm app behavior is unchanged

## Exit criteria
Phase 1 is complete when all local validation passes. Do not upgrade dependency versions or change application behavior in this phase.
