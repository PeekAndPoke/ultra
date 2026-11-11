# ULTRA-00307: Streams: a value published during a notification; a handler removed during one

> **Status (2026-11-11): DONE.** Review gate PASS 2026-11-11 (round 2 clean; ledger in ULTRA-00307). Docs (streams pages, LLM mirror) and CHANGELOG were written with the change, so no DOCS follow-up. Found by the streams operator audit of 2026-11-11 (the maintainer: "revisit all stream operators for defects. Write tickets for all defects and fix them right away."). Both MAJOR.

**Security-critical:** no

## Why this exists

`notifyHandlers` (`ultra/streams/src/commonMain/kotlin/streams.kt:28`) loops over a snapshot of the handlers with the
value it was called with.

**F1: a newer value is overtaken by the older one.** A handler that sets the stream again (a clamp) starts a nested
notification with the new value; the outer loop then goes on with the old one, so the handlers after it end on the
old value:

```kotlin
val s = StreamSource(0)
s.subscribeToStream { if (it < 0) s(0) }   // subscribed first
val d = s.map { it }
s(-5)
// s() == 0, d() == -5 while subscribed; a distinct() downstream keeps -5
```

Every publisher has the pattern: `StreamSourceImpl.invoke(next)`, `StreamWrapperBase.publish`, `StreamCombinator`,
`cutoff`, `switchMap`.

**F2: a handler removed during a notification is still called** with that value. User handlers run after their
`Unsubscribe` returned, and operators that tear down on the last unsubscribe get a value afterwards:
- `sampled` stops ticking for good: its `every` handler from the snapshot sets `interval` after `stopListening()`
  reset it, so the next listening sees "the same interval" and asks for no tick.
- `switchMap` returns a stale `invoke()`: its inner handler from the snapshot sets `hasValue = true` after `stop()`.

`cutoff` already guards against F2 locally (`sourceCount`).

## The fix

- F2: `notifyHandlers` skips a handler that is no longer in the live set (`ownEntry` makes every entry unique, so
  membership is exact).
- F1: each publisher counts its publishes; `notifyHandlers` stops a notification once a newer one of the same publisher
  started. The nested notification has already reached every live handler with the newer value. Earlier handlers saw
  both values in order; later ones see only the newer. That is "latest wins" for synchronous re-entrant writes, which
  is a change of delivery: a `history` downstream records only the newer value of such a write.

## Acceptance

- Specs (commonTest) for F1 on a source, a map, a combinator; F2 on a user handler, `sampled`, `switchMap`; each red
  before the fix.
- `:ultra:streams:allTests` green; the Kraft and Klang users of streams compile and their tests are green.

## Review record (the ledger) — streams gate (ULTRA-00302, 00307, 00308, 00309, 00271)

Round 1, 2026-11-11, level 1 (`reviewer-high`), blind; charters 1 (implementation) and 2 (domain). The security charter
was left out by risk: no auth, tenancy or data boundary in the change; the persistence item (F9) is covered by
charter 1 and a mutation check.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MAJOR | `ops/onEach.kt:20-25` | An `onEach` block that writes upstream still leaves subscribers on the older value; the docs promise no handler does (domain M1) | fix | Confirmed by reading: `block` runs the nested publish, then `onEach` publishes the older value with a fresh notification. `onEach` counts incoming values; the docs say `map`/`filter`/`fold` lambdas must not write upstream |
| 1 | MINOR | `kraft/core/.../messages/MessagesHandler.kt:10-18` | Kraft's message bus is a `StreamSource`: latest-wins drops a message for later handlers when a handler sends another synchronously (domain m1) | fix | An event bus must deliver every message; a queue is out because `sendMessage` reads `isStopped` right after `send()`. `MessagesHandler` gets its own delivery, every handler, in order, as before ULTRA-00307 |
| 1 | MINOR | `combining.astro:127-175`, `streams.md:446-453` | `history`/`historyOfNonNull`/`foldNotNull` examples have no subscriber, so they print nothing (impl F-B, domain m2) | fix | Confirmed: they record only while listened to; the examples subscribe first |
| 1 | MINOR | `ops/debounce.kt:131-137` | Clearing `pending`/`cancelPending` in the timer block can lose the newest value across threads (impl F-A) | fix | The block only checks its token; the fields are reset by `incoming` and `stopListening` |
| 1 | MINOR | `ops/filter.kt:82` | Nullable `filter`: a read without a subscriber ignores a passing `null` (impl F-C, domain n1) | fix | `takeIf` cannot tell a passing `null`; explicit check as in the initial-value variant |
| 1 | MINOR | `ultra/streams/CLAUDE.md` | Committed although ULTRA-00271 says it waits for the OK (impl, domain m3) | reject | The maintainer gave the OK on 2026-11-11 ("CLAUDE.md ok"); the ticket header was stale and is updated |
| 1 | NIT | `streams.kt:31` | `superseded` default unused (impl F-D) | fix | Removed |
| 1 | NIT | `ops/ticker.kt:16-19`, `ops/debounce.kt:21` | Constant overloads not via `steady`, no `context` (impl F-E) | fix | `ticker(Duration, context)` through `steady`; `debounce(delayMs)` already delegates; the Duration overload has the context |
| 1 | NIT | `ops/fallback.kt:9,16` | On a `Stream<Any?>`, `fallbackTo(stream)` now takes the stream's value (domain n2) | reject | Only an `Any`-typed stream; the stream overload is the more specific one by design (ULTRA-00271) |
| 1 | NIT | `smooth`/`quantize` | Mixed constant and stream config has no overload (domain n3) | fix (docs) | The docs say to wrap a constant in `steady()` |
| 1 | — | docs "Writes during a publish" | Handlers before the writer see both values, after it only the newer one (domain m1 note) | fix (docs) | One sentence |

