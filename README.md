# Kick Lite

A lightweight native Android client for Kick.com livestreams (Jetpack Compose, Media3, Hilt).
Based on the open-source [Kickstra](https://github.com/mucahit-sahin/Kickstra).

## Features
- Live chat over Kick's WebSocket, with real emotes (animated or still), badges, replies, @mentions and pinned messages
- Chat that never jumps while you scroll up, plus a "back to live chat" button
- Chat settings: text size, emote size, animated emotes, timestamps, compact mode, hide bots / `!` commands,
  highlight words, muted users, fullscreen chat width and side, performance mode, message limit
- Home category chips, pull to refresh, **live preview** (muted video preview of the stream resting on screen)
- Fullscreen gestures: double-tap pause, swipe for brightness / volume, side-by-side chat
- Data saver (caps Auto quality at 480p), mini player, picture-in-picture, background audio

## Push to GitHub
```
git init
git add .
git commit -m "Kick Lite"
git branch -M main
git remote add origin https://github.com/<you>/<repo>.git
git push -u origin main
```
`codemagic.yaml` must be at the repository root (it is).

## Build the APK with Codemagic (free)
1. Codemagic -> **Add application** -> pick the repo -> choose the **codemagic.yaml** configuration.
2. Start a build of the `kick-lite-apk` workflow on branch `main`.
3. Download `app-release.apk` from the build artifacts.

No secrets or variables are required. Optional variable: `ENABLE_R8=true` builds a smaller shrunk APK.

## Use Kick's original badge artwork (optional)
Badges are drawn in code as lookalikes. To use Kick's own artwork, put files named after the badge type into
`app/src/main/assets/badges/` (for example `moderator.svg`, `broadcaster.svg`, `vip.svg`, `og.svg`, `founder.svg`,
`verified.svg`, `subscriber.svg`, `sub_gifter.svg`, `staff.svg`; `.png` and `.webp` also work). They are picked up
automatically, no code change needed.

## Build locally
```
gradle assembleRelease      # Gradle 8.2, JDK 17, Android SDK 34
```

## Project layout
```
app/src/main/java/com/kicklite/
├── data/          # DTOs, Retrofit services, repositories, chat socket, preview player, preferences
├── domain/        # Models, repository contracts, use cases
├── presentation/  # Compose screens (home, player, channel, search, following, settings)
├── di/            # Hilt modules
└── ui/theme/      # Colors, typography, shapes
```
