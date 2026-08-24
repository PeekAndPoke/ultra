# Streams: switchMap / switchMapNotNull operator

**Status:** DONE (archived 2026-08-24)
**Plan:** standalone (user request 2026-08-24; design plan re-confirmed with user before implementation)
**Security-critical:** no (confirmed by the security review — no trust boundary, no red-team follow-up)

## Spec

Add a switch-to-latest-inner-stream operator (RxJS `switchMap` / Flow `flatMapLatest` semantics).
Driving use case: `playerStream: Stream<Player?>` with `Player.diagnostics: Stream<Diagnostics>` →
`playerStream.switchMapNotNull { it.diagnostics }` follows the diagnostics of the latest non-null
player, holds `null` while there is no player. Naming confirmed with the user (over `flatMapLatest`
and `subToInnerStream`).

```kotlin
fun <T, R> Stream<T>.switchMap(selector: (T) -> Stream<R>): Stream<R>
fun <T : Any, R> Stream<T?>.switchMapNotNull(selector: (T) -> Stream<R>): Stream<R?>
```

Acceptance criteria:

- [x] First subscriber receives the current inner value exactly once (no duplicate initial delivery)
- [x] Outer change → old inner unsubscribed BEFORE new inner subscribed; new current value published
- [x] Same inner stream instance selected again → subscription kept, nothing published
- [x] `switchMapNotNull`: holds/publishes `null` while outer is null; `null → null` is silent
- [x] `invoke()` recomputes fresh while unsubscribed (StreamMapper pattern); cached while subscribed
- [x] Last unsubscribe fully releases outer AND inner; re-subscribe rebuilds from current outer value
- [x] Selector throwing during an outer emission leaves the previous inner subscription intact
- [x] Non-null result via composition with existing `fallbackTo` (no `initial:` overload)
- [x] Tests green on jvm target (and linuxX64)

## Implementation notes

- `Stream<T>` is invariant (`ultra/streams/src/commonMain/kotlin/Stream.kt:6`), so `switchMapNotNull`
  cannot delegate via `steady(null)` — impl class `SwitchMapStream<OUTER, INNER, RESULT>` carries
  the `R → R?` upcast on values (`convert`), never on stream containers, keeping the inner-stream
  `===` identity check intact.
- **Subscription ordering follows `StreamWrapperBase`/`StreamCombinator`, NOT `CutoffStream`.**
  `start()` runs before the subscriber is added (`ops/switchMap.kt:88-95`), so its publishes land in
  an empty handler set and no user code runs while wiring up; the subscriber is then served
  explicitly via `sub(invoke())`. The first draft copied `CutoffStream`'s "initial value arrives via
  the upstream's immediate emission" trick and that was the root cause of three review defects.
- **The initial switch is performed by `start()` itself, not inside the outer handler**
  (`ops/switchMap.kt:105-124`). Running the selector during the outer's immediate emission would
  orphan that subscription when the selector throws, because the handle is not yet assigned. A
  `try/catch` around it calls `stop()` and rethrows, so a failed subscribe leaves no state behind.
- **`switchCount` guards the window where an inner subscription is being established**
  (`ops/switchMap.kt:148-166`). `inner.subscribeToStream` delivers the inner's current value
  synchronously, during which a subscriber may switch again or tear the operator down; the handle is
  therefore committed only if that has not happened, and released otherwise.
- `currentInner` is committed only after a successful inner subscribe, so an inner whose
  `subscribeToStream` throws cannot leave the operator permanently silenced by the identity check.
- `lastValue`/`hasValue` are lazy (`ops/switchMap.kt:64-66`): the selector does NOT run at
  construction. No other operator runs user code eagerly (`cutoff.kt:37` only reads `source()`), and
  eager evaluation made the operator factory throwable inside component field initializers.
- `compute()` uses early-return (`selector(outer()) ?: return absent()`) so "no inner selected" is
  not conflated with "inner currently holds null".

### Behaviour worth knowing (changed vs the original design sketch)

- Constructing the operator no longer runs the selector, so `outer.switchMap { … }` never throws;
  the selector's exceptions surface from the first value read or subscribe instead.
- The immediate call to a NEW subscriber is not wrapped in `notifyHandlers` — a throwing subscriber's
  first delivery propagates to the caller. This now matches `StreamSourceImpl.kt:41`,
  `StreamWrapperBase.kt:49` and `cutoff.kt:51`; the draft swallowed it, which was the outlier.

