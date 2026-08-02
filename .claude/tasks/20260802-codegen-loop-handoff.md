# Loop handoff — TS SDK codegen, nightly run 2026-08-02

> ## ⛔ BEFORE ANY GRADLE COMMAND OR COMMIT: read `.claude/BUILD-LOCK.md`
>
> The auth-transport agent shares this worktree and is on its own nightly loop, so the holder flips
> back and forth. If `STATE: LOCKED` and you are not the holder: **no gradle, no commit, no staging.**
> Reading, grepping, planning and DRAFTING EDITS are always fine.
>
> Two rules learned the hard way, both non-obvious:
>
> - **Never leave a shared module non-compiling while you do not hold the lock.** The other agent's
>   sweep then reports "clean" for modules that never actually compiled. It cost them four missed call
>   sites once.
> - **Commit with `git commit -- <paths>`, never `git add … && git commit`.** The latter takes the
>   WHOLE index, including the other agent's staged work. Check `git diff --cached --name-status`
>   first. Both of us have swept up each other's files doing this.

## WHERE THIS STANDS — 2026-08-02, loop ended

**The SDK side of a login screen is COMPLETE.** Everything below shipped overnight, each piece
mutation-tested and verified against REAL generated output rather than fixtures:

| | |
|---|---|
| `credentials` on `HttpRequest` + SSE | `c7faa088` — unblocks their cookie transport |
| session, storage, both transport modes | `c0264522` |
| the three-way sign-in flow | `2213ffd9` |
| refresh-before-expiry | `f8e8eac8` |
| public-route metadata, `ApiAcl`, `mount.ts` | earlier the same night |

An app can write a login form against this today. **The only thing missing is markup.**

### Nothing unblocked remains. What is left, and who owns it:

| Item | Owner |
|---|---|
| The `.vue` verification decision — `vue-tsc` cannot run on TS 7, so it is a second toolchain, not a flag. But the CONSUMING app's `vue-tsc` already checks components, which may be enough | maintainer |
| `out.scaffold` — writes outside the generator-owned directory, which is a boundary change | maintainer |
| `POST /logout` — JS cannot delete an httpOnly cookie | auth agent, increment 2 |
| Cookie-mode boot hydration | auth agent, increment 2 |

The auth agent has been idle since their increment 1 (`880a3e2e`).

### If you restart this loop

Re-read this section first. If none of the four rows above has moved, there is nothing to do and the
loop will only burn tokens — that is why it was stopped rather than left spinning.

**Read this file FIRST each iteration. Do the next unchecked item. Update the note at the bottom
LAST.** Keep it cheap — it is read every loop.

## Standing authorization (maintainer)

| Question | Answer |
|---|---|
| Commit? | **Yes**, on `auth-increments`. Explicit paths only |
| Push / PRs / anything outward-facing | **NO.** Never while unattended |
| Scope | `ultra/codegen/**` and `funktor/codegen/**` are mine. See "hands off" below |
| Storage strategy | `localStorage` now, mirroring Kotlin; hardened in the auth agent's increment 2 |

## Hands off — the auth agent owns these tonight

`funktor/auth/**`, `ultra/security/**/jwt/**`, and these two task files (they are adding resolution
notes): `20260731-auth-module-for-sdk.md`, `20260731-sdk-auth-integration.md`.

They have committed to touching **no** `ultra/codegen/**` or `funktor/codegen/**`.

## What is landing from them — `.claude/tasks/20260719-token-storage-hardening.md`

Increment 1, bearer-only, nothing changes at runtime:

```kotlin
data class Success(
    val session: Session,          // SEALED: Bearer(token) | Cookie
    val permissions: UserPermissions,
    val expiresAt: MpInstant,
    val userId: UserId?,
    val realm: AuthRealmModel, val user: JsonObject, val org: AuthOrgRef? = null,
)
```

`Token`, `permissionsNs` and `userNs` are deleted. Increment 2 is the httpOnly cookie.

### VERIFIED, and it contradicts their handover — do NOT act on the other reading

Their note calls the emitted `_type: z.literal('token')` (models.ts:170) "spurious". **It is not. The
server really writes it.** Checked 2026-08-02:

- `isPolymorphicChild` is SLUMBER's own function and fires on a bare `@SerialName`
  (`Polymorphic.kt:62-66`); `TypeWalker.kt:530` mirrors it deliberately.
