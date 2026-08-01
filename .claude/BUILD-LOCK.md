# BUILD LOCK — one agent builds this worktree at a time

**HOLDER: auth-transport agent**
**SINCE: 2026-08-02**
**STATE: LOCKED — do not run gradle, do not commit.**

---

## Take it before you build

Rewrite `HOLDER`, `SINCE` and `STATE`, **commit that change first**, then build. Read this file before
every build and every commit, not once per session — the holder changes underneath you.

## What the last holder changed — codegen agent, 2026-08-02

Commits `ceee702a`, `0238f9c1`, `5b131068`. All in `ultra/codegen` and `funktor/codegen`.

- **`TsSdkEmitContext` gained a `registry` field.** Any test constructing one directly needs it.
- **`TsRuntime.emit` plans with `out.shared`**, not `out.file` — several contributors may need the
  same runtime module. New `out.sharedResource` is the shared counterpart of `out.resource`.
- **New `TsRuntime.Module.Auth`** (`runtime/auth.ts`) and a new `AuthTsContributor`, registered in
  `funktorCodegen()`.
- **`mount.ts` is emitted** whenever any contributor registers a page route, and the builder now fails
  the build if a registered component was never emitted.
- **Generated members are wrapped in `route()` OR `publicRoute()`** by whether the route's auth rules
  admit an anonymous caller.

Nothing is owed to you and nothing of mine is half-finished. `:ultra:codegen:check` 280,
`:funktor:codegen:check` 59, `:funktor:rest:jvmTest` 111, 0 failures, compile sweep clean at release.

## If the lock looks stale

If `SINCE` is more than a day old and nothing has been committed by the holder in that time, the holder
probably died. **Do not take the lock silently — ask the maintainer.** A stale lock costs a wait; a
wrongly-taken lock costs a debugging session that looks like a real bug.
