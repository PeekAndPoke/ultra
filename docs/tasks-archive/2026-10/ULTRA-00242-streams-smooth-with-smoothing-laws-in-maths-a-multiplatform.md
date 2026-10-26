# ULTRA-00242: Streams: smooth() with Smoothing laws in maths, a multiplatform FrameClock, quantize()

> **Status (2026-10-26): DONE.** gate PASS; smooth, quantize, FrameClock, Smoothing on master, unreleased

**Security-critical:** no

## Why this exists

Klang built `Stream<Double?>.smooth()` in its workshop module (things bound for Ultra/Kraft live there first); it
drives the Motor's gauge needles. Ultra takes it over, makes it multiplatform and generalises the law. Once it ships,
Klang uses Ultra's and removes its copy; send Klang the version or hash.

Source (klang repo, Apache-2.0, no Klang dependencies): `workshop/src/jsMain/kotlin/streams/smooth.kt`,
`FrameClock.kt`, `workshop/src/jsTest/kotlin/streams/SmoothSpec.kt` (11 rows, each mutation-checked in Klang).

## The maintainer's decisions (via Klang, 2026-10-26, and in session)

1. "We keep .smooth() as it currently is default to exponential smoothing while having an overload for the other
   smoothings."
2. "Ultra has to put the Smoothing into the ultra/math package next to Ease. and then fully implement all variants and
   use them in .smooth()": `Exponential(keepPer10Ms)`, `Linear(unitsPerSecond)`, `AttackRelease(up, down)`,
   `Spring(stiffness, damping)`.
3. "i like the FrameClock approach... this would be the glue to make these operator multi-platform. ... check how the
   TickerStream currently operates under the hood."
4. Multiplatform is the goal, through the clock.
5. In session (2026-10-26): "we should also define a minimum delta. When we are below that one we should snap to the
   final value, otherwise the stream would produce new values forever."
6. In session: an operator that snaps a number to a grid: `step(size = 0.1, offset = 0.05)`, "the offset does not mean
   shifting the input value, but shifting the 'bins'"; then "`Stream<Number>.bin()` might be the right name... i let
   you judge".

## Carried over from Klang's review round 1 (2026-10-26)

- The first value after the first listener subscribes is taken as it is, no glide (`settledIn`); rising from `rest`
  only after a real `null`. Before, a needle swept up from 0 every time it was shown with a song playing.
- `listening`: upstream only while someone listens; no frame while nobody does, also when the last listener leaves
  during an emit; a resubscribe works.
- A share of 1 or more counts as 0.999 (1 never moves, above 1 moves away).
- `near` is in the value's units.

## Confirmed by the maintainer (2026-10-26, in session)

- `ultra:streams` depends on `ultra:maths` (api); `smooth()`, `FrameClock` and the grid operator stay in
  `io.peekandpoke.ultra.streams.ops`.
- Both tickers are rebuilt on `FrameClock` in this ticket.
- The grid operator is `quantize(size, offset)`.

## The design (proposed, the three points above confirmed)

- **Where the code lives.** `Smoothing` in `ultra/maths` (`io.peekandpoke.ultra.maths`, next to `Ease`); `smooth()`,
  `FrameClock` and `quantize()` in `ultra/streams` (`ops`). So **`ultra:streams` gains `api(project(":ultra:maths"))`**,
  and with it `ultra:datetime` (maths uses Kronos for `Ease.Timed`). No cycle (neither depends on streams). Every Kraft
  app gets maths and datetime transitively through `kraft:core`; Klang and Spross use datetime already. The other way
  (`smooth()` in maths) keeps streams lean but puts a stream operator outside the streams package.
- **Smoothing laws: pure steps.** `Smoothing.step(motion: Motion, goal: Double, elapsedMs: Double): Motion`, with
  `Motion(position, velocity)`, plus `isSettled(motion, goal, near)`. Testable without a clock; the operator keeps the
  clock, `null` handling, lifecycle and stop. Each law is exact over any `elapsedMs`, so the pace is the same at any
  frame rate:
  - `Exponential(keepPer10Ms)`: today's law.
  - `Linear(unitsPerSecond)`: constant speed, never past the goal.
  - `AttackRelease(up, down)`: exponential with `keepPer10Ms` for rising (`up`) and for falling (`down`).
  - `Spring(stiffness, damping)`: the damped oscillator's closed-form solution (not Euler steps, which change with the
    frame rate and can blow up on a long frame).
- **Minimum delta (decision 5)** is `near`, in the value's units: within `near` of the goal, the stream emits the goal
  exactly and asks for no more frames. A step that moves nothing (floating-point floor) also stops. For `Spring`,
  settled means its energy can no longer carry it out of `near` (`y^2 + v^2/stiffness <= near^2`, since round 1's
  batch), else a needle passing through the goal would stop mid-swing.