- Slumber dispatches on it (`BuiltInModule.kt:249`), and `createChildSlumberer` falls back to
  `getParent(cls) ?: cls`, takes the default `_type`, and `PolymorphicChildSlumberer.slumber` does
  `result.plus(disc2ident)` — it writes the key.

So the generator is faithful. **"Fixing" it to stop emitting `_type` would break sign-in.** Already
documented at `Polymorphic.kt:58-61`, which cross-references our mirror.

**The rule to carry:** any standalone class carrying `@SerialName` silently gains `_type` on the wire.
`Session.Bearer`/`Session.Cookie` under a sealed parent are genuine children, so they are fine.

## Backlog — in order

### 1. `credentials` on `HttpRequest` and `sseStream`

Increment 2 needs it and cannot proceed without it; it is in our files, purely additive, and breaks
nothing. `fetchTransport` passes only `method/headers/body/signal`
(`runtime/http.ts:13-19,62-77`), so cookie mode cannot be a transport decorator the way bearer is.
`sseStream` needs the same (`runtime/sse.ts:242-255`) — cookies are exactly where EventSource-style
auth helps.

- [x] **DONE** — `RequestCredentials`, `HttpRequest.credentials?`, pass-through in `fetchTransport`.
- [x] Same for `SseOptions` / `sseStream`, and it survives the generated `stream()` wrapper.
- [x] ts-verify, 6 checks: set / unset / the other enum value / through the wrapper. The unset case
      asserts the KEY IS ABSENT, not that it is `undefined` — a `RequestInit` consumer distinguishes
      them, and asserting the weaker thing would pass for a pass-through that does nothing.
- [x] Mutation-tested 5/5: pass-through removed (both files), spread unconditionally so `undefined`
      leaks into the init (both files), and `stream()` no longer forwarding caller options.

### 2. ~~`--check` cannot see a stale file~~ — ALREADY DONE. Item withdrawn 2026-08-02

**I wrote this item from a stale line in the vue-contributors plan and did not check the code first.**
`diffAgainst` already walks the directory and reports "on disk but not generated"; it landed in
`d760d9aa` and is tested at `TsSdkGenerateCliCommandSpec.kt:83`. Both docs corrected.

The manifest half is **moot and must not be built in a loop**: it was proposed so a run could delete
only its predecessor's paths, but the later decision is that the generator owns `<out>/` outright and
`writeTo` wholesale-replaces behind the `.funktor-sdk` marker, whose text already says "nothing you
add survives". Building it would soften a contract the maintainer deliberately made explicit.

**The lesson, which is the reusable part:** a backlog item is a CLAIM. Verify it against the code
before implementing it, exactly as you would a review finding.

### 3. `out.scaffold` / `out.requires` — BLOCKED on the maintainer, do not build in a loop

`scaffold` seeds an APP-OWNED file (a starting `vite.config.ts`) — which means writing OUTSIDE
`<out>/`. That is a change to the single most carefully-reasoned rule in the vue-contributors plan
("a boundary, not a mechanism": *under `<out>/` own it outright; anywhere else never write*).

The unresolved part is not the semantics, it is the base path: `--out` names the SDK directory, and
nothing tells the generator where the app root is. Options are a new flag, the parent of `--out`, or
keeping scaffolded files inside `<out>/` and exempting them from deletion — but the last contradicts
the marker's own text ("nothing you add survives") and would be confusing rather than safe.

`out.requires` (verify app-side wiring, fail with the lines to paste) has no such problem and could
be built alone, but it is only useful once the `@sdk` alias work starts.

### 4. An insights contributor — the registry's SECOND consumer

`TsSdkRegistry` has exactly one user today (a fixture). A second real one is what proves the
mechanism rather than assuming it. Page routes are `requiresAuth = true`, DECLARED not derived.

### 5. THE DTOs HAVE LANDED (2026-08-02) — execution plan below

**Shape as shipped** (`funktor/auth/src/commonMain/kotlin/model/AuthSignInResponse.kt`):

```kotlin
data class Success(
    val session: Session,          // sealed: Bearer(token) | Cookie   (Cookie is a `data object`)
    val permissions: UserPermissions,
    val expiresAt: MpInstant,
    val userId: UserId? = null,
    val realm: AuthRealmModel, val user: JsonObject, val org: AuthOrgRef? = null,
)
```