Round-1 fixes, verified: streams JVM 174/174, browser 208/208, Kraft core 219/219. Mutations, each red: R2-onEach
(the counter), R2-filter-null; `MessagesHandlerSpec` red on the old bus ("x" lost).

Probed and clean (round 1): `Notifier`'s superseded logic in every nested publish path (sources, wrappers, combinator,
cutoff, switchMap, smooth, debounce, history), diamonds, count overflow; F2 membership; `NotificationSpec` traced red
on the old code; mapAsync latest-wins; read semantics consistent ("what a new subscriber would be told"); overloads
unambiguous; `readStored` compatible with Klang's storage; no implementor of the changed base classes in the repo.

### Round 2, 2026-11-11, level 2 (`reviewer-xhigh`), two-phase; charters 1 and 2

Both reviewers disclosed that their phase-1 read of this ticket took in the ledger too (a whole-file read); their
findings were checked against the code, and phase 2 reconciled them. Result: **0 CRITICAL, 0 MAJOR: clean.** Every
round-1 fix holds; three reached only part of what they needed (rows marked "gap").

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 2 | MINOR | `AsyncStreamWrapper.kt:29-40` | Latest-wins can fail with the default multi-threaded scope (impl M1) | fix (KDoc) | The check and the publish are not atomic on a pool; the KDoc tells JVM users to pass a scope on the subscribers' thread, as debounce/sampled/smooth do |
| 2 | MINOR | `MessagesHandler.kt:24-32` | A handler that throws on the replayed message stays registered (impl M2, domain 3) | fix | Removed and rethrown, as `StreamSourceImpl` does; spec case |
| 2 | MINOR | `TickerIntervalStreamSpec.kt:37-48` | "keeps its clock" cannot fail (impl M3) | fix | Counts the clocks made |
| 2 | MINOR | `cutoff.kt:9-23` | The read limit is only on the private class (impl M4) | fix | On the public KDocs |
| 2 | MINOR | `control.astro:147-153`, `streams.md:572-579` | The mapAsync example has no subscriber (impl D1, domain B1; gap of round-1 row 3) | fix | Subscribes first |
| 2 | MINOR | `filter.kt:80,110`, docs | "else the last one that passed" is wrong for values nobody saw (domain 1) | fix | "the last passing value it saw" |
| 2 | MINOR | docs, CHANGELOG | The lambda rule lists only map/filter/fold; combinedWith and switchMap fail the same way (domain 2, gap of round-1 row 1) | fix (docs) | "no other operator's lambda may", naming them |
| 2 | MINOR | `StreamStorage.kt:8-17` | `readStored()` public but only internal code calls it (domain 4, SURPLUS) | reject | The interface itself could not say "a stored null"; the default keeps every implementor compiling, and an app building its own persistence on the interface needs it |
| 2 | MINOR | `CHANGELOG.md` | The stored-null change is data behaviour without a compile signal (domain 5) | fix | Moved under Breaking changes |
| 2 | NIT | `onEach.kt:7` | KDoc "then passes it through" (impl N1, gap of row 1) | fix | |
| 2 | NIT | `debounce.kt:34-35` | "published on [context]" wrong for a zero delay (impl N2) | fix | |
| 2 | NIT | `cutoff.kt:144` | Guard unreachable for library streams (impl N3) | fix (comment) | Kept for a foreign Stream implementation |
| 2 | NIT | `streams.kt:31-44` | Fold `notifyHandlers` into `Notifier` (impl N4) | reject | Style; one small function with one caller is clear as it is |
| 2 | NIT | `debounce.kt:133` | No test for the token check (impl N5) | fix | A late-cancel timer spec |
| 2 | NIT | `MessagesHandlerSpec` | In `kraft/core/src/jsTest`; `kraft/CLAUDE.md` says `core-tests` (impl N6) | fix | Moved |
| 2 | NIT | `AuditJvmSpec.kt:40-58` | Unsynchronised cancel can leave a frame chain running (impl N7) | fix | A stop flag |
| 2 | NIT | `history.kt`, `debounce.kt` | Third and fourth copy of the listening pattern (impl N8) | defer | A shared base is a refactor of its own |
| 2 | NIT | docs | quantize lacks the `steady()` hint (impl D2, domain B2; gap of row 10) | fix | |
| 2 | NIT | `CHANGELOG.md` | `ticker(Duration, context)` missing (impl D3) | fix | |
| 2 | NIT | `subscriptions.astro:67-76` | Aside under the wrong heading; mirror order differs (domain 6) | fix | |
| 2 | NIT | `MessagesHandler.kt:29` | The bus replays the last message to a new subscriber (domain 7) | maintainer decision | As before ULTRA-00307 (a StreamSource did the same); kept |

**Gate verdict (streams): PASS** — round 2 clean; its MINORs and NITs fixed as above.

## History

- 2026-11-11: created from the operator audit (findings F1, F2).
- 2026-11-11: implemented (`Notifier`, `notifyHandlers` skips removed handlers). `NotificationSpec`: 6 cases, all red on the old code; mutations M3 (membership check removed) and M4 (superseded check removed) red. Kraft core 216/216 green. Review gate next.
- 2026-11-11: review gate PASS (round 2 clean); DONE.
- 2026-11-11: DONE.
