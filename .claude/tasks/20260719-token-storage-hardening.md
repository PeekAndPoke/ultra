# Frontend token storage hardening (localStorage → httpOnly session cookie)

**Status:** DESIGN SETTLED 2026-08-02. **Increment 1 COMPLETE** — not yet through `/feature-review`,
which CLAUDE.md requires before DONE. Increment 2 (the cookie transport) not started, and needs its own
plan: it moves a security boundary. Security-critical.
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

## Increment 1 — the response contract (COMPLETE 2026-08-02)

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
    val expiresAt: MpInstant? = null,   // nullable -- see below
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

### A Slumber trap, corrected by the codegen agent (2026-08-02)

I described the `_type: z.literal('token')` on the old nested `Token` as a **spurious** artifact of the
generator mirroring `isPolymorphicChild`'s "carries `@SerialName`" branch. **That was wrong, and acting
on it would have broken sign-in.**

Slumber genuinely writes that key: `createChildSlumberer` falls back to `getParent(cls) ?: cls`
(`builtin/polymorphism/Polymorphic.kt:36`), takes the default `_type`, and
`PolymorphicChildSlumberer.slumber` does `result.plus(disc2ident)`
(`PolymorphicChildSlumberer.kt:30`). The generator was mirroring real behaviour, correctly.

**The rule that carries forward: any standalone class carrying `@SerialName` silently gains a `_type` on
the wire, and awaking ignores it.** Documented as a `TODO(scan)` at `Polymorphic.kt:55-61`. The new
`Session.Bearer` / `Session.Cookie` are unaffected — they really do implement a sealed parent, so their
discriminator is genuine.

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

### What shipped

| | commit |
|---|---|
| `JwtPayload.expiresAt` + `JwtClaim.asLong`, and a 1-in-4 flaky test fixed | `a0bef940` |
| The response reshape, `generateJwt` -> String, the client decoder deleted | see log |

553 tests green across `funktor:auth` (jvm+js), `funktor:all`, `funktor-demo:server`, `ultra:security`.
Compile sweep clean on jvm and js.

Two things found while building, both recorded so they are not re-derived:

- **`expiresAt` had to be nullable.** `exp` is optional in RFC 7519, the verifier treats an absent one as
  "no expiry check" (pinned by `JwtWireCompatSpec`), and each realm supplies its own claim lambda. The
  plan said non-null; that would have been a lie.
- **Deleting `JwtClaimsSpec` emptied `funktor:auth`'s only JS test source set.**
  `AuthStateSessionMappingSpec` replaces it and pins the property that matters: a malformed or empty
  bearer token changes nothing, because nothing reads it.

Four mutants, all killed — including one that survived at first: **nothing asserted `userId`.**
`FunktorApiSpec` now exposes the test user ids and `AuthApiSpec` asserts a refresh returns a token for the
SAME user, which nothing checked before.

## Increment 2 — the cookie transport (DESIGN SETTLED 2026-08-02)

### Deployment: one API host per realm

`api-ops.klang.art`, `api-b2b.klang.art`, … one per realm, each serving one frontend subdomain.

This is what makes the rest simple, and it replaced two designs that did not survive scrutiny:

- **Per-realm cookie NAMES on one shared API host.** Proposed first, and wrong. Cookies are scoped to a
  *host*, not an app, so all three would be attached to every request from every frontend, with nothing
  to select on — only the auth routes carry `{realm}` in their path; `InsightsApi` and app routes do not.
- **An `X-Funktor-Realm` header to select among them**, with the token's realm compared against it. It
  selects correctly, but does not close the escalation it appears to: an XSS on the b2b frontend sets the
  header to `ops`, the browser attaches the ops cookie, the token really is an ops token, header and
  token agree. Both sides of the comparison are things the attacker legitimately obtained.
- **An `Origin` check** would close that — `Origin` is a forbidden header name, so JS cannot forge it —
  but it is a dead end for any non-browser client, which sends none. Rejected on those grounds.

Separate hosts dissolve all of it: isolation, cookie selection, logout scoping, and multi-realm
sign-in (you can be signed into all three frontends at once, as today).

### The cookie

