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

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    if (url.pathname === "/health") return json({ ok: true, service: "kick-lite-oauth-gateway" });

    if (url.pathname === "/oauth/start") {
      const state = url.searchParams.get("state");
      const codeChallenge = url.searchParams.get("code_challenge");
      if (!state || !codeChallenge) return json({ error: "state and code_challenge are required" }, 400);

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
      if (!code || !state) return new Response("Missing OAuth code/state", { status: 400 });

      // The verifier remains on the Android side. KICK validates the PKCE challenge
      // while this server supplies the confidential client secret.
      const verifier = await env.OAUTH_SESSIONS.get(`pkce:${state}`);
      if (!verifier) return new Response("OAuth state expired or invalid", { status: 400 });
      await env.OAUTH_SESSIONS.delete(`pkce:${state}`);

      const body = new URLSearchParams({
        code,
        client_id: env.KICK_CLIENT_ID,
        client_secret: env.KICK_CLIENT_SECRET,
        redirect_uri: env.KICK_REDIRECT_URI,
        grant_type: "authorization_code",
        code_verifier: verifier
      });
      const tokenResponse = await fetch(KICK_TOKEN, {
        method: "POST",
        headers: { "content-type": "application/x-www-form-urlencoded" },
        body
      });
      if (!tokenResponse.ok) return new Response(await tokenResponse.text(), { status: 502 });

      const token = await tokenResponse.json() as Record<string, unknown>;
      const exchangeCode = randomCode();
      await env.OAUTH_SESSIONS.put(`token:${exchangeCode}`, JSON.stringify(token), { expirationTtl: 90 });

      return Response.redirect(`kicklite://oauth/callback?code=${encodeURIComponent(exchangeCode)}&state=${encodeURIComponent(state)}`, 302);
    }

    if (url.pathname === "/oauth/exchange" && request.method === "POST") {
      const payload = await request.json().catch(() => null) as { code?: string; state?: string } | null;
      if (!payload?.code || !payload.state) return json({ error: "code and state are required" }, 400);
      const raw = await env.OAUTH_SESSIONS.get(`token:${payload.code}`);
      if (!raw) return json({ error: "code expired or already used" }, 400);
      await env.OAUTH_SESSIONS.delete(`token:${payload.code}`);
      return json(JSON.parse(raw));
    }

    if (url.pathname === "/oauth/revoke" && request.method === "POST") {
      const auth = request.headers.get("authorization");
      if (!auth) return json({ error: "authorization required" }, 401);
      const token = auth.replace(/^Bearer\s+/i, "");
      const revokeUrl = new URL("https://id.kick.com/oauth/revoke");
      revokeUrl.searchParams.set("token", token);
      revokeUrl.searchParams.set("token_hint_type", "access_token");
      const response = await fetch(revokeUrl, { method: "POST" });
      return new Response(await response.text(), { status: response.status, headers: { "cache-control": "no-store" } });
    }

    if (url.pathname.startsWith("/oauth/pkce") && request.method === "POST") {
      const payload = await request.json().catch(() => null) as { state?: string; code_verifier?: string } | null;
      if (!payload?.state || !payload.code_verifier) return json({ error: "state and code_verifier are required" }, 400);
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
      const response = await fetch(upstream, { method: request.method, headers, body: request.method === "GET" || request.method === "HEAD" ? undefined : request.body });
      const out = new Response(response.body, response);
      out.headers.set("cache-control", "no-store");
      return out;
    }

    return json({ error: "not found" }, 404);
  }
};