## Test evidence

- [x] Unit/behavior tests: `ultra/streams/src/commonTest/kotlin/ops/SwitchMapSpec.kt` (26 cases)
- [x] End-to-end tests: n/a — pure common library code, no backend
- [x] Full test command(s) run + green (2026-08-24): `./gradlew :ultra:streams:jvmTest` and
      `:ultra:streams:linuxX64Test`; both XML reports show `tests="26" failures="0" errors="0"` for
      `SwitchMapSpec`. Whole module on jvm: 57 tests, 0 failures — no existing spec regressed.
- [x] **Findings verified before fixing**: the five regression tests were added FIRST and all five
      failed against the reviewed implementation. No finding was acted on unconfirmed.
- [x] **Mutation-tested after fixing** (4 mutants, all killed by the intended tests):
  1. `switchCount`/teardown guard disabled → the two reentrancy tests fail.
  2. `currentInner` committed before the inner subscribe → the failed-inner-subscribe test fails.
  3. `start()`'s cleanup `try/catch` removed → the selector-throws-while-subscribing test fails.
  4. Subscriber added before `start()` → 16 tests fail (the ordering is load-bearing).

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | FAIL → fixed | 2 HIGH (reentrant clobber, throwing selector on subscribe), 3 MEDIUM (reentrant double-start, vacuous cache test, missing mutation guards), 2 LOW |
| 2. Domain expert | FAIL → fixed | 1 HIGH + 2 MEDIUM (all the same exception-safety root cause), 3 MEDIUM/LOW KDoc gaps; endorsed every semantic decision (naming, identity dedup, `fallbackTo` composition, distinct functions) |
| 3. Security | PASS (no boundary findings) | 3 MEDIUM leak/state-corruption defects (same root cause), rest inherited library-wide patterns |

All three reviewers independently converged on ONE root cause: an unsubscribe handle was stored
*after* a subscribe call that can synchronously reenter or throw. Fixes applied:

1. **HIGH — selector throwing on first subscribe orphaned the outer subscription and permanently
   disabled teardown** (the zombie subscriber kept `subscriptions` non-empty, so `stop()` could never
   run again, and each retry added another orphan). Fixed by the deferred initial switch + cleanup.
2. **HIGH — reentrancy during an inner subscribe leaked a subscription and published values from a
   de-selected stream** (nested `switchTo` could not release the in-flight subscription, then the
   outer frame clobbered the nested handle). Fixed by the `switchCount` guard.
3. **MEDIUM — a subscriber subscribing again during its first value double-started the outer.**
   Fixed by the ordering restructure.
4. **MEDIUM — a failing inner `subscribeToStream` left `currentInner` set, silencing the operator
   forever.** Fixed by committing `currentInner` after success.
5. **MEDIUM — test gaps**: the "cached while subscribed" test was vacuous (mutating `invoke()` to
   `return compute()` passed all 18 cases). Added cached-value assertions plus the duplicate-handler
   and multi-subscriber-teardown mutation guards.
6. **LOW** — eager construction-time `compute()` removed; `error("unreachable")` given a real
   message; KDoc now states identity comparison, shared-upstream restart, and that
   `switchMapNotNull` RESETS to null rather than holding the last value (unlike `foldNotNull`).

**Rejected / deferred, with reasons:**

- `switchMapOrNull` for a *nullable inner stream* (reviewer 2): the private impl already supports it
  (`selector: (OUTER) -> Stream<INNER>?`), but adding unrequested public API during a review-fix pass
  is scope creep. Recorded as a possible follow-up if a caller needs it.
- Inherited library-wide patterns, NOT introduced here and left alone: the unprotected immediate call
  to a new subscriber (`StreamSourceImpl.kt:41` and friends); `Set`-based subscription storage
  silently deduping the same handler instance (`StreamSourceImpl.kt:18`); no bound on synchronous
  emission cycles, with `notifyHandlers` swallowing the resulting `StackOverflowError`
  (`streams.kt:25`); `notifyHandlers` printing throwables. Each was verified against the existing
  operators before being classified as inherited.

**Follow-up DOCS task:** `.claude/tasks/20260824-docs-streams-switchmap.md` (public API touched).