**Verify FIRST, before writing any TypeScript** — regenerate and read the output:

- `Session` must emit as its OWN `z.discriminatedUnion('_type', [...])` with `bearer` and `cookie`,
  not folded into the parent union.
- `Session.Cookie` is a **`data object`**, which is the unusual one. Slumber reaches it through
  `isPolymorphicChild` BEFORE the `objectInstance != null` branch (`BuiltInModule.kt:249` vs `:251`),
  so it slumbers via `DataClassSlumberer` to `{"_type":"cookie"}` — NOT through `ObjectInstanceCodec`,
  which has a known defect that drops everything. Confirm the walker agrees and emits an object with
  only the discriminator.
- `AuthSignInResponseToken` should be gone; `_type: z.literal('token')` with it.
- `expiresAt` is an `MpInstant`, so in TypeScript it is the claimed `{ts, timezone, human}` object —
  **`expiresAt.ts` is the epoch-millis number**, not `expiresAt` itself. `isExpiring` compares numbers.

**Then the runtime work:**

- [x] **DONE `c0264522`** — `decodeJwtClaims` / `expiryOf` deleted, mirroring their `jwtClaims.kt`.
- [x] **DONE `c0264522`** — `signedIn` takes the `Success` payload. In cookie mode there is no token,
      so a token-shaped API cannot express the state.
- [x] **DONE `c0264522`** — `TokenStorage` -> `SessionStorage`, persisting the whole payload.
- [x] **DONE `c0264522`** — `authTransport` attaches `Authorization` for bearer and
      `credentials: 'include'` for cookie, attaching no header.
- [x] **DONE `2213ffd9`** — `runtime/login.ts`, the three-way flow. Only `success` touches the session.
- [x] **DONE `f8e8eac8`** — `runtime/refresh.ts`, so something finally acts on `expiresAt`.
- [ ] **Boot hydration for COOKIE MODE ONLY — still open, and NOT mine to finish.** Bearer needs none:
      persisting the payload restores `expiresAt` and `permissions` across a reload. Cookie mode has
      nothing to restore, because the credential is the browser's, so that app must call
      `refreshToken` on boot. Blocked on the cookie transport existing at all (their increment 2).

## Rules that bite here (do not rediscover)

- **A fix without a failing-before test is not done.** Mutation-test every change: break it, confirm red.
- **A mutant that survives is not always a missing test.** It can mean the FIX was wrong, or the
  fixture cannot express the difference, or — twice now — that a comment CLAIMED something false.
  Check which before adding an assertion to make it die.
- **kotest ignores `--tests`.** Confirm a spec ran via `build/test-results/**/TEST-*.xml`; use the
  console to see WHICH case failed, because the XML mis-attributes that.
- **Module test tasks do not compile everything.** Before claiming a cross-module change is contained:
  `./gradlew compileKotlinJvm compileTestKotlinJvm compileKotlinJs compileTestKotlinJs compileKotlin
  compileTestKotlin --continue` and grep `^e:`.
- **A guard is only as good as the fixture that exercises it.** The barrel-collision CRITICAL shipped
  because the ts-verify fixture had no auth models, so the guard never had two sources for the name.
  Check the fixture covers the SHIPPING case, not merely a case.
- **ts-verify cannot see another module's resources.** Hand-written TS that ships from
  `funktor/codegen` is type-checked by nothing. That is why `runtime/auth.ts` lives in `ultra/codegen`.
- **Verify every claim before acting on it** — including one in a handover from the other agent, which
  is how the `_type` correction above was found.
- Explicit imports, no FQNs, KDoc `[refs]` must resolve, `Char(0xNN)` never `\uXXXX`.

## Needs the maintainer — do NOT decide these in the loop

- **The login page component needs a `.vue` verification decision.** `vue-tsc` cannot run on
  TypeScript 7, which ts-verify pins, so it is a second toolchain rather than a flag. Until that is
  settled, a shipped `.vue` is the one artifact nothing type-checks.
- Whether `refreshToken` is the right boot-hydration call or whether a dedicated `whoami` is wanted.

---

## Iteration notes

Append one short block per iteration. Newest at the top.

### Iteration 8 — 2026-08-02, `runtime/refresh.ts` — nothing was calling `isExpiring`

