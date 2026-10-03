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


## Advanced features in 1.1.0

- Home category chips sourced from live stream data.
- Chat user mute by long-press, persistent keyword highlighting, and pinned-message presentation when Kick supplies pin metadata.
- Configurable fullscreen chat width and persistent chat preferences.
- Data Saver mode prefers the lowest available HLS quality.
- Real channel-video API results only; no fabricated fallback videos.
- Kick OAuth 2.1 PKCE browser/deep-link flow scaffold.

### Kick login

Kick requires a registered application client ID and redirect URI. The Android client starts the official `https://id.kick.com/oauth/authorize` flow and validates the OAuth `state` on return. Kick's documented authorization-code exchange also requires a client secret, so the final token exchange must be performed by a trusted backend rather than shipping the secret inside the APK.

For a production build, set `KICK_CLIENT_ID` and `KICK_REDIRECT_URI` in the build environment and register the exact redirect URI in the Kick developer application. The default Android callback is `kicklite://oauth/callback`.
