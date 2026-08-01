# Ship auth through the SDK builder — the TypeScript counterpart of `AuthState`

**Status:** IN PROGRESS — the session runtime landed 2026-08-02. Login page + routing remain.
**Plan:** `.claude/tasks/20260729-ts-sdk-codegen.md` (the generator) and
`.claude/tasks/20260730-frontend-sdk-vue-contributors.md` (where contributors ship more than clients).
**Security-critical:** yes — sessions, tokens, JWT claims. Red-team follow-up required on completion.

## 1. Where this fits

**In the Vue-contributors family, and it should go FIRST there.**

Auth is the first contributor that ships *state and behaviour*, not just an API client. Everything the
frontend-SDK doc proposes — shared runtime modules, aggregation registries, components shipped from a
jar, a `requires(...)` check — is exercised by this one feature. Building it first turns that design
from a sketch into something with a user; building it after N other contributors means retrofitting.

It does **not** block, and is not blocked by, the remaining Phase-4 polish. Order does not matter
(maintainer, 2026-07-31).

## 2. What already exists, free

- **The auth API client is already generated.** The demo run emits `authClient.ts` from
  `AuthApiFeature` — `getRealm`, `signIn`, `selectOrg`, `signUp`, `activateAccount`,
  `resendActivation`, `setPassword`, the three `recoverAccount*` calls, `getMyApiAccess` and
  `refreshToken`. Nothing needs hand-writing at the API layer.
- **JWT injection is already the documented idiom.** `runtime/http.ts:50-58` says auth is a transport
  wrapper, and `SdkConfig` deliberately has no `headers` field so there is only one way to do it. The
  Kotlin side does exactly the same thing by another name: `AdminAppApis` installs a Ktor
  `defaultRequest` that reads a `tokenProvider()` closure
  (`funktor-demo/adminapp/src/jsMain/kotlin/api.kt:34-37`), wired to `State.auth().token?.token`.

So the gap is not the calls and not the header. **The gap is the session state machine.**

## 3. What `AuthState` actually does

`funktor/auth/src/jsMain/kotlin/AuthState.kt`, 488 lines. Read it before designing — this is a
summary, not a substitute.

| Concern | Detail |
|---|---|
| **Session data** | `Data<USER>` — token, realm, org, `tokenUserId`, `tokenExpires`, `claims`, the typed `user`, `permissions`. Reactive: `AuthState` *is* a `Stream<Data<USER>>` |
| **Lifecycle** | periodic expiry check, refresh before expiry, re-check on window focus, session-expired handling — all configurable via `AuthSessionConfig` (`checkIntervalMs`, `refreshBeforeExpiryMs`, `checkOnWindowFocus`, `onSessionExpired`, `onTokenRefreshed`) |
| **Flows** | login, logout, `selectOrg`, signup, activation, resend, the password-reset trio, `setPassword` |
| **JWT** | decodes claims client-side to derive `permissions`, `userId` (`sub`), expiry (`exp`) |
| **Routing** | `mount(builder)`, `routerMiddleWare(loginRoute)`, `redirectAfterLogin`, deep-link preservation |
| **Pending states** | `pendingOrgSelection`, `pendingActivation` — multi-step flows that survive a navigation |

## 4. Decisions needed before code

### 4.1 Storage — DECIDED 2026-08-02 (maintainer): localStorage, mirroring Kotlin

**Use `localStorage`, the same as `AuthState` does today.** This is a deliberate, eyes-open choice,
not an oversight, and the reasoning is worth keeping:

- The gap is real and filed — `.claude/tasks/20260719-token-storage-hardening.md`. Any script on the
  origin can read the JWT and replay it off-machine.
- But **two clients diverging makes the eventual fix harder, not safer.** A TypeScript SDK that is
  in-memory while Kotlin stays on `localStorage` gives a false sense of having solved something,
  splits the threat model, and means the real fix has to be designed twice.
- So: match the Kotlin behaviour now, and fix BOTH together once the strategy is decided.

**Implement it as an injected strategy anyway.** Not a hedge against the decision — the decision is
`localStorage` as the DEFAULT. Injecting costs nothing here and makes the agreed future fix a
one-line change per app instead of a rewrite, which is the whole point of fixing both at once.

    localStorageTokens()   // the default; mirrors AuthState
    inMemoryTokens()       // available, and what tests use

When the hardening task lands, it changes the default in two places and nothing else.

### 4.2 Framework-neutral core, thin Vue layer

`AuthState` is Kraft-coupled — it implements `Stream`, takes a `() -> Router`, and mounts routes. The
TypeScript version should split:

- `runtime/auth.ts` — the session store and lifecycle, depending on nothing but the generated auth
  client and a storage strategy. Testable in the node harness, usable from any framework.
- a Vue binding — `ref`/`computed` over the store, a router guard, `onScopeDispose` cleanup.

The alternative (one Vue-coupled class) is smaller today and wrong the first time anything else wants
it.

### 4.3 Generic over the user type

`AuthState<USER>` takes a `KSerializer<USER>`. The TypeScript equivalent takes a `z.ZodType<TUser>` —
which the generator already emits for whatever the app's user model is. This is the first real consumer
of generic emission outside `models.ts`.

### 4.4 Claims are NOT trustworthy, and the code must say so

`jwtClaims.kt:13` and `AuthState.kt:117` both warn that claims come from user-editable storage — a user
can hand-write `isSuperUser = true`. The client decodes them for UI decisions only; authorisation is
the server's. That warning must survive the port verbatim, at the point of use.

### 4.5 SSE intersects this

`.claude/tasks/20260731-sdk-sse-auth.md`: streams bypass the transport, so a transport-wrapper token
never reaches them. Whatever auth mechanism lands here must cover streams, or the two tasks must be
done together. Doing this one first without deciding that is how the asymmetry becomes permanent.

