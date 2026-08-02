# Red team — the TypeScript SDK auth runtime

**Status:** COLLECTED, not executed. **Do NOT run these during feature work.**
**Source:** the `/feature-review` gate on 2026-08-02 (`.claude/tasks/20260731-sdk-auth-integration.md`).
**Target:** `ultra/codegen/src/main/resources/ts/runtime/{auth,login,refresh,acl,acl-loader}.ts` and
the generated clients built on them.

Per `CLAUDE.md`, security-critical features collect attack scenarios here and a dedicated
penetration-test session sweeps them later.

## Why this list exists

The gate found three HIGH defects in this runtime, and **all three were lifecycle races** rather than
missing checks — a request outliving the session that issued it. That is the shape to attack. The
fix in both places is a generation counter (`AuthSession.generation()`, `AclLoader.generation`), so
the interesting question is whether any OTHER async path lacks one.

## Scenarios to attempt

### A. Session lifecycle races — the confirmed-defect family

1. **Any async caller that outlives its session.** The two known ones are fixed. Enumerate the rest:
   a call in flight across `signOut()`, across a second `signIn()`, across a realm switch. Anything
   that writes to `AuthSession` or `AclLoader` after an `await` without re-checking generation.
2. **Multi-tab.** `localStorage` is shared across tabs but `AuthSession` is per-tab and holds its own
   in-memory `current`. Sign out in tab A, act in tab B: B's in-memory session is untouched and its
   generation counter knows nothing about A. Does B keep using a token the user believes is dead?
   Does B's next write resurrect the storage entry A cleared?
3. **`storage` event.** Nothing listens for it. Confirm whether a cross-tab sign-out is meant to
   propagate, and if so that its absence is a decision rather than an omission.

### B. The access matrix as an information channel

4. **Matrix disclosure across users.** Fixed for `clear()`; probe the other orderings — a matrix
   arriving during `signIn`, two `load()`s racing, a retry landing after `clear()` (the retry timer
   is generation-checked, verify that holds under a `setTimer` that fires synchronously).
5. **Stale-matrix policy.** `giveUp` deliberately KEEPS a stale matrix rather than signing out.
   Attack the window: can an attacker force the matrix fetch to fail (offline, rate limit) right
   after a privilege REVOCATION, so the UI keeps offering routes the user no longer holds? The
   server still denies, so this is a UI-truthfulness question — but confirm nothing gates a
   destructive local action on it.

### C. Token handling

6. **Token egress.** `authTransport` attaches to every request it wraps with no origin pinning.
   Today `CallOptions` cannot steer the URL and `buildUrl` percent-encodes params — verify both, then
   attack: a contributor-supplied client with a different `baseUrl` sharing one transport, a
   `baseUrl` read from config an attacker can influence, an open redirect on the API host.
7. **`sseStream` carries no credential at all** (it calls `fetch` directly, bypassing the transport).
   Confirm it is absent rather than wrong, and that no generated member silently produces an
   unauthenticated stream for an `authenticated()` route. Tracked separately in
   `.claude/tasks/20260731-sdk-sse-auth.md`.
8. **Storage tampering.** `restore()` now validates the carrier and drops expired sessions. Attack
   the remainder: a payload with a far-FUTURE `expiresAt` and a dead token (does the client ever
   refresh?), a huge `permissions` object, prototype-pollution keys (`__proto__`, `constructor`) in
   the parsed JSON.

### D. Single-use tokens in the sign-in flow

9. `selectionToken` and `resendToken` are short-lived, single-use, and returned to the CALLER rather
   than stored. Verify single-use is enforced SERVER-side (a client-side-only guarantee is none) and
   that neither is logged, put in a URL, or persisted by any example code.
10. **Replay across realms/orgs:** obtain a `selectionToken` for realm A, present it to realm B, or
    to `selectOrg` with an org the user does not belong to.

### E. Error surface

11. **Reflected content in `rejected.message`.** The doc now warns, but confirm the SERVER side:
    `AuthSystem.kt:96` puts `{realm}` — a path segment — verbatim into a message returned on a public
    route, and `AuthError.userNotFound(user)` / `providerNotFound(provider)` do the same with body
    input. Attempt: script payloads, ANSI escapes, very long values, newline injection into logs.
12. **Account enumeration** via the difference between "no such user" and "wrong password" on
    `signIn`, and via response timing.

### F. The generator itself (dev-time, lower priority)

13. `--sdkDir` now rejects absolute, `..`, and anything canonicalising to the app root. Attack what
    remains: a SYMLINK inside the owned directory (Kotlin's `deleteRecursively` walks via
    `listFiles()` and does not check for links, so the target's contents are deleted), a race that
    swaps the directory for a link between the marker check and the wipe.

## Out of scope — settled, do not re-report

- The session JWT living in `localStorage`. Known, accepted, documented; the httpOnly-cookie
  alternative was designed and dropped for b2b2c custom domains
  (`.claude/tasks-archive/2026-07/20260719-token-storage-hardening.md`). Attack what makes it WORSE
  than that baseline, not the baseline.
- `signOut()` being local-only. There is no revocation endpoint; that is known and tracked.
- The client-side `ApiAcl` being advisory. The server is the authority by design. A finding here
  needs to show the SERVER trusting client input, not the client being permissive.
