interface Env {
  KICK_CLIENT_ID: string;
  KICK_CLIENT_SECRET: string;
  KICK_REDIRECT_URI: string;
  OAUTH_SESSIONS: KVNamespace;
}

const KICK_AUTH = "https://id.kick.com/oauth/authorize";
const KICK_TOKEN = "https://id.kick.com/oauth/token";
const KICK_API = "https://api.kick.com";

function json(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" }
  });
}

function randomCode(): string {
  const bytes = new Uint8Array(32);
  crypto.getRandomValues(bytes);
  return btoa(String.fromCharCode(...bytes)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=/g, "");
}

function configured(env: Env): boolean {
  return Boolean(env.KICK_CLIENT_ID?.trim() && env.KICK_CLIENT_SECRET?.trim() &&
    env.KICK_REDIRECT_URI?.trim() && env.OAUTH_SESSIONS);
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    if (url.pathname === "/health") {
      return json({ ok: true, service: "kick-lite-oauth-gateway" });
    }

    if (url.pathname === "/oauth/start") {
      if (!configured(env)) return json({ error: "OAuth gateway is not fully configured" }, 503);
      const state = url.searchParams.get("state");
      const codeChallenge = url.searchParams.get("code_challenge");
      if (!state || !codeChallenge) return json({ error: "state and code_challenge are required" }, 400);
      if (!/^[A-Za-z0-9_-]{20,128}$/.test(state) ||
          !/^[A-Za-z0-9_-]{43,128}$/.test(codeChallenge)) {
        return json({ error: "Invalid state or PKCE challenge format" }, 400);
      }

      const auth = new URL(KICK_AUTH);
      auth.searchParams.set("response_type", "code");
      auth.searchParams.set("client_id", env.KICK_CLIENT_ID);
      auth.searchParams.set("redirect_uri", env.KICK_REDIRECT_URI);
      auth.searchParams.set("scope", "user:read channel:read chat:write events:subscribe kicks:read");
      auth.searchParams.set("code_challenge", codeChallenge);
      auth.searchParams.set("code_challenge_method", "S256");
      auth.searchParams.set("state", state);
      return Response.redirect(auth.toString(), 302);
    }

    if (url.pathname === "/oauth/callback") {
      const code = url.searchParams.get("code");
      const state = url.searchParams.get("state");
      const providerError = url.searchParams.get("error");
      if (providerError) {
        const callback = new URL("kicklite://oauth/callback");
        callback.searchParams.set("error", providerError);
        if (state) callback.searchParams.set("state", state);
        const description = url.searchParams.get("error_description");
        if (description) callback.searchParams.set("error_description", description.slice(0, 300));
        return Response.redirect(callback.toString(), 302);
      }
      if (!code || !state) return new Response("Missing OAuth code/state. Return to Kick Lite and try again.", { status: 400 });
      if (!configured(env)) return new Response("OAuth gateway is not fully configured.", { status: 503 });

      const verifier = await env.OAUTH_SESSIONS.get(`pkce:${state}`);
      if (!verifier) return new Response("OAuth state expired or invalid. Return to Kick Lite and try again.", { status: 400 });

      const body = new URLSearchParams({
        code,
        client_id: env.KICK_CLIENT_ID,
        client_secret: env.KICK_CLIENT_SECRET,
        redirect_uri: env.KICK_REDIRECT_URI,
        grant_type: "authorization_code",
        code_verifier: verifier
      });
      let tokenResponse: Response;
      try {
        tokenResponse = await fetch(KICK_TOKEN, {
          method: "POST",
          headers: { "content-type": "application/x-www-form-urlencoded" },
          body
        });
      } catch {
        return new Response("Could not reach Kick token service. Return to Kick Lite and try again.", { status: 502 });
      }

      if (!tokenResponse.ok) {
        const details = (await tokenResponse.text()).slice(0, 500);
        // Keep the PKCE verifier until the provider confirms the exchange succeeded.
        return new Response(`Kick token exchange failed (HTTP ${tokenResponse.status}). ${details}`, { status: 502 });
      }

      const token = await tokenResponse.json() as Record<string, unknown>;
      if (typeof token.access_token !== "string" || !token.access_token) {
        return new Response("Kick token response did not contain an access token.", { status: 502 });
      }

      const exchangeCode = randomCode();
      await env.OAUTH_SESSIONS.put(`token:${exchangeCode}`, JSON.stringify({ state, token }), { expirationTtl: 90 });
      // Only consume the PKCE verifier after a successful upstream exchange.
      await env.OAUTH_SESSIONS.delete(`pkce:${state}`);

      return Response.redirect(`kicklite://oauth/callback?code=${encodeURIComponent(exchangeCode)}&state=${encodeURIComponent(state)}`, 302);
    }

    if (url.pathname === "/oauth/exchange" && request.method === "POST") {
      const payload = await request.json().catch(() => null) as { code?: string; state?: string } | null;
      if (!payload?.code || !payload.state) return json({ error: "code and state are required" }, 400);
      const key = `token:${payload.code}`;
      const raw = await env.OAUTH_SESSIONS.get(key);
      if (!raw) return json({ error: "Exchange code expired or already used. Please sign in again." }, 400);

      let record: { state?: string; token?: Record<string, unknown> };
      try {
        record = JSON.parse(raw) as { state?: string; token?: Record<string, unknown> };
      } catch {
        await env.OAUTH_SESSIONS.delete(key);
        return json({ error: "Invalid exchange session. Please sign in again." }, 400);
      }
      if (!record.state || record.state !== payload.state || !record.token) {
        return json({ error: "OAuth state mismatch. Please sign in again." }, 400);
      }

      await env.OAUTH_SESSIONS.delete(key);
      return json(record.token);
    }

    if (url.pathname === "/oauth/revoke" && request.method === "POST") {
      const auth = request.headers.get("authorization");
      if (!auth) return json({ error: "authorization required" }, 401);
      const token = auth.replace(/^Bearer\s+/i, "");
      const revokeUrl = new URL("https://id.kick.com/oauth/revoke");
      revokeUrl.searchParams.set("token", token);
      revokeUrl.searchParams.set("token_hint_type", "access_token");
      const response = await fetch(revokeUrl, { method: "POST" });
      return new Response(await response.text(), {
        status: response.status,
        headers: { "cache-control": "no-store" }
      });
    }

    if (url.pathname === "/oauth/pkce" && request.method === "POST") {
      if (!configured(env)) return json({ error: "OAuth gateway is not fully configured" }, 503);
      const payload = await request.json().catch(() => null) as { state?: string; code_verifier?: string } | null;
      if (!payload?.state || !payload.code_verifier) {
        return json({ error: "state and code_verifier are required" }, 400);
      }
      if (!/^[A-Za-z0-9_-]{20,128}$/.test(payload.state) ||
          !/^[A-Za-z0-9_-]{43,128}$/.test(payload.code_verifier)) {
        return json({ error: "Invalid state or code_verifier format" }, 400);
      }
      await env.OAUTH_SESSIONS.put(`pkce:${payload.state}`, payload.code_verifier, { expirationTtl: 600 });
      return json({ ok: true });
    }

    if (url.pathname.startsWith("/api/")) {
      const auth = request.headers.get("authorization");
      if (!auth) return json({ error: "authorization required" }, 401);
      const upstreamPath = url.pathname.substring("/api".length);
      const upstream = new URL(KICK_API + upstreamPath + url.search);
      const headers = new Headers(request.headers);
      headers.set("authorization", auth);
      headers.delete("host");
      const response = await fetch(upstream, {
        method: request.method,
        headers,
        body: request.method === "GET" || request.method === "HEAD" ? undefined : request.body
      });
      const out = new Response(response.body, response);
      out.headers.set("cache-control", "no-store");
      return out;
    }

    return json({ error: "not found" }, 404);
  }
};
