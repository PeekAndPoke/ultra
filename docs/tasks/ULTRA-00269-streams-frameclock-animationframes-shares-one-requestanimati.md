# ULTRA-00269: Streams: FrameClock.animationFrames shares one requestAnimationFrame per frame

> **Status (2026-10-29): DESIGNED.** Request from Klang (Klang-Research, 2026-10-29, KLANG-00388). Plan written 2026-10-29; part of the frame-and-focus batch with ULTRA-00264, ULTRA-00267, ULTRA-00268 and ULTRA-00270.

**Security-critical:** no

## Why this exists

Klang-Research, 2026-10-29 (KLANG-00388, klang `85f2986cf`, `spine/Frames.kt`): Klang moved its UI loops onto one
`ticker(FrameClock.animationFrames)`, but each `smooth()` (3 to 9 gauges at once) and each `animTicker()` still asks
the browser for its own frame. The maintainer's trace: about 9 rAF callbacks per frame, 52 ms/s of
`requestAnimationFrame` self time.

Checked against the code, 2026-10-29: `AnimationFrameClock`
(`ultra/streams/src/jsMain/kotlin/ops/FrameClock.js.kt:8-12`) calls `window.requestAnimationFrame` on every
`next()`. `FrameClock.default` on JS is that clock (`:14`), so every `smooth()` with the default clock and every
`animTicker()` (`ultra/streams/src/jsMain/kotlin/ops/ticker.kt:7`) pays for its own.

## Depends on / unblocks

- Unblocks ULTRA-00270 (threejs pause), which moves the threejs render loop onto this clock.

## Decide before implementing

1. **Batching inside the clock, same interface.** Klang's idea; no API change, every user gains. Recommendation: yes.
2. **Errors.** Native rAF isolates callbacks: one that throws is reported as uncaught and the others still run.
   Recommendation: keep exactly that: catch per callback, rethrow each on a fresh task (`window.setTimeout({ throw t }, 0)`)
   so it still reaches `window.onerror` and the console with its stack.
3. **Order and re-entry.** Callbacks run in the order they asked; a `next()` from inside a callback goes to the next
   frame (as nested rAF does). Recommendation: swap the pending list before running it.

## The work

1. `FrameClock.js.kt`: `AnimationFrameClock` becomes a private object holding `pending: MutableList<Entry>` and
   `rafId: Int?`. `next()` appends an entry and requests a frame only when none is pending; cancel marks the entry
   dead, removes it, and cancels the frame when the list empties. The frame callback takes the list, clears `rafId`,
   runs each live entry with the same timestamp, isolating errors as in decision 2.
2. Specs, `ultra/streams/src/jsTest/kotlin/ops/AnimationFrameClockSpec.kt` (Karma, real browser): with
   `window.requestAnimationFrame` wrapped to count, N `next()` calls in one frame → one rAF, all N called with the same
   timestamp, in order; cancel of one → the others still run; cancel of all → `cancelAnimationFrame` called, nothing
   runs; a `next()` inside a callback → runs on the following frame; a throwing callback → the others run.
   Each RED against today's clock (the count row is).
3. KDoc of `animationFrames`: "one browser frame request per frame, shared by all callers".
4. CHANGELOG (Unreleased): performance note; docs `streams/js-specific` one line, LLM mirror.

## Acceptance

- The count spec shows 1 rAF for 9 waiting callers (RED today: 9).
- `./gradlew :ultra:streams:jsTest` green, and jvm/native untouched.

## Out of scope

- Merging other code's own rAF loops (threejs: ULTRA-00270; pixi's own ticker stays pixi's).

## History

- 2026-10-29: request received, parked for the budget brake.
- 2026-10-29: created with its plan on the maintainer's go ("Start fully now").