The payoff for the whole `expiresAt` contract. `AuthSession.isExpiring` existed and **nothing called
it**, so a session simply died: token expires, every later call 401s, user is "randomly logged out"
with no error naming the cause — the exact failure their plan calls the sharp one.

`startAutoRefresh(session, () => client.login.refreshToken({ realm }))` returns a stop function.
Timers and clock are INJECTABLE, so the tests execute the real scheduling logic without sleeping.

Decisions worth keeping:

- **A failed refresh does NOT sign the user out.** Whether to drop the session, redirect or show a
  banner is app policy, and guessing it here takes the decision away at the worst moment. `onFailed`
  reports; the session is untouched.
- **A thrown refresh is caught.** A flaky network is not a session decision; the next tick retries.
- **A session with no expiry is never refreshed**, and that is correct now in a way it was not for
  Kotlin. `AuthState` treats unknown expiry as STALE and clears the session (`AuthState.kt:138-144`)
  because it learned expiry by DECODING, so "no expiry" and "could not decode" were the same state and
  could never self-heal. The response states it now, so the ambiguity is gone.

**Mutation testing earned its keep again — 3 of 5 survived the first pass, all test gaps:**

| mutant | why it survived |
|---|---|
| `catch` removed | an unhandled rejection does not fail a check; now asserted via `process.on('unhandledRejection')` |
| in-flight guard dropped | every scenario fired the timer ONCE, so overlap never happened |
| `stopped = true` removed | clearing the timer alone already prevented further ticks; the flag only matters for a refresh resolving AFTER stop |

All three now killed, 5/5.

**And an existing spec caught real drift:** `TsRuntimeSpec` verifies that each module's declared
`requires` matches what it actually imports. I declared `Refresh -> setOf(Login)`; the file imports
from `login.ts`, `auth.ts` AND `apiResponse.ts`. The closure would have emitted them anyway, so
nothing would have broken — but `requires` states DIRECT imports and the spec holds it to that.

Demo app regenerated (22 files), `vue-tsc` clean.

### Iteration 7 — 2026-08-02, `runtime/login.ts` — the sign-in FLOW

Not on the backlog; added because it is the actual remaining gap for a login screen and it needs
NONE of the blocked decisions. The `.vue` question is about a VIEW; this is the part that must not be
got wrong, and it is plain TypeScript that `ts-verify` executes.

**Why it exists:** `AuthSignInResponse` has three branches and **two of them are not failures** —
`org-selection-required` and `activation-required` mean the credentials were right and there is a
defined next step. Treating either as a bad password is the obvious bug, it is silent, and every app
would otherwise re-derive it.

`completeSignIn(session, call)` returns a four-way `LoginOutcome`: `signed-in`, the two next-steps,
and `rejected` (which the server expresses as a non-2xx envelope, not as a variant). **Only `success`
touches the session** — the other two carry single-use tokens that grant one next step, and storing
them would make `isLoggedIn` true for a user who is not.

**Deliberately ships no view.** The framework's own rule is that it ships only non-customizable
things, and a login form's appearance is the first thing anyone changes.

Verified against REAL generated output: a probe compiles
`const x: SignInResult<UserPermissions> = wire` for the generated `AuthSignInResponse`, plus an
exhaustive `switch` over the outcome. 19 ts-verify checks. Demo app regenerated, `vue-tsc` clean.

**A mutant survived and found a real hole.** Removing the status check entirely still passed, because
every rejection test used `data: null` — so the null guard caught them and the STATUS check was never
exercised alone. A server answering 401 with a success-shaped body would have logged the user in.
Test added, mutant now dies. 4/4 after the fix.

### Iteration 6 — 2026-08-02, `runtime/auth.ts` reshaped around the new response

Item 5's runtime half is DONE for bearer AND cookie. The JWT decoder is gone.

- `AuthSession.signedIn` takes the **response payload**, not a token string. In cookie mode there is
  no token, so a token-shaped API literally cannot express the state.
- `TokenStorage` -> `SessionStorage`: it persists the whole payload. **This is what replaces boot
  hydration for bearer mode** — a reload restores `expiresAt` and `permissions` from storage, so the
  refresh timer still schedules. Without it, deleting the decoder reintroduces the exact
  "randomly logged out" failure their plan called the sharp one. Mutation MUT 3 pins it.
