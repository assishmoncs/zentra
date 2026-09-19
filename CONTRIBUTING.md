# Contributing to Zentra

## Development flow

Use the following branch model:

- `main` — stable releases
- `develop` — integration branch
- `feature/*` — new features
- `fix/*` — bug fixes
- `chore/*` — maintenance and tooling

## Before opening a pull request

Run the available verification tasks locally:

```bash
./gradlew test
./gradlew lint
./gradlew assembleRelease
```

Keep pull requests focused. Avoid mixing feature work with unrelated refactors or formatting-only changes.

## Commit messages

Prefer clear, imperative messages such as:

- `feat: add focus score breakdown`
- `fix: correct midnight usage rollover`
- `refactor: split usage data source`
- `test: cover score threshold boundaries`
- `chore: update dependency catalog`
