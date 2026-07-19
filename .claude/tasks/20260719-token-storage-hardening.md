# Frontend token storage hardening (localStorage → memory + httpOnly refresh cookie)

**Status:** DESIGN GAP flagged, TO REVISIT (2026-07-19). Security-critical. Collected — not executed.
**Test bed:** the three-realm `funktor-demo` (operators / b2b / b2b2c) once the auth flows are wired.

## The gap (current behaviour)

- `AuthState` persists the **whole** `Data<USER>` — JWT `token`, `tokenExpires`, `tokenUserId`, and
  the deserialized user — into `localStorage` under key `"auth"`
  (`funktor/auth/src/jsMain/kotlin/AuthState.kt:91`, via `persistInLocalStorage`).
- localStorage is readable by **any** JS on the origin ⇒ an XSS can exfiltrate the JWT and replay it
  from the attacker's machine, indefinitely until expiry.
- Refresh infra already exists: `startSessionLifecycle` + `api.refreshToken()` on a timer /
  window-focus (`AuthState.kt:258–305`). The bearer token is now sent via Ktor `defaultRequest`
  (see `20260719-ktor-client-unification.md`).

## Framing (keep straight when we revisit)

- Storage hardening stops **exfiltration / offline replay**, NOT **session-riding** — an XSS still
  runs on the page and can call the API as the user regardless of where the token lives.
- Root cause is XSS. Storage choice only bounds the blast radius. **CSP + Trusted Types is the actual
  XSS mitigation** and must happen regardless of the storage decision.

## Architecture constraint that drives the design

The API is a **separate subdomain** (`api.funktor-demo.localhost`) from the three frontends
(`ops.` / `b2b.` / `b2b2c.`). Subdomains of `funktor-demo.localhost` are the **same site**, so a
`SameSite=Lax/Strict` cookie scoped to `.funktor-demo.localhost` IS sent on `ops.*→api.*` XHR and IS
CSRF-resistant against third-party sites (no `SameSite=None` needed). Catches: needs CORS
`Access-Control-Allow-Credentials: true` + `credentials: 'include'`; a parent-domain cookie is
auto-attached to ALL subdomains, muddying realm isolation (mitigate with per-realm cookie
names/paths, or host-only cookies set by each API host). A BFF sidesteps all of this by making auth
same-origin per app.

## Recommended phased approach

1. **Now-ish (highest value/effort): access token in memory + refresh token in httpOnly cookie.**
   - Long-lived refresh token → `HttpOnly; Secure; SameSite=Lax` cookie set by the API (unreadable
     by JS). Access token → memory only, NOT persisted. New tab silently bootstraps via the refresh
     endpoint ⇒ **multi-tab UX preserved**. XSS can read only the short-lived access token.
   - Pair with: short access-token TTL (5–15 min), refresh **rotation + reuse-detection**,
     server-side **revocation** so logout / "log out all devices" truly invalidate.
2. **In parallel, regardless of storage:** strict **CSP** (nonce/hash, no `unsafe-inline`),
   **Trusted Types**, SRI on external scripts, dependency hygiene — across all three SPAs.
3. **North star (later phase): BFF (Backend-for-Frontend).** SPA never holds a token; a same-origin
   backend keeps it server-side and attaches it. Strongest, and dissolves the cross-subdomain cookie
   friction. Cost: an extra proxy tier per frontend.

Alternative if we choose to keep localStorage: OWASP **token sidejacking** mitigation (random
fingerprint in an httpOnly cookie, its SHA-256 embedded in the JWT, verified server-side) — a stolen
JWT is then useless without the cookie. Still allows local session-riding.

## Decisions to make when we revisit

- Refresh-cookie scope: parent `.funktor-demo.localhost` (one cookie, all subdomains) vs host-only
  per API host vs per-realm name/path — trade convenience against realm isolation.
- Access-token TTL + refresh rotation policy + reuse-detection response (revoke the whole family?).
- Where revocation state lives (server-side session store / refresh-token table) so logout is real.
- Do we commit to BFF as the end state now (shapes the three-app architecture) or keep #1 long-term?

## Security-test / verification matrix (write when built)

- [ ] Access token is NOT in localStorage/sessionStorage; only in memory (assert nothing sensitive
      persists across a hard reload except via the refresh cookie).
- [ ] Refresh cookie is `HttpOnly; Secure; SameSite` and not readable via `document.cookie`.
- [ ] New tab bootstraps a session from the refresh cookie without re-login (multi-tab preserved).
- [ ] Logout + "log out all devices" invalidates the refresh token server-side (subsequent refresh
      ⇒ 401).
- [ ] Refresh rotation: an old (already-used) refresh token ⇒ rejected + family revoked
      (reuse-detection).
- [ ] Cross-origin credentialed request works only for allowed origins (CORS allow-credentials +
      explicit origin, never `*`).
- [ ] CSP blocks inline script execution (Trusted Types enforced) — smoke test on each SPA.

## Cross-references

- `20260719-ktor-client-unification.md` (bearer now via Ktor `defaultRequest`; the transport this
  builds on).
- `20260719-cross-realm-authz-and-tests.md` (realm boundary — cookie scope interacts with realm
  isolation).
- `20260717-auth-orgs-foundation.md` (`refreshToken` guard, session lifecycle, Model C re-login).
- Backlog neighbours: 2FA (opt-in per org + app-wide override), new-device-login email.
