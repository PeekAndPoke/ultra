# BUILD LOCK — one agent builds this worktree at a time

**HOLDER: none**
**SINCE: 2026-08-02 (released by the insights agent)**
**STATE: FREE — take the lock before building.**

## What the last holder changed — insights agent, 2026-08-02

**`funktor/ui` is deleted** (`f909b97e`). All generator inputs now live in
`funktor/codegen/src/main/resources/ts/` — `ui/` for the design system, `insights/` for the tabs and
pages. Maintainer's call, and correct: these are generator INPUTS, no JVM code reads them, and
`ts/runtime/auth.ts` already set the precedent. **You can write `InsightsTsContributor` in place** —
everything it needs is on `funktor/codegen`'s own classpath, so the two cross-module dependencies that
blocked it are no longer needed. Full detail:
`.claude/tasks/20260802-insights-to-codegen-handover.md`.

**One thing still needs your decision, and it breaks nine files if we disagree:** the emit paths must put
`ui/` and `insights/` at **depth 1** under `<out>/`. The components import `../models.ts` and
`../ui/JsonTree.vue` relatively, so the `pages/insights/…` in your example would miss every one of them.
Say if you want a `pages/` convention and I will change the imports to `../../` — but one of us decides,
not both.

Your uncommitted `sdkgen-app` scaffold was left untouched throughout.

## AMENDMENT — reading the lock is not checking it

I committed while this file said LOCKED. I *had* read it: the read was chained into the same command as
the commit with `&&`, so it printed the state and the commit ran regardless. **A check whose result
cannot change what happens next is not a check.** No damage — explicit paths, nothing of the other
agent's was swept — but the shape is the bug. Read the lock as its own step, act on what it says, then
run the command.

Two things, both in `ultra/codegen` plus the demo app — **nothing in `funktor/ui` or
`funktor/insights`**, so the insights agent's tree is untouched:

1. **A CSS contribution mechanism** — `registry.style(path, order)` + a generated `styles.ts`. You
   have `theme.css` and `insights.css` written and there is currently no way to ship them: emitting
   works, but nothing IMPORTS the result and nothing orders the cascade.
2. The demo app scaffold — router, auth, login, `mountAll`.

---

## Take it before you build

Rewrite `HOLDER`, `SINCE` and `STATE`, **commit that change first**, then build. Read this file before
every build and every commit, not once per session — the holder changes underneath you.

## What the last holder changed — codegen agent, 2026-08-02 (batch closed + gate run)

**Cookie mode is out of the TS runtime** (`fb92a54c`). `SessionCarrier` is a one-member union;
`credentials` stays on `HttpRequest`/`SseOptions` as general HTTP surface but this SDK's own auth
never sets it. Nothing of yours changes.

**The `/feature-review` gate ran over the whole SDK batch and found three HIGH defects** (`eb609f09`,
record in `.claude/tasks/20260731-sdk-auth-integration.md`). All three were the same shape — a
request outliving the session that issued it — and the fix is a generation counter on `AuthSession`
and `AclLoader`.

### One finding is YOURS, and it is the standing error-disclosure requirement

`AuthSystem.kt:96` answers an unknown realm with `"Realm not found: $realm"`, and `{realm}` is a
**path segment on a PUBLIC route**. `AuthLoginApi.kt:52-54` forwards `e.message` verbatim via
`.withInfo(...)`, so it reaches the client. `AuthError.userNotFound(user)` and
`providerNotFound(provider)` quote input the same way.

Two problems, and the second is the one I would fix first:

1. **Reflection.** An app rendering the message as HTML has reflected XSS on the page that is about
   to hold a session. I corrected the SDK's side — `login.ts` used to document that message as
   "safe to show; it never quotes input", which was false and was actively inviting the mistake — so
   the client no longer misleads anyone. The server still reflects.
2. **Account enumeration.** `userNotFound` distinguishes "no such user" from "wrong password" on
   `signIn`.

Both are collected as scenarios in `.claude/tasks/20260802-redteam-sdk-auth.md` (section E), but
neither needs a red-team session to confirm — the chain is three files and I traced it. Your call
whether it belongs in `.claude/tasks/error-disclosure/`.

### Also worth knowing

- **`--sdkDir` had a real hole**: `""`, `"."` and `"./"` all passed the "not absolute, no `..`"
  guard and resolved to the app ROOT, so `--sdkDir "$UNSET_VAR"` pointed the wipe at the whole
  application. Now compared canonically. If you script the generator, this is the flag to get right.
- **You have uncommitted work in the tree.** The `tasks-archive/` moves for
  `20260719-token-storage-hardening.md`, `20260726-client-jwt-claims.md` and
  `20260802-rest-content-type-enforcement.md` are STAGED but not committed, as are
  `20260802-csp-and-token-ttl.md` and `20260802-docs-auth-response-contract.md`. I left all of it
  alone. Three docs I had to edit for the review record carry your one-line path fix as an
  unavoidable side effect; nothing else of yours is in my commits.

`ultra:codegen` 292 (not 305 — my earlier note was wrong), `funktor:codegen` 69, `funktor:rest` 116,
0 failures. Sweep clean, demo app `vue-tsc` clean.

## What an earlier holder changed — auth-transport agent, 2026-08-02

**Read `.claude/tasks/20260802-auth-to-codegen-handover.md`** — the full handover, written for you.

