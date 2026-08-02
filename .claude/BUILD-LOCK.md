# BUILD LOCK — one agent builds this worktree at a time

**HOLDER: codegen agent (ACL docs, --sdkDir, boot sequencer)**
**SINCE: 2026-08-02 (taken for the remaining decided batch)**
**STATE: LOCKED — do not run gradle, do not commit.**

---

## Take it before you build

Rewrite `HOLDER`, `SINCE` and `STATE`, **commit that change first**, then build. Read this file before
every build and every commit, not once per session — the holder changes underneath you.

## What the last holder changed — codegen agent, 2026-08-02

Latest: `6c5cba4e` — **`ApiRoutes.mountPoint` is REMOVED.** This touches `funktor/rest` and
`funktor/core`, so it is the one change of mine that lands in your area.

An `ApiRoutes` group is declared by a `TypedApiEndpoint` the CLIENT owns too, but only the server
applied the prefix while `ApiClient` built its URL from the raw `endpoint.uri` — so any non-empty
value meant a 404 AND a silently empty access matrix. No value could be correct, because the prefix
lives in server-only code while the endpoint is shared. Nothing passed one.

`Routes.mountPoint` stays — it is sound where there is no client-side counterpart
(`InsightsGuiRoutes : Routes("/_")`), and its KDoc now says why API routes cannot have one.

If you have an `ApiRoutes` subclass in flight, drop the argument; the compiler will find it.

Earlier the same night: `c7faa088` (`credentials` on `HttpRequest`/SSE — the SDK half your increment 2
needs), `c0264522` (session reshaped around your new response), `2213ffd9` (`runtime/login.ts`, the
three-way sign-in flow), `f8e8eac8` (`runtime/refresh.ts`, refresh-before-expiry).

`funktor:rest` 116, `ultra:codegen` 288, `funktor:codegen` 59, 0 failures. Full compile sweep clean —
this was an ABI change to a published constructor.

## If the lock looks stale

If `SINCE` is more than a day old and nothing has been committed by the holder in that time, the holder
probably died. **Do not take the lock silently — ask the maintainer.** A stale lock costs a wait; a
wrongly-taken lock costs a debugging session that looks like a real bug.
