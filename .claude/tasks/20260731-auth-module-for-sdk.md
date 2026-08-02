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

`.claude/tasks-archive/2026-07/20260719-token-storage-hardening.md` — filed, security-critical, and
**closed 2026-08-02** with the exposure accepted deliberately: `AuthState` persists the whole session —
JWT included — to `localStorage`, so any XSS on the origin exfiltrates the token and replays it
off-machine until expiry. Every alternative container cost more than it bought (below). The mitigations
that do address it are tracked in `.claude/tasks/20260802-csp-and-token-ttl.md`.

**The TypeScript client is being written now.** If the hardening lands later, it is written twice; if
the intended contract is known now, it is written once. So:

**ANSWERED 2026-08-02**, and the answer is *no*. Full reasoning in
`.claude/tasks-archive/2026-07/20260719-token-storage-hardening.md`.

- [x] **Is the cookie design happening?** **No — designed and dropped, same day.** b2b2c frontends run on
      customer-controlled custom domains, which are a different *site*, so the cookie would need
      `SameSite=None` — losing the strongest protection precisely where it was wanted. Credentialed CORS
      also forbids wildcards, so every customer domain would need a dynamic allowlist. Two earlier
      variants died first: per-realm cookie names (cookies are host-scoped, nothing to select on) and an
      `X-Funktor-Realm` header (attacker-controlled, so it does not close the escalation it appears to).
- [x] **The server contract.** Unchanged in transport terms: **`localStorage` + bearer stays.** What DID
      change is the response shape, and it is the part that matters to you — see below.
- [x] **CORS.** No longer load-bearing for auth: bearer needs no `allowCredentials`, and a custom-domain
      frontend needs only an allowlist entry.
- [x] **Say so explicitly.** Said: no cookie, not soon. Ship `localStorage` and do not pretend otherwise;
      the honest mitigations are CSP + Trusted Types, a shorter TTL, and session revocation.

**What this changes for the SDK.** `AuthSignInResponse.Success` now states `permissions`, `expiresAt` and
`userId`, and `Success.token: Token` became a sealed `Success.session: Session` carrying a single
`Bearer(token)` variant. So:

- **No client needs to decode the JWT.** The Kotlin side deleted `jwtClaims.kt` and its spec outright
  (175 lines, zero consumers). `runtime/auth.ts`'s `decodeJwtClaims` / `expiryOf` are unnecessary for the
  same reason — your file, your call.
- `AuthSession.signedIn`'s KDoc points at `AuthSignInResponseToken.token`; that type is gone, replaced by
  the `Bearer` variant of the `Session` union.
- `Session` stays **sealed with one variant** deliberately, so the discriminator is already in the wire
  format and adding a transport later is additive rather than another breaking reshape.

The earlier note here — "storage is an injected strategy defaulting to in-memory, never
localStorage-by-default" — was reversed on 2026-08-02 in favour of matching Kotlin. Keep it injected; it
is now the *only* storage decision rather than a placeholder for a cookie.

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
