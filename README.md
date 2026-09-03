# SEA — FlashLearn Android Foundation

This is the first implementation milestone for the frozen SEA / FlashLearn architecture.

## Stack

- Kotlin 2.3.21
- Android Gradle Plugin 9.4.0
- Gradle 9.6+
- Jetpack Compose BOM 2026.08.00
- Room 2.8.4
- Hilt 2.60.1
- Coroutines / Flow
- minSdk 26
- compileSdk / targetSdk 37

## Current milestone

Implemented:

- Android application skeleton
- Clean Architecture package boundaries
- Domain models
- Room v1 entities
- Required DAO boundaries
- Room database
- Hilt database provider
- Basic Compose entry screen
- Navigation foundation
- Git-safe `.gitignore`

Not yet implemented:

- Repository implementations
- Mappers
- Review transition engine
- Onboarding
- Real Home dashboard
- Vocabulary UI
- Review UI
- Import/export
- AI
- Production tests

## Open in Android Studio

Open the project root (`SEA`) in Android Studio and allow Gradle sync.

Use JDK 17.

Then run the `app` configuration on an Android 8.0+ emulator/device.

## First Git commit

After verifying the project builds:

```bash
git add .
git commit -m "feat: add Android foundation and Room schema"
git push origin main
```

The repository is intended to remain aligned with the frozen architecture documents in the parent GitHub repository.
