# KICK Lite OAuth Gateway

This small Cloudflare Worker keeps the KICK client secret off the Android APK.

It implements:

- `GET /health`
- `GET /oauth/start?state=...&code_challenge=...`
- `GET /oauth/callback`
- `POST /oauth/exchange` with `{ "code": "...", "state": "..." }`
- `POST /oauth/revoke`
- `ALL /api/*` as an authenticated proxy to `https://api.kick.com/*`

Required Worker secrets:

- `KICK_CLIENT_ID`
- `KICK_CLIENT_SECRET`
- `KICK_REDIRECT_URI` (example: `https://your-worker.example.workers.dev/oauth/callback`)

Required KV binding:

- `OAUTH_SESSIONS`

The Android app should use the worker URL as `KICK_OAUTH_GATEWAY_URL` and keep the KICK client secret only in Worker secrets.
