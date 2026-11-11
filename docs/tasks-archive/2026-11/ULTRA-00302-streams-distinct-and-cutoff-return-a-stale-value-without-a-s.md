# ULTRA-00302: Streams: distinct and cutoff return a stale value without a subscriber

> **Status (2026-11-11): DONE.** Review gate PASS 2026-11-11 (round 2 clean; ledger in ULTRA-00307). Docs (streams pages, LLM mirror) and CHANGELOG were written with the change, so no DOCS follow-up. Widened by the operator audit of 2026-11-11. Requested by Klang-Research (KLANG-00422); the design below was agreed by the maintainer on 2026-11-11 ("Yes please put it in as described").

**Security-critical:** no

## Why this exists

`distinct()` and `cutoffWhen()` keep their current value in a field that only a subscription updates. Read with
`invoke()` while nobody listens, they return the value from construction time:

```kotlin
val source = StreamSource(1)
val d = source.distinct()
source(2)
d()   // 1, expected 2
```

- `ultra/streams/src/commonMain/kotlin/ops/distinct.kt`: `private var latest: T = wrapped()`, `invoke() = latest`,
  `latest` updated only in `handleIncoming`.
- `ultra/streams/src/commonMain/kotlin/ops/cutoff.kt`: `private var lastValue: T = source()`, `invoke() = lastValue`,
  updated only by the source subscription. A first subscriber arriving while cut off also gets that value.

Klang hit it: stored sections hand out `stream.map { pick(it) }.distinct()`; `spine.recents.entries()` read outside a
component gave the empty list from construction time, and three browser specs failed (RecentsSpec, TutorialsSpec,
LearnPagesSpec). Klang works around it in `Stored.part`. Not urgent.

The pattern to follow is already there: `StreamMapper` reads the wrapped stream while it is not subscribed,
`sampled` and `smooth` use `if (listening) current else source()`.

## Design (agreed)

**distinct:** `invoke()` returns the source's current value (`StreamWrapper`'s default, `wrapped()`). `latest` stays,
only for the comparison in `handleIncoming`. Distinct never holds a value back, so the source's value is always right.

**cutoff:** while subscribed, unchanged. While nothing is subscribed, `invoke()` asks the predicate: not cut off →
read the source and remember it; cut off → return the remembered value.

```kotlin
override fun invoke(): T {
    // A subscriber keeps lastValue current. Without one, nothing watches the source, so read it here.
    if (predicateUnsubscribe == null && !predicate()) lastValue = source()
    return lastValue
}
```

- Why not plain `source()` while unsubscribed: holding the value while cut off is the operator's purpose; a read while
  cut off must not show what it holds back.
- The limit, to be written in the KDoc: without a subscriber nothing watches the source, so the value at the moment the
  cut-off began is unknowable. A read while cut off returns the last value read (or seen by an earlier subscription),
  else the one from construction. Watching the source without a subscriber would break the operator's resting design
  (ULTRA-00284 relies on it).
- A first subscriber arriving while cut off gets that same remembered value (today: the construction-time one).

## The work

1. `distinct.kt`: drop the `invoke()` override; a KDoc line on the comparison field.
2. `cutoff.kt`: the `invoke()` above; KDoc of `cutoffWhen`/`cutoffWhenNot` states the limit.
3. Specs (commonTest), each RED on the current code first:
   - distinct: source changes with no subscriber → `invoke()` gives the new value; also `distinctStrict`.
   - distinct: after a subscriber left, the source changes → `invoke()` gives the new value.
   - cutoffWhen: source changes with no subscriber, not cut off → `invoke()` gives the new value.
   - cutoffWhen: source changes, a read sees it, cut-off begins, source changes again → `invoke()` still gives the
     value seen before the cut-off.
   - cutoffWhen: a first subscriber arriving while cut off gets the last value read, not the construction-time one.
   - The same for `cutoffWhenNot`.
4. CHANGELOG (Fixed).

## Acceptance

- The specs above were red before the fix and are green after it.
- Existing `DistinctSpec`, `CutoffSpec`, `HandlerResilienceSpec` unchanged and green.
- `:ultra:streams:allTests` green on JVM, JS and native.

## Widened by the operator audit (2026-11-11)

The same class, found by the audit the maintainer asked for ("revisit all stream operators for defects"):
- `filter()` / `filter(initial)`: a read without a subscriber returned what it held (`null` / the initial value).
  Now: the source's value when it passes, else the last one that passed (`ops/filter.kt`).
- `debounce`: `invoke()` returned the source's value, one subscribers may not have received yet. Now: the last value
  passed on while listened to, the source's while not (built with ULTRA-00309 and ULTRA-00271).
- `indexed()` (audit F4): a read without a subscriber counted up. Now its own operator; a read counts nothing.
- `fold` / `history` / `mapAsync` see values only while listened to, by nature; `fold` and `history` also take the
  source's current value in again on every new subscription (audit F11; a repeated value is legitimate, so no code
  fix). KDoc says so.

## Test evidence

- `commonTest/ops/ReadWithoutSubscriberSpec.kt` (distinct, distinctStrict, cutoffWhen, cutoffWhenNot, filter, indexed)
  and `DebounceStreamSpec.kt`. The distinct and cutoff cases were red on the old code (8 of 8, "expected 2 but was 1").
- Mutations (undoing each fix): see ULTRA-00309's log; cutoff M2, filter M6, indexed M7, debounce read M10.

## History

- 2026-11-11: created on Klang-Research's request (KLANG-00422); design agreed by the maintainer.
- 2026-11-11: implemented; widened by the operator audit (filter, debounce, indexed, fold KDoc).
- 2026-11-11: review gate PASS (round 2 clean); DONE.
- 2026-11-11: DONE.