- **FrameClock, multiplatform.** `fun interface FrameClock` in commonMain; `FrameClock.animationFrames` (JS,
  `requestAnimationFrame`); `FrameClock.ticker(intervalMs = 16)` everywhere (coroutines, `TimeSource.Monotonic`);
  default per platform: animation frames on JS, the ticker elsewhere.
- **What the tickers do today** (decision 3), `ultra/streams/src/commonMain/kotlin/ops/ticker.kt` and
  `src/jsMain/kotlin/ops/ticker.kt`:
  - `ticker()` is a coroutine `delay` loop on `Dispatchers.Default`: on the JVM subscribers are called on a pool
    thread, and streams are not thread-safe. It drifts (`delay` after the work) and reports times truncated to whole
    ms.
  - `animTicker()` (JS) starts its `currentTime` from `Date.now()` but frames carry the `requestAnimationFrame` time
    (`performance.now()`), two different time bases.
  - Proposal: the FrameClock ticker takes a `CoroutineContext` (default `Dispatchers.Default`, as now), and both
    tickers are rebuilt on `FrameClock` in this ticket, so there is one timing path; `TickerFrame` stays.
- **The grid operator (decision 6): `quantize(size, offset = 0.0)`**, emitting the nearest `offset + k * size`.
  "Quantize" is the usual term (audio, MIDI), and it is plainly a value. `bin` reads as a histogram bin, an index or a
  bucket; `step` would collide with `Smoothing.step`. Also a pure `Double.quantize(size, offset)` in maths, which the
  operator uses. On `Stream<Double>` and `Stream<Double?>` (`null` passes), composing with `smooth()`:
  `smooth(0.9).quantize(0.1)`. A `size` of 0 or less, or NaN, passes values as they are.
- **Signature kept from Klang:** `smooth(sharePer10Ms, rest = 0.0, near = 0.001, clock = FrameClock.default)` plus
  `smooth(smoothing: Smoothing, rest, near, clock)`, so Klang's call sites move by changing the import.

## The work

