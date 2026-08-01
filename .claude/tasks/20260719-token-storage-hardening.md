# Frontend token storage hardening (localStorage → httpOnly session cookie)

**Status:** DESIGN SETTLED 2026-08-02. Increment 1 (the response contract) IN PROGRESS.
Security-critical.
**Test bed:** the three-realm `funktor-demo` (operators / b2b / b2b2c).

## The gap (current behaviour)

- `AuthState` persists the **whole** `Data<USER>` — JWT `token`, `tokenExpires`, `tokenUserId`, and
  the deserialized user — into `localStorage` under key `"auth"`
  (`funktor/auth/src/jsMain/kotlin/AuthState.kt:91`, via `persistInLocalStorage`).
- localStorage is readable by **any** JS on the origin ⇒ an XSS can exfiltrate the JWT and replay it
  from the attacker's machine, indefinitely until expiry.
- Refresh infra already exists: `startSessionLifecycle` + `api.refreshToken()` on a timer /
  window-focus (`AuthState.kt:258–305`).

## Framing (keep straight)

- Storage hardening stops **exfiltration / offline replay**, NOT **session-riding** — an XSS still
  runs on the page and can call the API as the user regardless of where the token lives.
- Root cause is XSS. Storage choice only bounds the blast radius. **CSP + Trusted Types is the actual
  XSS mitigation** and must happen regardless of the storage decision. Out of scope here.

## Architecture constraint that drives the design

The API is a **separate subdomain** from the frontends — `api.funktor-demo.localhost` in the test bed,
`api.klang.art` in production. Subdomains of one registrable domain are the **same site**, so a
`SameSite=Lax` cookie IS sent on `app.* → api.*` XHR and IS CSRF-resistant against third-party sites
(no `SameSite=None` needed). Catches: needs CORS `Access-Control-Allow-Credentials: true` +
`credentials: 'include'`; a parent-domain cookie is auto-attached to ALL subdomains, muddying realm
isolation.

**Resolution:** host-only cookies set by the API host, via the `__Host-` prefix — the browser *enforces*
`Secure`, `Path=/` and no `Domain`, which closes cookie-tossing from a sibling subdomain for free. The
SPA never needs to read it: cookies attach based on the *request target*, not the page origin.

CORS is already correct in the test bed — `allowCredentials = true` with an explicit origin allowlist and
no wildcard (`funktor-demo/server/src/main/kotlin/server.kt:38-84`). Confirm it holds for real origins.

## SETTLED (maintainer, 2026-08-02)

**The session JWT itself goes in the httpOnly cookie.** Cookie mode's sign-in response carries no token
at all. One credential, one lifetime, no rotation machinery; multi-tab works because the cookie is simply
present.

**The earlier two-token design is DROPPED** — access token in memory + long-lived *refresh* token in the
cookie, with rotation and reuse-detection. It is recorded here only so it is not re-proposed: its headline
benefit was a short exposure window, and the Framing section above is why that was weaker than it read.
An XSS on the page can call refresh to mint fresh access tokens either way, so the two-token design bought
complexity — two lifetimes, rotation, family revocation — against a threat that storage does not address.

Three further decisions, same day:

| Decision | Choice |
|---|---|
| CSRF defence for cookie mode | **A required custom header.** A cross-origin page cannot set one without a preflight the server refuses. With `SameSite=Lax` and same-site deployment this is sufficient, needs no state and no second cookie |
| Sequencing | **Contract first, transport second.** See increments below |
| Logout | **Clears the cookie only** (`Max-Age=0`). The JWT stays valid until `exp`, exactly as today. Real revocation stays tracked in `.claude/tasks/20260728-session-revocation-wiring.md` |

## Increment 1 — the response contract (IN PROGRESS)

Ships **bearer-only**; nothing changes at runtime. It exists as its own step because the SDK work is
blocked on the contract (`.claude/tasks/20260731-sdk-auth-integration.md:121-123`).

`AuthState.readJwt` (`AuthState.kt:445-487`) currently decodes **four** things out of the JWT, and
`httpOnly` kills all four: permissions (via `permissionsNs`), `exp` → the auto-refresh timer, `sub` →
`tokenUserId`, and the raw claim map. Losing `exp` is the sharp one — the session lifecycle stops
scheduling refreshes and it presents as "randomly logged out", not as an error.

So the response carries the first three, **in both modes**:

```kotlin
data class Success(
    val session: Session,              // sealed: Bearer(token) | Cookie
    val permissions: UserPermissions,
    val expiresAt: MpInstant,
    val userId: UserId?,
    val realm: AuthRealmModel,
    val user: JsonObject,
    val org: AuthOrgRef? = null,
)
```

`permissionsNs` and `userNs` leave the wire; the nested `Token` class is deleted.

