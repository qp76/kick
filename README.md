# Kick Lite

A lightweight native Android KICK client built with Jetpack Compose, Media3, Hilt and Retrofit.

The project now has an **official KICK Public API integration** for livestream discovery and a production-oriented OAuth architecture. The UI is inspired by the mobile KICK layout shown in the project screenshots: featured creators, live categories, horizontal live rows, dark UI, green KICK accent, bottom navigation, channel pages and the existing player stack.

## Official API

The data layer uses KICK's documented Public API rather than depending on KICK website JSON endpoints for livestream discovery.

- API: `https://api.kick.com/`
- Livestream discovery: `/public/v2/livestreams`
- Users: `/public/v1/users`
- Channels: `/public/v1/channels`
- Categories: `/public/v1/categories`
- OAuth server: `https://id.kick.com`

Official docs: https://docs.kick.com/

## Authentication / security

Do **not** put a KICK client secret in the Android APK.

The repo includes `backend/kick-gateway`, a small Cloudflare Worker that performs the confidential OAuth token exchange. The Android app uses PKCE, receives a short-lived one-time exchange code, and stores only the resulting access token locally.

For quick development/testing, Settings also has an access-token field. Never commit a real token to GitHub.

### Worker setup

1. Create a Cloudflare Worker and a KV namespace.
2. Put the Worker ID in `backend/kick-gateway/wrangler.toml`.
3. Set Worker secrets:
   - `KICK_CLIENT_ID`
   - `KICK_CLIENT_SECRET`
   - `KICK_REDIRECT_URI` = `https://YOUR-WORKER/oauth/callback`
4. Deploy the Worker.
5. Configure Android/Codemagic variables:
   - `KICK_OAUTH_GATEWAY_URL`
   - `KICK_CLIENT_ID`

The KICK app's redirect URI must match the Worker callback URI exactly.

## Codemagic

`codemagic.yaml` builds an installable release APK. Recommended variables/secrets:

- `KICK_OAUTH_GATEWAY_URL`
- `KICK_CLIENT_ID`
- `KICK_API_BASE_URL` (normally `https://api.kick.com/`)

For development-only builds you may also set `KICK_API_TOKEN` as a protected Codemagic secret.

The client secret must stay in the OAuth Worker and must **never** be added to Codemagic Android build variables.

## Existing features retained

- HLS livestream playback with Media3
- quality selector / data saver
- PiP and mini-player
- background playback / notification controls
- live chat WebSocket
- channel profiles
- VOD/clip players
- search screen
- following/local follow state
- theme settings
- watch history
- live previews

Some KICK features are only available through KICK's current public API scopes. The official docs are the source of truth as the API evolves.

## Build

The project targets Android 34 and Java 17 in Codemagic.

```bash
gradle assembleDebug
gradle assembleRelease
```

The repository intentionally does not contain signing credentials or KICK secrets.
