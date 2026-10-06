# Kick Lite implementation status

## Implemented in this revision

- Official KICK Public API client added.
- `/public/v2/livestreams` is now the Home stream source.
- Defensive JSON parsing for livestream/channel/category/thumbnail fields.
- Runtime KICK access-token storage.
- Settings screen token configuration for testing.
- PKCE OAuth architecture with an optional Cloudflare Worker gateway so the client secret stays server-side.
- Android `kicklite://oauth/callback` deep-link handling.
- Screenshot-inspired Home UI with Featured Creators, Top Live Categories and category-based live rows.
- Removed fabricated channel VOD fallback data.
- Codemagic variables documented.

## API boundaries

The current KICK Public API documentation does not expose every endpoint required to reproduce every first-party mobile feature. In particular, VOD/clip discovery, the exact following feed, and some viewer-side account features are not all represented by public API endpoints.

The project therefore keeps those areas behind their existing adapters instead of inventing API responses. They can be migrated when KICK exposes equivalent public endpoints.

## Important

This is a third-party client and is not the official KICK application. KICK branding/assets should only be used where you have permission to use them.