## 5. Spec (draft — do not implement before §4 is settled)

- [ ] `runtime/auth.ts`: session store, `AuthSessionConfig` equivalent, refresh lifecycle, injected
      storage strategy defaulting to memory.
- [ ] Transport wrapper helper, so the documented idiom is one call rather than a snippet to copy.
- [ ] Vue binding layer.
- [ ] `AuthTsContributor` shipping the runtime + bindings, conditional on the auth feature being reached.
- [ ] ts-verify: login → token attached → refresh-before-expiry → logout, against a stub transport.
- [ ] Red-team task on completion (`YYYYMMDD-redteam-sdk-auth.md`).

## 6. The other side — `.claude/tasks/20260731-auth-module-for-sdk.md`

Seeded for whoever owns `funktor/auth`. Two of the four questions above answered themselves while
writing it, so they are recorded there as **already done** rather than left as work:

- **`AuthApiFeature` IS sufficient.** All twelve endpoints generate, one-for-one with the Kotlin
  `AuthApiClient`. No new endpoint is needed for any flow `AuthState` implements.
- **The models already reach TypeScript.** `PasswordPolicy` is on `AuthRealmModel`, so
  `getPasswordPolicy()` has everything it needs; nothing needs claiming.

What genuinely blocks this task: **the token-storage contract** (§4.1) — whether the refresh-cookie
design is happening, because the client should be written against the intended contract rather than
the current one. Realm ergonomics and the SSE mechanism want deciding at the same time.

## Test evidence

- [ ] Unit/behaviour tests
- [ ] ts-verify checks executing the session lifecycle
- [ ] Full test command(s) run + green: `...`

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

**Red-team follow-up** (required — auth): `.claude/tasks/YYYYMMDD-redteam-sdk-auth.md`

## Progress — 2026-08-02

**Landed: `runtime/auth.ts`** — `TokenStorage` (`localStorageTokens` default, `inMemoryTokens`),
`decodeJwtClaims` / `expiryOf`, `AuthSession` (restore, subscribe, signedIn, signOut, isExpiring), and
`authTransport`. Shipped by `AuthTsContributor` in `funktor:codegen`.

**It lives in `ultra:codegen`, not beside the auth feature — a REVERSAL, and worth the reason.** The
plan said auth's TypeScript would ship from its own module's resources, which `out.resource` supports
for any contributor. But `ts-verify` lives in `ultra:codegen` and cannot see another module's
resources, so that placement would have shipped a session runtime that nothing type-checks or
executes. Nothing in the file is funktor-specific anyway — a bearer token, a storage strategy, a
transport wrapper; the funktor-specific part of login is the realm/provider flow, and that is
GENERATED. `AuthTsContributor` still owns the decision of WHEN it ships, and is the seam the login
page and its route registration attach to.

**`TsRuntime.emit` now plans with `out.shared`, not `out.file`.** Two contributors legitimately need
`runtime/http.ts` — the REST client and the auth session — and `file` is exclusive, so the second to
ask was a hard error. Content comes from one resource per module, so it is identical by construction
and dedupes. `out.sharedResource` is the new counterpart of `out.resource`; sharing stays opt-in.

**Verified:** 29 ts-verify checks, EXECUTED not just compiled — JWT decoding including multi-byte
UTF-8 and five malformed inputs, session lifecycle, restore-from-storage, expiry semantics, and the
transport wrapper. Mutation-tested 7/7: token captured at wrap time instead of per request; an
explicit `Authorization` header overwritten; `subscribe` not firing immediately; `signOut` not
clearing storage; `exp` unit wrong; a no-`exp` token treated as expiring; the UTF-8 round-trip
dropped.

One fixture bug found and fixed en route: the test's `fakeJwt` used `btoa(json)`, which encodes `ö`
as one latin1 byte rather than the UTF-8 pair a real JWT carries. The UTF-8 check failed against a
CORRECT decoder — a fixture bug wearing the costume of a code bug.

### RESOLVED 2026-08-02 — attaching the token to public routes is harmless

`authTransport` attaches to **every** request, public ones included, because the transport sees a
built URL rather than a route pattern. That was filed as a limitation; it is not one.

**An outdated token degrades to ANONYMOUS, it does not 401.** Verified end to end:

- `tryJwtCaller` returns `null` when verification fails — "signature, issuer, audience, or **expiry**"
  (`funktor/rest/src/jvmMain/kotlin/auth/jwtCaller.kt`).
- Ktor's default `FirstSuccessful` strategy then falls through to the next provider.
- `anonymous(AUTH_ANON)` is registered last and listed last in `authenticate(AUTH_JWT, AUTH_ANON)`
  (`funktor-demo/server/src/main/kotlin/api/ApiApp.kt:28-30,71`), and
  `AnonymousAuthenticationProvider` "always succeeds with `Caller.AnonymousCaller`".

So a stale token in `localStorage` makes `signIn` behave exactly as if none were sent: the caller is
anonymous, and a `public()` rule grants anonymous. **No route information needs plumbing into the
transport**, and the Kotlin client's always-attach behaviour is right for the same reason.

The one thing this does NOT excuse: a protected route with a stale token returns whatever the rule
chain decides for an anonymous caller — typically 401/403, not a token-expired signal. Refresh-before-
expiry is what avoids that, and it is still open below.

### Next

- Login page component + the aggregation registry (page routes, nav, `mountAll`).
- Refresh-before-expiry scheduling. `isExpiring` exists; nothing calls it yet. `AuthState` hooks
  window focus (`AuthState.kt:133`).
- Nothing yet runs against the real server. That is the natural next check.
