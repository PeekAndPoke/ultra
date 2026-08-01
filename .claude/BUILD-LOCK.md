# BUILD LOCK — one agent builds this worktree at a time

**HOLDER: none**
**SINCE: 2026-08-01 (released by the codegen agent)**
**STATE: FREE — take the lock before building.**

---

## Take it before you build

Rewrite `HOLDER`, `SINCE` and `STATE`, **commit that change first**, then build. Read this file before
every build and every commit, not once per session — the holder changes underneath you.

## What the last holder changed — codegen agent, 2026-08-01

Commit `09a59008`, route access control. Touches `ultra/codegen`, `funktor/codegen` and
`funktor/rest`. What your build will pick up:

- **Every generated client member is now wrapped in `route()`** and carries `.method` / `.uri`. Any
  test asserting an emitted member's exact text needs `route('METHOD', '/pattern', ` prepended and a
  closing `)` — ten assertions in `RestApiTsContributorSpec` did.
- **Two new runtime modules** ship in every SDK with a client: `runtime/route.ts` and
  `runtime/acl.ts`. Exact-emitted-file-list assertions need both.
- **`ApiAcl` methods are renamed** in `funktor/rest`: `hasAnyAccessTo` -> `canAccess`,
  `hasAccessTo` -> `canFullyAccess`, plus new `canPartiallyAccess` and `isDenied`. Note `canAccess`
  is the PERMISSIVE one — the inverse of what `hasAccessTo` meant. Neither old name had a production
  caller.
- **`TsClientSpec.Endpoint` now rejects an HTTP method outside GET/POST/PUT/PATCH/DELETE/HEAD/OPTIONS.**

**I did not touch the in-flight auth work** (`AuthApi.kt` -> `AuthLoginApi.kt`, `AuthUserApi.kt`,
`letTheBotsWait.kt` and the test edits). Some of it was already staged in the index when I arrived
and it is staged still — I committed by explicit path, so nothing of it was swept in.

`:ultra:codegen:check` 259, `:funktor:codegen:check` 59, `:funktor:rest:jvmTest` 111, 0 failures,
compile sweep clean at release time.

## If the lock looks stale

If `SINCE` is more than a day old and nothing has been committed by the holder in that time, the holder
probably died. **Do not take the lock silently — ask the maintainer.** A stale lock costs a wait; a
wrongly-taken lock costs a debugging session that looks like a real bug.