- `authTransport` attaches `Authorization` in bearer mode and sets `credentials: 'include'` in cookie
  mode, attaching NO header — JS cannot read the cookie, so there is nothing to attach.
- `AuthSession<P>` is generic over the permissions type, so an app passes the generated
  `UserPermissions` and gets full typing without this file importing anything generated.

**Verified against REAL generated output, not fixtures:** a probe compiles
`const x: SignedIn<UserPermissions> = success` where `success: AuthSignInResponseSuccess`, plus the
whole app wiring including `session.state().permissions?.roles`. If the runtime and the generator ever
drift, that stops compiling.

36 ts-verify checks, all executed. Mutation-tested 4/4: cookie credentials dropped; `restore()`
skipping its `_type` check so corrupt storage logs you in; only the token persisted so expiry dies on
reload; the token captured at wrap time instead of per request.

Demo app regenerated and `vue-tsc --noEmit` clean.

### STILL OPEN for cookie mode (not mine to finish)

- **Boot hydration is still required in COOKIE mode** and is NOT done. Bearer restores from storage;
  cookie has nothing to restore, because the credential is the browser's. That app must call
  `refreshToken` on boot — it returns the same payload `signedIn` takes. Left undone deliberately:
  it needs the endpoint to exist in cookie mode and a decision on realm configuration.
- `POST /logout` does not exist yet (their increment 2). `signOut()` currently clears local state
  only, which is right for bearer and INSUFFICIENT for cookie — JS cannot delete an httpOnly cookie.

### Iteration 5 — 2026-08-02, the new DTOs VERIFIED through the generator

Regenerated against the live API after their `5026e436`. Everything the plan said to check:

- `Session` emits as **its own** `z.discriminatedUnion('_type', [Bearer, Cookie])`, not folded in.
- `Session.Cookie`, the `data object`, emits `z.object({ _type: z.literal('cookie') })` — exactly as
  predicted from the `BuiltInModule.kt:249` vs `:251` dispatch order. It does NOT go through
  `ObjectInstanceCodec` and its everything-dropping defect.
- `AuthSignInResponseToken` and its `_type: z.literal('token')` are gone.
- `Success` carries `session`, `permissions`, `expiresAt`, `userId`.

**One thing differs from their handover, and the generator is right.** `expiresAt` emits as
`MpInstant.nullable().optional()`. The handover's snippet showed `val expiresAt: MpInstant`, but the
COMMITTED declaration is `MpInstant? = null` — they changed it before committing because `exp` is
optional in RFC 7519 and claiming non-null "would have been a lie" (their words). Checked the
committed source rather than the handover, which is the only reason this did not read as a bug.

**Consequence for us:** the field that exists specifically to drive refresh scheduling can be absent.
`AuthSession.isExpiring` already returns FALSE for unknown expiry — deliberately, so it does not
refresh forever — so the semantics were already right. It is now load-bearing rather than defensive.

Also note `expiresAt` is an `MpInstant`, so the number is **`expiresAt.ts`**, not `expiresAt`.

### Iteration 4 — 2026-08-02, the demo app regenerated and type-checked

Regenerated `funktor-demo/sdkgen-app/src/funktorsdk` in place and ran `vue-tsc --noEmit` over the
whole app. **Exit 0** — the maintainer's real Vue components compile against a freshly generated SDK,
including `publicRoute`, `runtime/auth.ts` and `mount.ts`.

Safe to redo, and worth redoing after any emitter change: the directory is gitignored
(`funktor-demo/sdkgen-app/.gitignore:7`) and marker-owned, so regeneration touches nothing tracked and
the generator refuses outright if the marker is missing. `vue-tsc` works here because the app pins
TypeScript 5.9.3 — it is only `ts-verify`, on TypeScript 7, where `vue-tsc` cannot run.

That closes the verification chain: live route graph → correct `publicRoute`/`route` → barrel compiles
with the real auth models → the consuming app compiles. Nothing in it was fixture-only.

**Still nothing for the maintainer to decide here.** The `.vue` question is unchanged: a component
shipped by a contributor is verified by the CONSUMING app's `vue-tsc`, not by `ts-verify`. That is
weaker than everything else in the SDK, but it is not nothing — and it may be enough, which is itself
worth putting to them rather than assuming a second toolchain is needed.

### Iteration 3 — 2026-08-02, the long-outstanding REAL-API check finally done

