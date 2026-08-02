# BUILD LOCK — one agent builds this worktree at a time

**HOLDER: codegen agent (auto-refresh scheduling)**
**SINCE: 2026-08-02 (taken for auto-refresh scheduling)**
**STATE: LOCKED — do not run gradle, do not commit.**

---

## Take it before you build

Rewrite `HOLDER`, `SINCE` and `STATE`, **commit that change first**, then build. Read this file before
every build and every commit, not once per session — the holder changes underneath you.

## What the last holder changed — codegen agent, 2026-08-02

Commits `c0264522` and `2213ffd9` — the SDK side of your increment 1, plus the sign-in flow. Only `ultra/codegen/**` and my own task doc.

**Your reshape is fully consumed.** `Session` emits as its own `z.discriminatedUnion`,
`Session.Cookie` (a `data object`) emits `{_type:'cookie'}`, and `AuthSignInResponseToken` is gone.
The SDK's `AuthSession` now takes the RESPONSE payload instead of a token string, persists the whole
thing, and the JWT decoder is deleted — same reasoning as your `jwtClaims.kt` deletion.

`authTransport` already handles **both** modes: `Authorization` for bearer, `credentials: 'include'`
and no header for cookie. So the SDK is not a blocker for increment 2.

**Two things increment 2 still needs from your side, both recorded in my loop doc:**

- **`POST /logout`.** `signOut()` clears local state, which is right for bearer and insufficient for
  cookie — JavaScript cannot delete an httpOnly cookie.
- **Boot hydration in cookie mode.** Bearer restores from storage; cookie has nothing to restore
  because the credential is the browser's. The app must call `refreshToken` on boot — it returns the
  same payload `signedIn` takes, so one code path covers both modes.

One correction to your handover, verified before acting: the `_type: z.literal('token')` you called
spurious was REAL wire data — Slumber writes it for any `@SerialName` class via
`PolymorphicChildSlumberer`. It is gone now because `Token` is gone, but "fixing" the generator would
have broken sign-in. Evidence in `.claude/tasks/20260802-codegen-loop-handoff.md`.

**New: `runtime/login.ts`.** `completeSignIn(session, call)` collapses your three-way response into a
four-way outcome an app can switch on, and only `success` touches the session — the other two carry
single-use tokens, so storing them would make `isLoggedIn` true for a user who is not. It ships no
view, so it needs none of the open `.vue` decisions.

`:ultra:codegen:check` and `:funktor:codegen:check` green, sweep clean, demo app `vue-tsc` clean.

## If the lock looks stale

If `SINCE` is more than a day old and nothing has been committed by the holder in that time, the holder
probably died. **Do not take the lock silently — ask the maintainer.** A stale lock costs a wait; a
wrongly-taken lock costs a debugging session that looks like a real bug.
