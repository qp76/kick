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

## Deploy to the existing production Worker

The Wrangler config targets the existing Worker named `kick`. From the repository root in Codespaces, run:

```sh
cd backend/kick-gateway
npx wrangler deploy
```

Keep `KICK_CLIENT_SECRET` out of source files and GitHub. In Cloudflare, open **Workers & Pages → kick → Settings → Variables and Secrets** and confirm these values exist for the Production environment:

- `KICK_CLIENT_ID`
- `KICK_CLIENT_SECRET` (secret)
- `KICK_REDIRECT_URI` = `https://kick.boy82102.workers.dev/oauth/callback`

Also confirm the `OAUTH_SESSIONS` KV binding points to the namespace ID in `wrangler.toml`. If these are already configured, leave them as they are; redeploying does not require changing the credentials.

After deploying, open `https://kick.boy82102.workers.dev/health` in your phone browser. The expected JSON contains `"ok":true`. Then build the `main` branch in Codemagic. The app routes public API calls through this Worker so it can obtain and cache an app access token server-side; the client secret must never be added to the APK.