No new feature. Two backlog items removed by verification rather than implementation, and the
end-to-end check that had been deferred three times.

**Item 2 withdrawn — it was already done.** See the item. I had written it from a stale line in the
vue-contributors plan without checking the code. Both docs corrected.

**Item 3 blocked** — `scaffold` writes outside the owned directory, which is a boundary decision, not
a mechanism. Left for the maintainer rather than guessed.

**REAL-API REGENERATION — the thing "not yet verified" in three previous commit messages.** Local
DBs were already up, so `./gradlew :funktor-demo:server:run --args="--cli sdk:ts:generate --out <abs>"`
runs the whole generator against the live demo route graph. 20 files. Results:

- `getRealm` and `signIn` emit as **`publicRoute`**; `setPassword`, `refreshToken` and
  `getMyApiAccess` as **`route`** — in ONE merged `LoginApi`, importing both wrappers. The
  publicness derivation works against the real `funktor:auth` rule chains, not just fixtures.
- **The real SDK compiles through its own barrel** under `--strict --erasableSyntaxOnly`, WITH the
  real auth models present. That is the exact scenario the `ApiAccessLevel`/`AccessLevel` collision
  broke, verified against real output rather than a fixture for the first time.

Reusable: the generate CLI needs only `docker start mongodb arangodb` and takes ~4s. It is cheap
enough to run every iteration that touches the emitter, and it is the only check that sees the real
route graph. Generate into a scratch dir, not into `funktor-demo/sdkgen-app`.

### Iteration 2 — 2026-08-02, item 1 DONE and verified

Lock freed mid-iteration; took it, verified, mutation-tested, committed. **The blind draft was
correct** — all six checks passed first run, which I did not expect and did check rather than assume:
grepped the check names out of the run to confirm they EXECUTED rather than silently not running.

Mutation-tested 5/5. Note MUT 5 (`stream()` drops `...options`) is killed, but by
`generatedClient: threw — fetch failed` rather than by my own wrapper check — because `fetchImpl`
rides through the same spread, so breaking option forwarding breaks the stub injection first. The
mutant dies either way; the message just names a different symptom.

**Watch out — my own lock monitor lied to me.** It compares `STATE` only, so when I took the lock it
reported "lock re-taken by the other agent". Re-armed to compare HOLDER as well. If a future iteration
sees that message, check `HOLDER` before believing it.

Baseline: `:ultra:codegen:check` 280, `:funktor:codegen:check` 59, 0 failures, sweep clean.
The auth agent landed `JwtPayload.expiresAt` (`a0bef940`) upstream of us; sweep confirms no impact.

Next: item 2, `--check` cannot see a stale file.

### Iteration 1 — 2026-08-02, item 1 DRAFTED but NOT verified

Lock held by the auth-transport agent for the whole iteration, so: no gradle, no tsc, no commit.
Drafted item 1 in full — it is complete and internally consistent, and **that is not the same as
working**. Nothing here has been compiled or executed.

Changed (all uncommitted, all in our files):

- `runtime/http.ts` — `RequestCredentials` type, `HttpRequest.credentials?`, and pass-through in
  `fetchTransport`. Spread CONDITIONALLY (`...(x !== undefined ? {credentials: x} : {})`) so omitting
  it leaves `fetch`'s own default rather than pinning `undefined`, which is not the same thing to a
  `RequestInit` consumer.
- `runtime/sse.ts` — the same on `SseOptions`, plus the `RequestCredentials` import. The stream path
  needs its own because it calls `fetch` directly and never sees `HttpTransport`; for a cookie session
  it is the ONLY way a stream authenticates, since there is no header to set instead.
- `ts-verify/verifyRuntime.ts` — checks for both: set / unset / through the generated `stream()`
  wrapper. The unset case asserts the KEY IS ABSENT, not that it is undefined.

**First thing next iteration:** take the lock, run `:ultra:codegen:tsVerify`, and expect something to
be wrong — this was written blind. Then mutation-test the two pass-throughs before believing it.

### Iteration 0 — 2026-08-02, doc written

Nothing built yet. Lock held by the auth agent at the time of writing, so this file is uncommitted
until it frees. Baseline at handoff: `:ultra:codegen:check` 280, `:funktor:codegen:check` 59,
`:funktor:rest:jvmTest` 111, 0 failures, compile sweep clean, docs-site builds.
