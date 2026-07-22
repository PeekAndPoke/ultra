# Red-team: async/suspend form validation rules

**Status:** COLLECTED — not executed. For a dedicated penetration-test session.
**Feature:** [[20260721-async-suspend-rules]] (async validation, FormController status + double-submit guard).

These are concrete attack scenarios to ATTEMPT later. Do not run them during normal feature work.

## Trust model (as-built, for the attacker to probe)
- The `FormController` `isBusy`/status double-submit guard is **client-side UX only**, never authoritative.
- Every migrated sensitive site backstops with `doubleClickProtection` (`noDblClick.runBlocking`, counter
  incremented synchronously → race-safe on the single-threaded event loop) AND the server re-validates.
- Async rules (`given { slug -> api.isSlugAvailable(slug) }`) run in the browser; their result is UX only.

## Scenarios to attempt

1. **Double-submit via the FormController guard alone.** Find/author a form that relies ONLY on
   `formCtrl.validate { … }` for de-dup (no `doubleClickProtection`). Fire two clicks in the same JS
   tick (`el.click(); el.click();`) and synthetic dispatch. Post-fix the guard flips VALIDATING
   synchronously before dispatch — confirm it now drops the second. Then confirm the SERVER tolerates
   duplicate submissions idempotently regardless (the real guarantee).
2. **Status clobber via concurrent `suspend validate()`.** While a `validate(onValid)` is PROCESSING,
   drive an overlapping `suspend validate()` (now status-neutral) — confirm it can no longer reset the
   shared status to IDLE and reopen the window.
3. **Client-side TOCTOU on availability checks.** Pass `given { slug -> api.isSlugAvailable(slug) }`
   for a slug, then race a second actor claiming the same slug before submit. Confirm the server
   enforces uniqueness/idempotency at WRITE time — a passed client check reserves nothing.
4. **Secret exfiltration via an async rule on a secret field.** Attempt to attach an async `given`
   check to a password/confirm-password field → it would transmit the plaintext secret to the server
   as an "availability" query. Assert in review that no such rule exists and none is introduced.
5. **Resource exhaustion / self-amplified load.** Hold a key / script rapid value changes on a field
   carrying an async rule (no debounce today, global never-cancelled scope) → 1 keystroke = 1 server
   call. Confirm server-side rate limiting on any endpoint reachable from a rule.
6. **Out-of-order async response poisoning.** Type a valid value then quickly an invalid one; make the
   valid value's server reply arrive last. Post-fix (latest-wins seq token) confirm the stale reply
   cannot overwrite the newer errors, and confirm submit's `runValidation()` is authoritative anyway.
7. **Error-message leakage.** Make an async rule throw with a server message / secret and confirm the
   ERROR path (`console.error`) does not render it into the DOM (it does not today) and does not leak
   cross-user.

## Notes from the security reviewer (feature gate)
- No CRITICAL/HIGH exploitable finding at ship time — client guard is defense-in-convenience, servers
  authoritative, `equalTo` still never echoes its operand.
- Mandatory server-side controls regardless of client behavior: idempotent writes, uniqueness at write
  time, rate limiting on rule-reachable endpoints.
