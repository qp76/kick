# Kick Lite

Kick Lite is a lightweight native Android client for Kick.com livestreams and VODs, built with Jetpack Compose, Media3 and Hilt.
It is based on the open-source [Kickstra](https://github.com/mucahit-sahin/Kickstra) project.

## Build the APK with Codemagic

1. Push this folder to a GitHub / GitLab / Bitbucket repository (the repo root must contain `codemagic.yaml`).
2. In Codemagic: **Add application** -> pick the repo -> choose the **codemagic.yaml** configuration.
3. Start a build of the `kick-lite-apk` workflow.
4. Download `app-release.apk` from the build artifacts.

The workflow installs Gradle 8.2, creates a self-signed keystore and builds a signed release APK. Package name: `com.kicklite`.

## Build locally

```
gradle assembleRelease      # Gradle 8.2, JDK 17, Android SDK 34
```

## Project layout

```
app/src/main/java/com/kicklite/
├── data/          # DTOs, Retrofit services, repositories, local preferences
├── domain/        # Models, repository contracts, use cases
├── presentation/  # Compose screens (home, player, channel, search, following, settings)
├── di/            # Hilt modules
└── ui/theme/      # Kick-Lite colors, typography, shapes
```
