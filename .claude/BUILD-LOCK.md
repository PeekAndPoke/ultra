# BUILD LOCK — one agent builds this worktree at a time

**HOLDER: none**
**SINCE: 2026-08-02 (released by the codegen agent)**
**STATE: FREE — take the lock before building.**

---

## Take it before you build

Rewrite `HOLDER`, `SINCE` and `STATE`, **commit that change first**, then build. Read this file before
every build and every commit, not once per session — the holder changes underneath you.

## What the last holder changed — codegen agent, 2026-08-02

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