**The outcome the original design did not anticipate:** once the response carries these for *both*
transports, the fourth item turns out to have no consumers, and **the client-side JWT decoder can be
deleted outright** rather than kept alive for bearer mode. `funktor/auth/src/jsMain/kotlin/jwtClaims.kt`
and its spec (175 lines) go, and the client stops caring which transport is in use. Today it parses an
unverified token to learn things the server already knew and could simply have said.

That also retires the "these claims are user-editable, display-only" caveat carried at
`AuthState.kt:113-121` — permissions now arrive from the server over TLS rather than out of a blob the
user can rewrite in devtools. Still not an authorization decision; the server remains the only authority.

### Correction found while implementing (2026-08-02)

The plan said "do not change the abstract `generateJwt` signature — that is the whole blast radius".
Half right. `AuthRealm.generateJwt` **returns** `AuthSignInResponse.Token`, so deleting that type forces
the signature to change regardless: it becomes `String`. Six implementations, two lines each — four in
`funktor-demo` (`b2b`, `b2b2c`, `admin`, `operator`) plus two test realms.

What still stands, and is the part that mattered: **do not thread permissions/expiry through it.** Those
are derived in `successFor` from the freshly minted token, so the response and the token cannot disagree.

Consequence for sequencing: steps 2-4 are **one indivisible unit**. The model, `successFor`, the six
realms and `AuthState` all cascade from deleting `Token`, so the tree does not compile in between. Do not
start it without room to finish — see the lock rule about not going idle on a non-compiling module.

## Increment 2 — the cookie transport

- `FunktorRestBuilder.jwtCookie(...)` alongside the three `jwt()` overloads
  (`funktor/rest/src/jvmMain/kotlin/index_jvm.kt:96-121`), with the cookie's attributes configurable.
- A cookie `AuthenticationProvider` following the `AnonymousAuthenticationProvider` pattern
  (`funktor/rest/src/jvmMain/kotlin/auth/anonymous.kt:16`) — no Ktor `session()` machinery needed.
- A **transport field on `Caller.JwtCaller`** (`auth/Caller.kt:28`) so the CSRF header check fires only
  for cookie-authenticated requests. A Bearer header is inherently CSRF-safe; gating on the source keeps
  the check off routes that never use a cookie.
- `POST /logout` sending `Max-Age=0` — there is no logout endpoint today at all
  (`AuthState.logout()` just drops local state).
- SDK: a `credentials` field on `HttpRequest` (`runtime/http.ts:13-19`). Cookie mode **cannot** be a
  transport decorator the way bearer is, because `fetchTransport` passes only
  `method/headers/body/signal`. Same for `sseStream` (`runtime/sse.ts:242-255`).

## Verification matrix

- [ ] Token is NOT in localStorage/sessionStorage in cookie mode; assert nothing sensitive survives a
      hard reload except via the cookie.
- [ ] The cookie is `__Host-`-prefixed, `HttpOnly; Secure; SameSite=Lax`, and not readable via
      `document.cookie`.
- [ ] A new tab bootstraps a session from the cookie without re-login (multi-tab preserved).
- [ ] Logout clears the cookie; a subsequent authenticated call is anonymous.
- [ ] A cookie-authenticated request WITHOUT the required custom header is rejected; the same request
      with it succeeds. Both directions, or the check proves nothing.
- [ ] A **bearer**-authenticated request without the custom header still succeeds — the CSRF check must
      be gated on transport, not applied globally.
- [ ] Cross-origin credentialed request works only for allowed origins (CORS allow-credentials +
      explicit origin, never `*`).
- [ ] CSP blocks inline script execution (Trusted Types enforced) — smoke test on each SPA. Independent
      of storage, still required.

## Cross-references

- `.claude/tasks/20260731-sdk-auth-integration.md` §4.1 — the provisional localStorage decision this
  supersedes.
- `.claude/tasks/20260731-auth-module-for-sdk.md` §1 — the four open questions this answers.
- `.claude/tasks/20260731-sdk-sse-auth.md` — SSE cannot set headers, so cookie mode helps there; it also
  needs `credentials: 'include'` on the stream fetch.
- `.claude/tasks/20260728-session-revocation-wiring.md` — real revocation, deliberately not bundled.
- `20260719-ktor-client-unification.md`, `20260719-cross-realm-authz-and-tests.md`,
  `20260717-auth-orgs-foundation.md`.

## 2026-08-02 — this has TWO call sites, and they must be fixed together

The TypeScript SDK's `runtime/auth.ts` deliberately mirrors `AuthState`'s `localStorage` behaviour
(maintainer decision, `.claude/tasks/20260731-sdk-auth-integration.md` §4.1). The reasoning was that two
clients with different storage strategies would split the threat model and force the fix to be designed
twice.

So this lands in both:

- `funktor/auth/src/jsMain/kotlin/AuthState.kt` — the Kraft client
- `ultra/codegen/src/main/resources/ts/runtime/auth.ts` — the generated SDK

Both take storage as an injected strategy, so the change is the DEFAULT in two places. Fixing only one is
worse than fixing neither: it makes the remaining one look intentional.
