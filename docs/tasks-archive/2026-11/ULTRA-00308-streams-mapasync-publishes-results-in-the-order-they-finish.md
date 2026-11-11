# ULTRA-00308: Streams: mapAsync publishes results in the order they finish

> **Status (2026-11-11): DONE.** Review gate PASS 2026-11-11 (round 2 clean; ledger in ULTRA-00307). Docs (streams pages, LLM mirror) and CHANGELOG were written with the change, so no DOCS follow-up. Found by the streams operator audit of 2026-11-11 (finding F3, MAJOR).

**Security-critical:** no

## Why this exists

`AsyncStreamWrapper.handleIncoming` (`ultra/streams/src/commonMain/kotlin/AsyncStreamWrapper.kt:26`) launches one
coroutine per value and publishes whichever result finishes, in finishing order. The docs' own example
`userId.mapAsync { api.fetchUser(it) }`: `u1` (300 ms) then `u2` (50 ms) publishes u2, then u1, and the stream ends on
u1 while `userId()` is u2. A job still running after the last unsubscribe can land after the fresh one of the next
subscriber.

## The fix

Latest wins: every value takes a number; a result is published only if no newer value came meanwhile. Older jobs are
not cancelled (cancelling a user's call midway is a surprise; the result is just dropped).

## Acceptance

- Spec (commonTest, `runTest` or a hand-driven scope): a slow first and a fast second mapping end on the second;
  red before the fix.
- KDoc of `mapAsync` says "latest wins" and that a value is seen only while someone listens.

## History

- 2026-11-11: created from the operator audit (F3).
- 2026-11-11: implemented (a counter in `AsyncStreamWrapper`). `MapAsyncOrderSpec` red on the old code (ended on "one"); mutation M5 red. `MapAsyncSpec` updated: two equal values within the mapping time now give one result. Review gate next.
- 2026-11-11: review gate PASS (round 2 clean); DONE.
- 2026-11-11: DONE.
