# ULTRA-00309: Streams: small defects from the operator audit (asFlow, smooth, debounce, ticker, persistence)

> **Status (2026-11-11): DONE.** Review gate PASS 2026-11-11 (round 2 clean; ledger in ULTRA-00307). Docs (streams pages, LLM mirror) and CHANGELOG were written with the change, so no DOCS follow-up. Found by the streams operator audit of 2026-11-11 (findings F5-F10, MINOR). F4 (`indexed`) and F11 (`fold`/`history` on re-subscription) went to ULTRA-00302, which is about reads and re-reads.

**Security-critical:** no

## The defects and their fixes

| # | Where | Defect | Fix |
|---|---|---|---|
| F5 | `ops/asFlow.kt` | `callbackFlow`'s buffer (64) is full: `trySend` fails silently, the latest value included | `.conflate()`: a state stream wants the latest |
| F6 | `ops/smooth.kt` `onSub` | The first subscriber gets the first value twice (`emit` notifies the new entry, then `sub(current)`) | set the first value without notifying while subscribing |
| F7 | `ops/debounce.kt` | The first subscriber gets the current value again after the delay (the subscribe-time value starts a job) | no job for the value that comes while subscribing |
| F8 | `ops/FrameClock.kt` `TickerFrameClock` | `ticker(0)` / `ticker(-5)` / `FrameClock.ticker(0)` run frames back to back | an interval below 1 ms counts as 1 ms, as `sampled` does |
| F9 | `StreamStorage`, `PersistentStreamSource`, `jsMain/ops/localstorage.kt` | A stored `null` comes back as the initial value: `read(): T?` cannot tell "nothing stored" from "null stored" | `read()` returns `StreamStorage.Stored<T>?` (breaking for own storages) |
| F10 | `ops/debounce.kt` | Publishes on `Dispatchers.Default`, no choice: on the JVM a pool thread races the caller's subscribe | a `context` parameter, as `sampled` and `ticker` have |

`debounce` also gets its delay as a stream here (ULTRA-00271) and `invoke()` returning the last value passed on
(ULTRA-00302).

## Acceptance

- A spec per row, red before its fix (F5 by 100 sets before the collector runs; F10 by the context being used).
- `:ultra:streams:allTests` green.

## History

- 2026-11-11: created from the operator audit (F5-F10).
- 2026-11-11: implemented. Specs: `AuditJvmSpec` (F5, F8, F10), `SmoothConfigStreamSpec` (F6), `DebounceStreamSpec` (F7), `PersistInLocalStorageSpec` (F9). F9 kept `read()` and added `StreamStorage.readStored()` with a default: Klang implements `StreamStorage`. Mutations M8 (F6), M9 (F7), M11 (F8), M12 (F5), M15 (F9) red; F10 by the thread name. Review gate next.
- 2026-11-11: review gate PASS (round 2 clean); DONE.
- 2026-11-11: DONE.
