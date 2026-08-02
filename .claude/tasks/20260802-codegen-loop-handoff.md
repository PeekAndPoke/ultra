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

### 5. WHEN THEIR DTOs LAND — do not start early

- [ ] Regenerate and confirm `Session` emits as **its own `z.discriminatedUnion`**, not folded into
      the parent union. Their handover flags this explicitly.
- [ ] Delete `decodeJwtClaims` / `expiryOf` from `runtime/auth.ts`.
- [ ] **Add boot hydration — this is the part their plan does not cover.** Deleting the decoder means
      that after a page RELOAD, bearer mode has only the token string; `expiresAt` and `permissions`
      came from the response and are gone. The session then restores with no expiry, the refresh timer
      never schedules, and it presents as "randomly logged out" — the exact failure their plan calls
      "the sharp one".
      The fix is not persisting the payload, it is calling `refreshToken` on boot: it already returns
      the full `AuthSignInResponse`, and it works in BOTH modes (bearer authenticates with the stored
      token, cookie with the cookie). One code path, and cookie mode needs it regardless because it
      boots with no session state at all.
- [ ] Restructure the storage seam: `TokenStorage` assumes a readable token, which httpOnly does not
      have. The four questions are establish / authorize a request / end / where claims come from.

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
