# ULTRA-00267: Streams: sample a stream's latest value on a clock (for history graphs), and history's copy cost

> **Status (2026-10-29): DESIGNED.** Plan written 2026-10-29, waiting for the maintainer's choices (Decide section). Request from Klang (Klang-Research, 2026-10-28; the maintainer's idea), from the Motor window's task-manager graphs. Not scheduled; Klang uses ticker().map { online() } meanwhile. No epic fits (as ULTRA-00264).

**Security-critical:** no

## Why this exists

Klang-Research, 2026-10-28, the maintainer's idea: Klang's Motor window draws task-manager graphs
(klang `src/jsMain/kotlin/comp/MotorHistory.kt`) from engine diagnostics that arrive every ~20 ms.
`diagnostics.history(n)` would keep 6000 entries for two minutes, copy the list 50 times a second, and tie the time
axis to the producer: when the player is gone the history stops instead of showing a gap. Klang's code today:

```kotlin
ticker(intervalMs = 250).map { online()?.let(::sampleOf) }.history(capacity = 480).map { /* 3 graphs */ }
```

The `map` reads another stream inside, which is impure, and `online` needs a subscription of its own to stay fresh.
Wished for:

```kotlin
online.sampled(every = 250.milliseconds).history(capacity = 480).map { ... }
```

Checked against the code, 2026-10-28 (`ultra/streams/src/commonMain/kotlin/ops/`):

- **Holds:** no operator samples on a clock. `ticker(...)` (`ticker.kt:11-23`) gives the frames, and the composing
  operators (`combinedWith`, `pairedWith`) emit on either side's change, so none of them gives "the latest value at
  each tick, and only then".
- **Holds:** `history` and `historyOfNonNull` are `acc.plus(next).takeLast(capacity)` (`history.kt:8-21`): two copies of
  up to `capacity` per value.

## Decide before implementing

1. **The name.** The maintainer suggested `tap`; in Rx `tap` is a side effect (our `onEach`) and `sample(period)` is
   exactly this. Recommendation: `sampled(every = ...)`, which reads as what it returns and does not clash with Rx's
   `tap`. The maintainer decides.
2. **Semantics** (Klang's, recommended as they are): emits the source's current value at every tick, unchanged and
   `null` included, so gaps show; subscribes to the source and the clock only while someone listens, as `FrameTicker`
   does; overloads for `Duration`, `intervalMs: Int` and a `FrameClock` (tests use `HandClock`).
3. **Config as a stream:** the maintainer's principle relayed in ULTRA-00264 ("the config value itself needs to be a
   stream") would make the interval a `Stream<Duration>` as well. Follows that decision.
4. **history's cost.** A ring buffer must still hand out an immutable list per value, so a snapshot copy stays unless the
   emitted list is a read-only view that never changes (a persistent structure, or a view pinned to a ring generation).
   Recommendation: a KDoc note now ("copies up to [capacity] per value; sample a fast producer first"), and measure
   before building a view.

## The work

1. `sampled` in `ops/sampled.kt` with the overloads; specs (commonTest, `HandClock`): one value per tick, repeats and
   `null` included; nothing between ticks; source and clock released on the last unsubscribe.
2. The KDoc note on `history` and `historyOfNonNull`.
3. Docs: streams operators page and LLM mirror; CHANGELOG.

## Acceptance

- `source.sampled(clock).history(n)` with a `HandClock` gives exactly one entry per tick whatever the source's rate.
- All three platforms green.

## History

- 2026-10-28: created from Klang's request; checked against the code the same day.
- 2026-10-29: DESIGNED.
- 2026-10-29: maintainer: name `sampled(every = ...)`; the interval is a stream too (the config-is-a-stream rule).