```
Set-Cookie: __Host-session=<jwt>; HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=<exp - now>
```

Plain `__Host-session` on every API host — **per-realm names are unnecessary** once the host boundary
does the isolating.

`__Host-` is not decoration. The browser REJECTS such a cookie if it carries a `Domain`, has a `Path`
other than `/`, or lacks `Secure`, which makes it host-only by construction and closes cookie tossing: a
sibling subdomain cannot set `Domain=.klang.art` under this name and have it delivered to the API
alongside the real one (session fixation).

**Keep the mental model straight:** host-only restricts where the cookie is SENT, not who can CAUSE it to
be sent. A page on any same-site subdomain can still make a credentialed request to the API host. That is
what the next two items are for.

### CSRF: three layers, and CORS is not one of them

**CORS does not prevent CSRF.** It governs whether the *response* is readable, not whether the request is
sent. A "simple request" — POST with `text/plain`, `form-urlencoded` or `multipart` — is delivered and
executed with no preflight; the attacker simply cannot read the reply. The side effect already happened.

1. **`SameSite=Lax`** — blocks true cross-site entirely. The cookie is not attached at all. Free, and the
   strongest layer. Does NOT cover same-*site* siblings.
2. **Enforce `Content-Type: application/json` on REST routes** — the layer that actually closes the
   same-site gap, by making every request non-simple and therefore preflighted, at which point the CORS
   allowlist genuinely is the gate. **This is a real hole today, independent of cookies**: `routing.kt:205,234`
   does `call.receive<ByteArray>()` and parses it as JSON with no Content-Type check anywhere, so a
   cross-origin `<form enctype="text/plain">` POST reaches a handler right now.
   - Check for form-encoded / multipart routes first — file upload is the usual exception.
3. **`X-Funktor-Csrf: 1`, a constant** — defence in depth (maintainer, 2026-08-02). Set automatically by
   `authTransport` in cookie mode. **The value is not a secret and must never become one**: the protection
   is that a cross-origin page cannot set a custom header without a preflight the server refuses. Say so
   in its KDoc, or someone will later "harden" it into a token and add machinery for nothing.

**Send it in both modes, require it only for cookie-authenticated requests.** Mandatory-for-bearer breaks
every non-browser client — mobile, CLI, server-to-server — that legitimately sends nothing but
`Authorization: Bearer`, and those have no ambient credential and so no CSRF exposure.

Still open: `SameSite=Lax` attaches the cookie to top-level GET navigations, so a state-changing GET
remains CSRF-able. REST hygiene says there are none; confirm rather than assume.

### Server pieces

- `FunktorRestBuilder.jwtCookie(...)` alongside the three `jwt()` overloads
  (`funktor/rest/src/jvmMain/kotlin/index_jvm.kt:96-121`), cookie attributes configurable.
- A cookie `AuthenticationProvider` following `AnonymousAuthenticationProvider`
  (`funktor/rest/src/jvmMain/kotlin/auth/anonymous.kt:16`) — no Ktor `session()` machinery needed. Follow
  the fail-soft convention: an unusable cookie falls through to anonymous, and the 401 comes from the
  auth floor (`auth/call.kt:20-26`).
- **A transport field on `Caller.JwtCaller`** (`auth/Caller.kt:28`) so the CSRF check fires only for
  cookie-authenticated requests.
- `POST /auth/{realm}/logout` — `public()` floor and idempotent, since clearing an absent cookie must
  still answer 200. Realm-scoped for consistency with every other auth route; with one API host per realm
  the realm is implied anyway.
- An explicit **`realm` claim** on the token. Today `type` (user type) stands in for it — that is what
  `refreshToken` validates via `expectedUserType` — but they coincide only because each demo realm happens
  to have one user type. Additive.

### Client — already done by the codegen agent, nothing owed

`HttpRequest.credentials` and `SseOptions.credentials` exist (`c7faa088`), and `authTransport` already
handles both modes (`c0264522`): `Authorization` for bearer, `credentials: 'include'` and no header for
cookie. Boot hydration in cookie mode calls `refreshToken`, which behaves identically under cookie auth —
it sits behind `AuthUserApi`'s `authenticated()` floor and reads whichever provider authenticated.

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
