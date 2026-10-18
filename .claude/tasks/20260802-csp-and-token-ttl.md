# CSP + Trusted Types, and a shorter token TTL

**Status:** TODO — created 2026-08-02 on archiving
`.claude/tasks-archive/2026-07/20260719-token-storage-hardening.md`, which declared both of these out of
its own scope (§"Framing") and would otherwise have taken them into the archive with it.
**Plan:** none.
**Security-critical:** yes → red-team follow-up required if it lands.

## Why this exists

The token-storage task concluded that **storage choice only bounds the blast radius; the root cause is
XSS**. Bearer + `localStorage` won on product grounds (see that task for the three cookie designs that
failed), which means the exfiltration exposure is accepted deliberately — and the mitigations that
actually address it are these two, neither of which is transport-dependent.

So this is not leftover work from a dropped design. It is the part that was always owed regardless of
which design won, and it is currently unowned.

## Spec

- [ ] **CSP with nonces** on each SPA. Smoke-test that inline `<script>` execution is blocked — a header
      that is present but unenforced is worse than none, because it reads as done.
- [ ] **Trusted Types enforced.** This is the one that closes DOM-sink injection; CSP alone does not.
      Expect friction with any library that assigns to `innerHTML` — find out which, before committing.
- [ ] **Shorter access-token TTL.** 1h in the demo realms today (`funktor-demo/server/.../*Realm.kt`,
      `withExpiresAt(... plus(1.hours))`). Narrows the replay window for a stolen token. Trade-off is
      refresh frequency, and `AuthState` already refreshes 2 min before expiry — so the practical floor is
      set by how long a user may be offline mid-session, not by machinery.

## Not a substitute for

`.claude/tasks/20260728-session-revocation-wiring.md`. A shorter TTL narrows the window; revocation is
what actually ends a session. Neither replaces the other, and only revocation makes `logout()` real.

## Test evidence

- [ ] Browser smoke test per SPA: inline script blocked, app still functions.
- [ ] Nothing regresses in the four demo frontends (ops / b2b / b2b2c / admin).
