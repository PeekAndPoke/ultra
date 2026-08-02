# Frontend token storage — bearer stays; the cookie route was designed and dropped

**Status:** **Increment 1 COMPLETE** (the response contract) — not yet through `/feature-review`, which
CLAUDE.md requires before DONE. **Increment 2 (the cookie transport) DROPPED 2026-08-02** — see below;
the reasoning is kept so it is not re-proposed. Security-critical.
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

## DROPPED (maintainer, 2026-08-02): no cookie transport

The `httpOnly` cookie was designed in full and then dropped. **Bearer + `localStorage` stays.** What
killed it, in the order the objections arrived:

**1. Cookies are host-scoped, not app-scoped.** Three frontends against one API host means every cookie
is attached to every request from every app. Per-realm cookie *names* do not disambiguate — only the auth
routes carry `{realm}` in their path, so there is nothing to select on for `InsightsApi` or app routes.

**2. A client-sent realm header does not fix it.** `X-Funktor-Realm` selects correctly but does not close
the escalation it appears to: an XSS on the b2b frontend sets it to `ops`, the browser attaches the ops
cookie, the token really is an ops token, header and token agree. Both sides of the comparison are things
the attacker legitimately obtained.

**3. An `Origin` check would close that** — `Origin` is a forbidden header name, so JS cannot forge it,
and scoping the check to cookie-authenticated requests costs machine clients nothing. **But it breaks the
product.** b2b2c frontends run on customer-controlled custom domains; `shop.customer.com` can never map
to a realm in any config we own.

**4. And custom domains are the killer anyway.** A custom domain is a different *site*, not just a
different origin, so `SameSite=Lax` would block the cookie outright and force `SameSite=None` — throwing
away the strongest layer precisely where it was wanted. Plus credentialed CORS forbids wildcards, so
every customer domain would need a dynamic allowlist, each becoming a trusted origin.

### What the two designs actually protect against — they are NOT the same

- **`localStorage` + bearer** — exposed to token *exfiltration* via XSS: read it, replay it off-machine
  until expiry. **Not exposed to CSRF at all**, because there is no ambient credential; an attacker page
  cannot set `Authorization` cross-origin. A CSRF header here would protect against nothing.
- **`httpOnly` cookie** — not exposed to exfiltration. Exposed to CSRF, and to session-riding, which
  nothing fixes.

Both need XSS to start. The delta is narrow: whether the attacker can use the session after leaving the
page. See the Framing section above — storage bounds the blast radius, **CSP + Trusted Types is the
actual mitigation**, and that is still owed regardless.

**Obfuscating the token in `localStorage` was considered and rejected** (maintainer): the key ships in
the bundle, so it is a speed bump measured in minutes.

### What is left open, and where

- **Real logout.** Neither transport gives it. `logout()` drops local state and the JWT stays valid until
  `exp`; clearing a cookie would not have invalidated it either. The fix is
  `.claude/tasks/20260728-session-revocation-wiring.md` — storage half built, unwired — and it is
  transport-independent.
- **Content-Type enforcement.** Split out as its own task: it is a live hole today, not a cookie concern.
- **CSP + Trusted Types.** Still the real XSS mitigation, still unowned.
- **Shorter token TTL.** Narrows the replay window; currently 1h in the demo realms.

### The door is left open, cheaply

`AuthSignInResponse.Session` stays **sealed with a single `Bearer` variant**. That keeps the discriminator
in the wire format and in the generated TypeScript, so adding a transport later is additive rather than a
breaking reshape of every client. Cookie mode would still be viable for fixed-origin first-party surfaces
(ops/admin), where `SameSite=Lax` works properly — transport is a per-session choice, not a server mode.

### Transport is chosen per session, not per deployment (settled, and still true)

Register both providers; the client says which it wants at sign-in. A browser asks for one thing, a
mobile app, CLI or another server uses bearer or an API key. Relevant now only as the reason a future
cookie mode would not need a deployment fork.

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

## Verification matrix — what is still owed

The cookie rows are gone with the design. What remains is transport-independent:

- [ ] **CSP with nonces, and Trusted Types enforced** — the actual XSS mitigation, and the root cause for
      either storage design. Smoke-test that inline script execution is blocked on each SPA.
- [ ] **Session revocation makes logout real** — `.claude/tasks/20260728-session-revocation-wiring.md`.
      Until then `logout()` drops local state and the token stays valid until `exp`.
- [ ] Consider a shorter access-token TTL (currently 1h in the demo realms) to narrow the replay window.
- [ ] `Content-Type` enforcement on REST routes — split out, see cross-references.

## Cross-references

- `.claude/tasks/20260731-sdk-auth-integration.md` §4.1 — the provisional localStorage decision this
  supersedes.
- `.claude/tasks/20260731-auth-module-for-sdk.md` §1 — the four open questions this answers.
- `.claude/tasks/20260731-sdk-sse-auth.md` — SSE cannot set headers, so cookie mode helps there; it also
  needs `credentials: 'include'` on the stream fetch.
- `.claude/tasks/20260728-session-revocation-wiring.md` — **the only real logout mechanism**, and
  transport-independent. This task cannot deliver logout; that one can.
- `.claude/tasks/20260802-rest-content-type-enforcement.md` — split out of the dropped cookie work;
  a live hole today, not a cookie concern.
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