- [x] `ultra/maths/src/commonMain/kotlin/Smoothing.kt`: the 4 laws and `Motion` (velocity in units per second;
  Exponential's is the curve's speed at the step's end, so it does not depend on how time is cut); `quantize.kt`.
  `SmoothingSpec` 13 rows (pace by time for every law, NaN/0/negative parameters, Spring under/critical/over-damped,
  settled needs low speed, quantize grid, halfway, edge cases).
- [x] `ultra/streams`: `api(project(":ultra:maths"))`; `ops/FrameClock.kt` (common, `ticker(intervalMs, context)`,
  `default` by platform), `FrameClock.js.kt` (`animationFrames`), `.jvm.kt` / `.native.kt` (one shared ticker);
  `ops/ticker.kt` one `FrameTicker` behind `ticker(intervalMs)`, `ticker(Duration)`, new `ticker(clock, startTime)`
  and `animTicker()`; `ops/smooth.kt` both overloads, ported from klang `0c69190f` with the law generalised;
  `ops/quantize.kt` on `Stream<Double>` and `Stream<Double?>`.
- [x] `SmoothSpec` (commonTest): Klang's 15 rows unchanged (diffed against `0c69190f`), plus 4 for the laws (Spring
  overshoots and lands, keeps its speed on a new goal, Linear's pace, a nonsense law lands on its first frame).
- [x] Docs: maths `smoothing.astro` (new, in the sidebar), streams `control.astro` (FrameClock, smooth, quantize,
  `ticker(clock)`), `js-specific.astro`; mirrors `maths.md`, `streams.md` (+ platform table). The maths "no
  dependencies" claim fixed (it depends on datetime). CHANGELOG `## Unreleased`.
- [x] /feature-review: gate PASS. (Telling Klang: not needed, maintainer 2026-10-26.)

## Implementation notes

- Klang's "share of 0 or less passes at once, no frame" stays for `smooth(sharePer10Ms)`: the operator gets no law
  then. `smooth(smoothing)` with a nonsense law lands on the first frame (the law lands at once).
- "Moves nothing" stop generalised: a step whose `Motion` (position and velocity) equals the previous one. A spring at
  its turning point has the same position for a moment, but not the same velocity.
- Not mutation-checked: neither security-relevant nor persistence (CLAUDE.md scope). Klang's rows were, against
  Klang's operator (16 mutants).

## Acceptance

- [x] Klang's 15 rows green on JVM, JS and linuxX64; every law's step tested without a clock.
- [x] No frame requested while settled or unsubscribed (Klang's rows `clock.waiting shouldBe false`).
- [x] ~~Klang has the version or hash~~: not needed (maintainer, 2026-10-26: "No need to send to klang").

## Test evidence

The latest run is the last entry of this section; the earlier ones stay as the record.

- `./gradlew :ultra:maths:jvmTest :ultra:maths:jsTest :ultra:streams:jvmTest :ultra:streams:jsTest
  :ultra:streams:linuxX64Test`, 2026-10-26, XML newer than the last edit: `SmoothSpec` 19/19 on jvm, jsBrowser,
  linuxX64; `SmoothingSpec` 13/13 jvm and js; `TickerSpec` 5/5, `CutoffTickerSpec` 3/3 (js); native 89/89 total.
- `./gradlew :kraft:core:compileKotlinJs`: compiles with the new dependency.
- Docs site: 115 pages (the new smoothing page), mirrors carry `Smoothing`.

- Final tree, 2026-10-26, after round 2's MINORs (maintainer: "Fix, no re-review"): `SmoothSpec` 21,
  `SmoothingSpec` 15, `TickerClockSpec` 5, `QuantizeSpec` 2, `FilterIsInstanceOfSpec` 4, `EasingSpec` 28 on jvm,
  jsBrowser and linuxX64; `TickerSpec` 5, `CutoffTickerSpec` 3 (js); maths native 129/129, streams native 98/98. The
  new landing row RED with `target == goal` dropped (mutation), green again on the restored tree. Docs site 115 pages.

## Review record

| Round | Level | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|---|
| 1 | 1 | MINOR | `ops/ticker.kt:34-60` | `cancelFrame` is null during the emit: on the JVM a subscribe from another thread can start a second chain (the old ticker's `job` had no such window) | fix: keep the handle during the emit; KDoc says it narrows, not closes, the race | Both reviewers (impl, domain) |
| 1 | 1 | MINOR | `ops/ticker.kt`, `ops/quantize.kt` | `ticker(clock)` has no deterministic test; the stream `quantize` none at all | fix: HandClock rows, quantize rows | |
| 1 | 1 | MINOR | `streams/extending.astro:66-87` | The platform table row is in the mirror only | fix | CLAUDE.md: pages and mirrors move together |
| 1 | 1 | MINOR | `maths/smoothing.astro:18` | "runs on animation frames": JS only | fix | |
| 1 | 1 | MINOR | `Smoothing.kt:127-128` | Spring's settled (`|y| <= near`, `|v|/60 <= near`) cuts off a swing of up to ~19x near (`Spring(10, 1)`) | fix: energy bound `y^2 + v^2/k <= near^2` | Damping only loses energy, so `|y|` stays within near |
| 1 | 1 | MINOR | `Smoothing.kt:42` | `from*keep + goal*(1-keep)` is not monotone under rounding | fix: `goal + (from - goal) * keep` | |
| 1 | 1 | MINOR | `smooth.kt`, `Smoothing.kt:58-60` | `near <= 0` trickles until the subnormals (~71 s for `Exponential(0.9)`); Linear below half an ulp snaps | fix: KDoc | Only nonsense parameters |
| 1 | 1 | MINOR | `quantize.kt:16` | `1e300.quantize(1e-10)` is Infinity | fix: a non-finite result returns the value | |
| 1 | 1 | MINOR | `control.astro` | `smooth().quantize()` re-emits the same grid value each frame | fix: point to `.distinct()` | Design note (domain) |
| 1 | 1 | SURPLUS | `ops/ticker.kt:17` | `startTime` public only for `animTicker` | fix: `internal` overload | Both reviewers |

Round 1 (two `reviewer-high`, implementation & code style and domain expert; scoped by risk: no security charter):
0 CRITICAL, 0 MAJOR, clean. Clean: parity with Klang's operator line by line (two intended generalisations), the
three spring closed forms and Exponential's velocity re-derived, long frames (only underflow), termination with the
default `near`, the lifecycle traces, the ticker rewrite against the old tickers, KDoc links, imports. The MINORs and
the surplus point were listed to the maintainer, who asked for them applied and re-reviewed (2026-10-26), together
with ULTRA-00241's open docs MINOR (the `filter` examples read unsubscribed operators).

| Round | Level | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|---|
| 2 | 1 | MINOR | `Smoothing.kt:28` vs `:138` | A negative `near`: Spring squares it and lands within `|near|`, the KDoc says "lands only at the floor" | listed for the maintainer | Phase 2: the two batch fixes contradict each other |
| 2 | 1 | MINOR | `maths/index.astro:105`, `maths.md:3` | "Depends only on `ultra:datetime`": also kotlinx.serialization | listed | `ultra/maths/build.gradle.kts:54-55` |
| 2 | 1 | MINOR | `js-specific.astro:31`, `streams.md:678` | "It is `ticker(FrameClock.animationFrames)`": the public `ticker(clock)` starts at 0.0, `animTicker()` at `performance.now()` | listed; the public `ticker(clock)` is a maintainer call (SURPLUS) | `ticker.kt:17-22` |
| 2 | 1 | MINOR | `Smoothing.kt:54-55` | Linear's sub-ulp stride lands on the second frame, not "at once" | listed | The first frame changes the velocity |
| 2 | 1 | MINOR | `SmoothSpec.kt:260-277` vs `smooth.kt:176` | No row writes back during the landing emit; dropping `target == goal` stays green, and would stall short of the new goal | listed | |
| 2 | 1 | MINOR | ticket, Design line 66 | Still gives Spring's old settle rule | listed | The part of MINOR 6 not withdrawn |

Round 2 (one `reviewer-high`, both charters, level 1: the maintainer's re-review of a MINOR batch after a clean round;
two-phase): 0 CRITICAL, 0 MAJOR, clean. Phase 2: all ten round-1 fixes hold where they had to reach; MINOR 6 withdrawn
in part (the batch section names a run on the final tree). SURPLUS: the defensive `smoothing == null` branch and the
unreachable `|| !listening` in `smooth.kt`; the public `ticker(clock)`.

**Verdict:** gate PASS (rounds 1 and 2 clean).

Round 2's six MINORs applied on the maintainer's word (2026-10-26, "Fix, no re-review"): Spring's `near` below 0 counts
as 0; maths depends on datetime and kotlinx.serialization (page, mirror, CHANGELOG); `animTicker()` "built on
`FrameClock.animationFrames`"; `ticker(clock)` stays public (maintainer), its KDoc and docs say its initial frame
carries 0.0; Linear lands "on its second frame"; the landing-emit row; the Design line. The defensive branches in
`smooth.kt` stay (SURPLUS, harmless).

### Round 1's batch, applied (2026-10-26, maintainer: "Apply all, re-review")

- Spring settles on energy (`y^2 + v^2/stiffness <= near^2`); mutation "the old speed test" RED on
  `SmoothingSpec` "a soft spring crossing the goal slowly" (jvm, 2026-10-26).
- Exponential `goal + (from - goal) * keep`. The new `SmoothSpec` row (goal 1e15, beyond near's floating-point step)
  checks termination only: the old formula passes it too (mutation survived), the monotone form is argued, not shown.
- `quantize` returns the value when the result would overflow (row); `near` and Linear's sub-ulp stride in KDoc.
- `FrameTicker` and the operator keep the frame's handle during the emit; only the frame still holding it reschedules,
  and the operator goes on when a new goal came during the landing emit. `TickerClockSpec` (5 rows on `HandClock`, now
  shared in `HandClock.kt`): frames and times, stop and restart, subscribe / leave / leave-and-rejoin during an emit.
  The cross-thread race itself is not testable on one thread; the CHANGELOG says it can rarely happen.
- `ticker(clock, startTime)` is `internal`; public `ticker(clock)`.
- `QuantizeSpec` for the stream operators; docs: the platform row on `extending.astro`, "on a FrameClock" on the
  smoothing page, `.distinct()` after `quantize`, "JVM and native"; the `filter` examples subscribe and say why,
  `filterNotNull`'s result type, `filter(initial)`'s KDoc (ULTRA-00241's open MINOR).
- Tests 2026-10-26 (XML newer than the last edit): `SmoothSpec` 20, `SmoothingSpec` 15, `TickerClockSpec` 5,
  `QuantizeSpec` 2, `FilterIsInstanceOfSpec` 4 on jvm, jsBrowser and linuxX64; `TickerSpec` 5 and `CutoffTickerSpec`
  3 on js; streams native 97/97, maths native 129/129.

## History

- 2026-10-26: created from Klang's hand-over and the maintainer's additions in session.
- 2026-10-26: plan confirmed: streams → maths, tickers on FrameClock, `quantize`.
- 2026-10-26: IN PROGRESS. maths laws, FrameClock, tickers, smooth, quantize written; tests green
- 2026-10-26: review rounds 1 and 2 clean, gate PASS; MINORs applied. DONE. Docs were updated in the change itself
  (maths smoothing page, streams control page, mirrors), so no DOCS follow-up.
- 2026-10-26: DONE. gate PASS; smooth, quantize, FrameClock, Smoothing on master, unreleased
