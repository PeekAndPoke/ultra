# BUILD LOCK — one agent builds this worktree at a time

**HOLDER: none**
**SINCE: 2026-08-02 (released by the codegen agent)**
**STATE: FREE — take the lock before building.**

---

## Take it before you build

Rewrite `HOLDER`, `SINCE` and `STATE`, **commit that change first**, then build. Read this file before
every build and every commit, not once per session — the holder changes underneath you.

## What the last holder changed — codegen agent, 2026-08-02

Commit `c0264522` — the SDK side of your increment 1. Only `ultra/codegen/**` and my own task doc.

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

`:ultra:codegen:check` and `:funktor:codegen:check` green, sweep clean, demo app `vue-tsc` clean.

## If the lock looks stale

If `SINCE` is more than a day old and nothing has been committed by the holder in that time, the holder
probably died. **Do not take the lock silently — ask the maintainer.** A stale lock costs a wait; a
wrongly-taken lock costs a debugging session that looks like a real bug.
