# BUILD LOCK — one agent builds this worktree at a time

**HOLDER: none**
**SINCE: 2026-08-02 (released by the codegen agent)**
**STATE: FREE — take the lock before building.**

---

## Take it before you build

Rewrite `HOLDER`, `SINCE` and `STATE`, **commit that change first**, then build. Read this file before
every build and every commit, not once per session — the holder changes underneath you.

## What the last holder changed — codegen agent, 2026-08-02

Commits `887f9cd7` (review fixes) and `ceee702a` (public-route metadata). `ultra/codegen`,
`funktor/codegen`, `funktor/rest`, `docs-site`. What your build will pick up:

- **`runtime/acl.ts` exports `AccessLevel`, NOT `ApiAccessLevel`.** The old name collided with what
  `models.ts` generates for the auth feature and was a hard `TS2308` through the barrel. There is now
  an `FxAccessProbe` fixture emitting `ApiAccessLevel` so the collision cannot come back unnoticed.
- **Generated members are wrapped in `route()` OR `publicRoute()`** depending on whether the route's
  auth rules admit an anonymous caller. Any assertion on emitted member text needs the right one.
- **`ApiAclSpec` / `AclRuntimeParitySpec` / `AclRuntimeSpec`** — the two closed unions are guarded in
  `ultra:codegen` (`AclRuntimeSpec`), route identity in `funktor:codegen`.
- **`FxSplitSecuredRoutes` now floors `authenticated()`**, not `public()`, so the merged-group fixture
  mirrors the real `funktor:auth` shape.

Nothing is owed to you and nothing of mine is half-finished. `:ultra:codegen:check` 266,
`:funktor:codegen:check` 57, `:funktor:rest:jvmTest` 111, 0 failures, compile sweep clean at release.

## If the lock looks stale

If `SINCE` is more than a day old and nothing has been committed by the holder in that time, the holder
probably died. **Do not take the lock silently — ask the maintainer.** A stale lock costs a wait; a
wrongly-taken lock costs a debugging session that looks like a real bug.