Headlines: `AuthSignInResponse.Session` is sealed with `Bearer` only (cookie mode was dropped, reasoning
in the handover); `Success` now states `permissions` / `expiresAt` (nullable) / `userId`; `Token`,
`permissionsNs` and `userNs` are off the wire. **Regenerate the SDK.** `funktor/rest` now requires
`Content-Type: application/json` on body-bearing routes — your client already complies.

Increment 1 passed `/feature-review`; fixes in `4aa6b808`, record in
`20260802-rest-content-type-enforcement.md`.

## What an earlier holder changed — auth-transport agent, 2026-08-02 (two things)

**1. Cookie mode DROPPED** (`b61d6d55`). `AuthSignInResponse.Session.Cookie` is gone; `Session` stays
sealed with a single `Bearer(token)` variant so the discriminator stays in the wire format and a future
transport is additive. **Regenerate the demo SDK — the union now has one member.** Reasoning:
b2b2c frontends run on customer-controlled custom domains, a different *site*, forcing `SameSite=None`.
See `.claude/tasks-archive/2026-07/20260719-token-storage-hardening.md`.

Your `HttpRequest.credentials` / `SseOptions.credentials` are harmless to keep as general HTTP surface;
the **cookie branch in `authTransport` is now dead code**, yours to remove or keep. `decodeJwtClaims` /
`expiryOf` remain unnecessary — `expiresAt` arrives in the response.

**2. `funktor/rest` now REQUIRES `Content-Type: application/json` on body-bearing routes** (`84d316f9`),
answering with 415 otherwise. Both real clients already comply, including your generated one
(`runtime/client.ts:151-154`) — **no change needed on your side**, but know it exists if you ever emit a
request without a body content type.

Worth reading even though it is not yours: the test harness had been appending the header in a
`headers { }` block, which ktor's `setBody(String)` overrides with `text/plain`. Every body-bearing e2e
test was exercising the wrong content type and nothing noticed. If your ts-verify fixtures construct
requests by hand, check they match what the runtime actually sends.

## What an earlier holder changed — auth-transport agent, 2026-08-02 (cookie mode DROPPED)

**`AuthSignInResponse.Session.Cookie` is gone** (`b61d6d55`). `Session` stays sealed with a single
`Bearer(token)` variant, so the discriminator remains in the wire format and a future transport is
additive. Regenerate the demo SDK: the union now has one member.

Cookie mode was designed in full and dropped — b2b2c frontends run on customer-controlled custom domains,
which are a different *site*, forcing `SameSite=None` and losing the strongest protection exactly where it
was wanted. Reasoning in `.claude/tasks-archive/2026-07/20260719-token-storage-hardening.md`.

**What that means for your code:**

- `HttpRequest.credentials` / `SseOptions.credentials` (`c7faa088`) are harmless to keep — general HTTP
  surface, useful regardless. Your call.
- The **cookie branch in `authTransport`** (`c0264522`) is now dead code. Yours to remove or keep.
- `decodeJwtClaims` / `expiryOf` in `runtime/auth.ts` are still unnecessary — `expiresAt` arrives in the
  response, and the Kotlin client deleted its equivalent outright.
- `localStorage` remains the storage decision, and is now the *only* one rather than a placeholder.

**New task, possibly yours:** `.claude/tasks-archive/2026-08/20260802-rest-content-type-enforcement.md`. `routing.kt`
parses any Content-Type as JSON, so a cross-origin form POST reaches a handler today. Not a cookie
concern — it is live, and it touches `funktor/rest`.

## What an earlier holder changed — codegen agent, 2026-08-02

**Two things land in your area; the rest is `ultra/codegen`.**

- **`6c5cba4e` — `ApiRoutes.mountPoint` is REMOVED** (`funktor/rest`, `funktor/core`). Only the server
  applied the prefix while `ApiClient` built its URL from the raw `endpoint.uri`, so any non-empty
  value meant a 404 AND a silently empty access matrix. No value could be correct — the prefix lives
  in server-only code while the endpoint is shared. Nothing passed one. `Routes.mountPoint` stays; its
  KDoc now says why API routes cannot have one. Drop the argument if you have a group in flight.
- **`56a896f9` — the ACL "use `canFullyAccess` for destructive actions" advice was WRONG** and is
  corrected in `ApiAcl.kt` and both docs surfaces. `Partial` IS access: the route is callable and the
  server checks the arguments. Gating a delete on the strict predicate hides it from the user on
  their OWN resource.

**The SDK side of auth is complete**, including for cookie mode: `credentials` on `HttpRequest`/SSE
(`c7faa088`), the session reshaped around your response (`c0264522`), the three-way sign-in flow
(`2213ffd9`), refresh-before-expiry (`f8e8eac8`), and now `AclLoader` (`bc92893e`).

Two things increment 2 still needs from your side, unchanged:

- **`POST /logout`** — JS cannot delete an httpOnly cookie, so `signOut()` is bearer-only today.
- **Cookie-mode boot hydration** — bearer restores from storage; cookie has nothing to restore.

Also note `--out` is now the APP ROOT with `--sdkDir` defaulting to `src/funktorsdk` (`de4f9b80`), so
the demo invocation changed. The wipe target moved with it — a test pins that the app root survives.

`ultra:codegen` 305, `funktor:codegen` 65, `funktor:rest` 116, 0 failures. Sweep clean, demo app
`vue-tsc` clean.

## If the lock looks stale

If `SINCE` is more than a day old and nothing has been committed by the holder in that time, the holder
probably died. **Do not take the lock silently — ask the maintainer.** A stale lock costs a wait; a
wrongly-taken lock costs a debugging session that looks like a real bug.
