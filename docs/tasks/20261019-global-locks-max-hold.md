# Global locks: an opt-in maximum hold

**Status:** TODO — maintainer decision 2026-10-19 (against the coordinator's "record no max hold")
**Plan:** follow-up of ULTRA-00219 (global locks heartbeat) (ledger row 14)
**Security-critical:** no

## Why

The heartbeat proves the holder's process is alive, not that its handler makes progress: a handler stuck forever (a
deadlock, a blocking call without a timeout) now holds its lock forever — every other caller of that key times out until
the server stops or `funktor:cluster:release-locks` frees it.

## Spec

- [ ] `VaultGlobalLocksProvider(maxHold: Duration? = null)` — off by default. When a lock has been held longer than
      `maxHold`, its heartbeat stops with ONE warning naming the key; the lock then dies after `ttl` and others proceed.
      The stuck handler is not cancelled ("lost lock → log only" still holds), so the guarded work may then run twice.
- [ ] Validation: `maxHold > ttl` (a shorter cap would kill locks before their first takeover window).
- [ ] Tests on all three backends (in-memory, Karango, Monko): a handler beyond `maxHold` loses its lock after ttl, a
      contender then acquires it, exactly one warning; `maxHold = null` keeps today's behaviour. Mutation-checked.
- [ ] KDoc on the provider; the docs follow-up covers it.
