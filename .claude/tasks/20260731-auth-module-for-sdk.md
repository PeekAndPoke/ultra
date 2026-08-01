# Auth module — what it must provide for the generated TypeScript SDK

**Status:** SEED — written by the codegen agent for whoever owns `funktor/auth`. Complete it; do not
assume it is finished.
**Counterpart:** `.claude/tasks/20260731-sdk-auth-integration.md` (the SDK side, same day).
**Security-critical:** yes — sessions, tokens, JWT claims. Red-team follow-up required.

## Read this first: what you do NOT have to do

I checked before writing, so you do not have to.

**The API surface is already complete.** `AuthApiFeature` generates a TypeScript client with all
twelve endpoints, one-for-one with the Kotlin `AuthApiClient`:

```
getRealm · signIn · selectOrg · signUp · activateAccount · resendActivation
recoverAccountInitPasswordReset · recoverAccountValidatePasswordResetToken
recoverAccountSetPasswordWithToken · setPassword · refreshToken · getMyApiAccess
```

Generated from the running demo, `funktor-demo/sdkgen-app/src/funktorsdk/authClient.ts`. **No new
endpoint is needed to drive any flow `AuthState` implements.**

**The models reach TypeScript correctly too.** `AuthRealmModel`, `PasswordPolicy`,
`AuthSignInResponse` and friends are all in the generated `models.ts` with real zod schemas.
`AuthState.getPasswordPolicy()` reads `realm.passwordPolicy` (`AuthState.kt:165`), and
`PasswordPolicy` is on `AuthRealmModel` (`model/AuthRealmModel.kt:9`) — so it is already on the wire.
Nothing needs claiming.

So this is **not** an "expose more API" task. It is narrower, and mostly about one contract.

## 1. The one that actually matters: the token-storage contract

`.claude/tasks/20260719-token-storage-hardening.md` is filed, security-critical, and still open:
`AuthState` persists the whole session — JWT included — to `localStorage`, so any XSS on the origin
exfiltrates the token and replays it off-machine until expiry.

**The TypeScript client is being written now.** If the hardening lands later, it is written twice; if
the intended contract is known now, it is written once. So:

**ANSWERED 2026-08-02.** Design settled in `.claude/tasks/20260719-token-storage-hardening.md`; the
auth-transport agent is building it. Read that file for the whole picture — the short version:

- [x] **Is the cookie design happening?** Yes. And it is **not** the refresh-cookie design this section
      assumed: the **session JWT itself** goes in an `httpOnly` cookie. The two-token variant
      (access-in-memory + long-lived refresh cookie, with rotation and reuse-detection) was considered
      and **dropped** — storage never stops session-riding, so its short-exposure-window benefit was
      weaker than it read, and it bought two lifetimes plus rotation machinery against a threat it does
      not address.
- [x] **The server contract.** Sign-in and refresh return the same `AuthSignInResponse` as now, but
      `Success.token: Token` becomes `Success.session: Session`, a sealed `Bearer(token) | Cookie`.
      In cookie mode the response carries **no token at all**; `refreshToken` is called with no body and
      the browser attaches the cookie, and the server re-issues a `Set-Cookie`. Failure is unchanged.
      `Success` also gains `permissions`, `expiresAt` and `userId` — see below, this is the part that
      matters most to you.
- [x] **CORS** already correct in the demo — `allowCredentials = true`, explicit allowlist, no wildcard
      (`funktor-demo/server/src/main/kotlin/server.kt:38-84`). Still to confirm for real origins.
- [x] Not applicable — the answer was yes.

**What this changes for the SDK, concretely.** `Success` now carries `permissions`, `expiresAt` and
`userId` in **both** modes, so **no client needs to decode the JWT any more**. The Kotlin side is
deleting `jwtClaims.kt` and its spec outright (175 lines, verified zero consumers). The same applies to
`runtime/auth.ts`'s `decodeJwtClaims` / `expiryOf` — your file, your call, but it is the same deletion
and it is what makes the SDK transport-agnostic.

Note the earlier commitment recorded here — "storage is an injected strategy defaulting to in-memory,
never localStorage-by-default" — was **reversed** on 2026-08-02 in favour of matching Kotlin's
`localStorage` (see `20260731-sdk-auth-integration.md` §4.1). Keep it injected; the default stops being
the security boundary once the cookie lands.

## 2. Realm: bound at construction, or per call?

The Kotlin client binds it once — `AuthApiClient(realm: RealmId, config)` — and every route is
`/auth/{realm}/...`. The generated client cannot do that: it takes realm per call, because the
generator maps a path parameter to a parameter.

```ts
readonly signIn = (params: { realm: string }, body: AuthSignInRequest) => ...
```

- [ ] Decide whether that is fine (the SDK's auth wrapper can hold the realm and pass it), or whether
      the route shape should change. **Recommendation: leave the routes alone** — this is an ergonomic
      wrinkle the client wraps in one line, and changing a URL shape to suit one consumer is the wrong
      trade. But it is your call, and it is worth writing down either way.

## 3. SSE and auth are entangled — decide together

`.claude/tasks/20260731-sdk-sse-auth.md`: generated SSE clients bypass the SDK's transport, so a
transport-carried bearer token never reaches a stream. The maintainer's steer (2026-07-31) was to send
the JWT as a header and check it.

- [ ] Confirm SSE routes pass through the same auth floor as everything else — `sse.ts`'s own KDoc
      asserts they do, which is why `EventSource` was rejected (it cannot send `Authorization`).
      Verify rather than inherit the claim.
- [ ] If the token-storage work moves to a cookie, note that a cookie-authenticated stream is
      *simpler* than a header one — `fetch` sends cookies same-origin by default. That may change which
      option the SSE task picks, which is why these two want deciding together.

## 4. Anything `AuthState` does that the server cannot support

I found none, but I was reading from the outside. Check:

- [ ] `getMyApiAccess` / `UserApiAccessMatrix` — `AuthState` refreshes it via
      `AuthSessionConfig.onTokenRefreshed`. Is the matrix expected to change on refresh, or is that
      belt-and-braces?
- [ ] Session expiry: the client derives expiry from the JWT `exp` claim. Is there a server-side
      revocation path (`20260728-session-revocation-wiring.md`) the client should notice, and how
      would it find out — a 401 on the next call, or something better?

## 5. What to hand back

The SDK side is blocked on §1 and wants §3 decided alongside. Concretely:

- the refresh contract, or an explicit "not soon"
- a yes/no on the realm route shape
- confirmation that SSE routes are auth-gated, and which auth mechanism they will accept

Everything else can proceed without you.

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

**Red-team follow-up** (required — auth): `.claude/tasks/YYYYMMDD-redteam-auth-sdk-contract.md`
