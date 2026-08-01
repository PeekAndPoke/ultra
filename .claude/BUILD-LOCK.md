# BUILD LOCK — one agent builds this worktree at a time

**HOLDER: none**
**SINCE: 2026-08-02 (released by the codegen agent)**
**STATE: FREE — take the lock before building.**

---

## Take it before you build

Rewrite `HOLDER`, `SINCE` and `STATE`, **commit that change first**, then build. Read this file before
every build and every commit, not once per session — the holder changes underneath you.

## What the last holder changed — codegen agent, 2026-08-02

Commits `ceee702a` (public-route metadata) and `0238f9c1` (auth session runtime). `ultra/codegen` and
`funktor/codegen`. What your build will pick up:

- **`TsRuntime.emit` now plans with `out.shared`, not `out.file`.** Several contributors may require
  the same runtime module — the REST client and the auth session both need `runtime/http.ts` — and
  `file` is exclusive. New `out.sharedResource` is the shared counterpart of `out.resource`.
- **New `TsRuntime.Module.Auth`** (`runtime/auth.ts`), requiring `Http`. Emitted by the new
  `AuthTsContributor`, which is registered in `funktorCodegen()`.
- **Generated members are wrapped in `route()` OR `publicRoute()`** by whether the route's auth rules
  admit an anonymous caller. Assertions on emitted member text need the right one.
- **`FxSplitSecuredRoutes` floors `authenticated()`**, and there are four new auth-floor fixture
  groups in `rest_fixtures.kt`.

Nothing is owed to you and nothing of mine is half-finished. `:ultra:codegen:check` 270,
`:funktor:codegen:check` 59, `:funktor:rest:jvmTest` 111, 0 failures, compile sweep clean at release.

## If the lock looks stale

If `SINCE` is more than a day old and nothing has been committed by the holder in that time, the holder
probably died. **Do not take the lock silently — ask the maintainer.** A stale lock costs a wait; a
wrongly-taken lock costs a debugging session that looks like a real bug.
