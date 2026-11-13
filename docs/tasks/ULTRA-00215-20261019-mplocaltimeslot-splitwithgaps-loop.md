# ULTRA-00215: MpLocalTimeSlot.splitWithGaps loops forever across midnight

> **Status (2026-11-13): IMPLEMENTED, awaiting the review gate** (track A of ULTRA-00316, worktree `datetime`).
>
> **Part of:** ULTRA-00255 (Ultra libraries epic)

**Plan:** none — out-of-scope finding from the `/feature-review` of ULTRA-00213 (datetime js performance) (round 1, 2026-10-19)
**Security-critical:** yes, mildly (a caller-controlled duration hangs the thread and fills memory) → red-team note below

## Spec

**Pre-existing** (the old `%` in `MpLocalTime.ofMilliSeconds` wrapped the same way). `MpLocalTimeSlot.splitWithGaps`
(`ultra/datetime/src/commonMain/kotlin/MpLocalTimeSlot.kt:173-180`) steps `currentTo = currentFrom.plus(duration)`
while `currentTo <= to`. `MpLocalTime.plus` wraps at midnight, so a step that crosses midnight lands EARLIER and the
loop never ends: `MpLocalTimeSlot(08:00, 20:00).splitWithGaps(24.hours)` adds the same slot forever (OOM).
`duration + gap` that wraps past midnight does the same.

- [x] Stop when a step would cross midnight (compare in milliseconds of day, or guard `currentTo > currentFrom`)
- [x] Tests: `24.hours`, `25.hours`, a duration + gap that wraps, a slot ending at 23:59:59.999 — all terminate
- [ ] Red-team scenario (collect only): an endpoint that passes a user-chosen duration into `splitWithGaps`

## Implementation notes

- `MpLocalTimeSlot.splitWithGaps` now counts in milliseconds of day (`Long`) instead of stepping with
  `MpLocalTime.plus`, which wraps at midnight. Round 1 (first commit): a loop over `durationMs <= endMs - currentFromMs`.
  Review round 1: the count is worked out before anything is built, `(span - duration) / (duration + gap) + 1`, or 0
  when `duration > span` (a remainder, not a sum, because `Duration.INFINITE` is `Long.MAX_VALUE` ms); the gap is
  capped at one day, so `duration + gap` cannot overflow; the slots come from `List(count) { }`, no extra copy.
- **`maxSlots: Int = 100_000`** (review round 1, MAJOR; the maintainer: "yes to maxSlots and the default"). A whole day
  in 1 ms steps is 86 399 999 slots. A split above `maxSlots` throws `IllegalArgumentException("splitWithGaps would
  create <count> slots, more than maxSlots = <limit>")` before any slot exists; a `maxSlots` below 1 throws too. An
  infinite duration still yields an empty list, not an exception.
- Callers: only `MpLocalTimeSlotSpec` in the repo; the docs example (`ranges.astro`, named `duration`/`gap`) still
  compiles and its result (2 slots) holds. The new parameter needs a docs line (coordinator, not this track).
- Found along the way, same hang: a duration under one millisecond (`500.microseconds`) passed the old
  `duration <= ZERO` guard but added 0 ms per step. The guard now checks whole milliseconds, so it yields no slots.
- A slot ending at 23:59:59.999 (`completeDay`) hung too: the last step wrapped to 00:00. Now its last slot is 22:00-23:00.
- **Corrected in review round 1:** behaviour is NOT unchanged for every input that used to terminate. The old code took
  durations and gaps modulo 24 h (`MpLocalTime.plus`) and read `to < from` as a slot across midnight, so these
  terminated before and now give a different, correct result (re-checked with a simulation of the old loop):
  `08:00-20:00` by 25 h gave twelve 1-hour slots, now none; by `Duration.INFINITE` one slot (08:00-15:12:55), now
  none; by 1 h with a 24 h gap twelve slots, now one; by 1 h with a 20 h gap four slots across midnight
  (08-09, 05-06, 02-03, 23-00), now one; the invalid slot `22:00-02:00` by 2 h two slots (22-00, 00-02), now none.
  Inputs whose steps never wrapped (the existing minute table) are unchanged.
- Tests are bounded by `withinTimeLimit` (expect/actual in the test sources): the JVM runs the block on a daemon thread
  and fails after the limit; JS and native call it directly.
- Red-team scenario (collect only, not executed): an endpoint that passes a caller-chosen duration into
  `splitWithGaps`. Not filed as its own ticket (track agents create none); the coordinator decides.

## Test evidence

- New cases in `MpLocalTimeSlotSpec` (commonTest, so JVM, JS and native): 24 h, 25 h, infinite and sub-millisecond
  durations, zero and negative duration, a step that fits exactly with the next one wrapping, a duration + gap that
  wraps, an infinite gap, a gap ending exactly at `to`, a negative and a zero gap, `completeDay` by 1 h and by 3 h + 3 h
  gap, an invalid slot.
- Before the fix (bounded run, JVM): 10 of the new cases failed (2522 tests, 10 failures). Eight hung and failed with
  `OutOfMemoryError` from the endless list growth (24 h, under 1 ms, 12 h, 4 h + 20 h gap, 4 h + 8 h gap, 6 h with a
  negative gap, `completeDay` by 1 h and by 3 h + 3 h). 25 h and `Duration.INFINITE` terminated and failed as
  assertion failures (the modulo-24-h results above). The others (zero/negative duration, infinite gap, invalid slot)
  already passed. (Corrected in review round 1.)
- After the fix, from the XML: jvmTest 2522 tests / 0 failures; jsBrowserTest 1887 / 0; linuxX64Test 1875 / 0
  (`MpLocalTimeSlotSpec` 71 / 0 on each).
- Mutation-checked:
  - removed the whole-millisecond guard → red: zero, sub-millisecond and negative duration (+ the existing
    "duration < 1" case), 4 failures.
  - loop condition as a sum (`currentFromMs + durationMs <= endMs`) → red: infinite duration, 1 failure.
  - removed the gap `break` → stays green: an EQUIVALENT mutant. Without it the next loop check already sees a negative
    remainder, and an overflowing gap wraps to a negative remainder as well. Kept as the explicit guard.

- Review round 1, `maxSlots`: six new tests (exactly 100 000 slots at the default; 100 001 throws with the exact
  message; `completeDay` in 1 ms steps throws within 1 s, bounded; a custom limit of 4 / 3; `maxSlots` 0, -1 and
  `Int.MIN_VALUE` refused; infinite duration or gap with `maxSlots = 1` do not throw). RED first with the parameter
  added but not enforced: 4 failed (no exception thrown; the 1 ms day ran out of memory), the at-limit and infinite
  cases passed already.
  After: jvmTest 2529 / 0, jsBrowserTest 1894 / 0, linuxX64Test 1884 / 0 (`MpLocalTimeSlotSpec` 77 / 0 each).
  Mutation-checked (JVM):
  - count check removed → red, 3 (one above, the 1 ms day, the custom limit).
  - `+ 1` dropped from the count → red, 16.
  - `maxSlots >= 0` instead of `>= 1` → red, 1 (below 1 is refused).
  - count as a sum (`fromMs + durationMs > fromMs + spanMs`) → red, 2 (both infinite-duration cases).
  - gap cap removed → stays green: EQUIVALENT. The overflowed `duration + gap` is negative and larger than the
    remainder, so the quotient is 0 and the count 1, and index 0 never uses the step. Kept: the cap states the bound.
- Review round 1, KDoc boundaries (no code change): the KDoc states that slots never reach past `to`, a shorter
  remainder is dropped, an invalid slot (`to <= from`, e.g. 22:00-02:00) yields none, and durations are cut to whole
  milliseconds. Two pins added (22:00-02:00 by 2 h → none; 00:00:00.000-00:00:00.003 by 1.5 ms → three 1 ms slots).
  They describe the current behaviour, so they were not RED first; their mutation checks:
  - span taken modulo a day (`(to - from).mod(day)`) → red, 2 (both invalid-slot cases).
  - duration rounded up to whole milliseconds → red, 2 (under one millisecond, cut to whole milliseconds).
  After: jvmTest 2531 / 0, jsBrowserTest 1896 / 0, linuxX64Test 1886 / 0 (`MpLocalTimeSlotSpec` 79 / 0 each).
  `completeDay.splitWithGaps(1.hours)` still gives 23 slots and its test is unchanged (not decided by the maintainer).

## Changelog text

- `ultra/datetime`: `MpLocalTimeSlot.splitWithGaps` no longer loops forever when a step or a step plus gap crosses
  midnight (e.g. a duration of 24 h, or a slot ending at 23:59:59.999), nor on a duration under one millisecond; such
  steps now end the split (ULTRA-00215).
- `ultra/datetime`, behaviour change: `splitWithGaps` no longer takes durations and gaps modulo 24 h. `08:00-20:00`
  by 25 h or by `Duration.INFINITE` now yields no slots (was twelve 1-hour slots, resp. one), and a gap of 20 h or
  24 h leaves room for one slot only (was slots across midnight). An invalid slot (`to <= from`, e.g. 22:00-02:00) now
  yields no slots (was split across midnight) (ULTRA-00215).
- `ultra/datetime`: `MpLocalTimeSlot.splitWithGaps` takes `maxSlots` (default 100 000) and throws
  `IllegalArgumentException` before building a split with more slots, e.g. a whole day in 1 ms steps (ULTRA-00215).

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|

**Verdict:**

## History

- 2026-10-21: numbered ULTRA-00215 (was `docs/tasks/20261019-mplocaltimeslot-splitwithgaps-loop.md`).
- 2026-11-13: implemented on track A (datetime) of ULTRA-00316; awaiting the review gate.
- 2026-11-13: review round 1 fixes (`maxSlots`, KDoc boundaries, corrected notes); awaiting round 2.

## Review record (the ledger): track A of ULTRA-00316 (ULTRA-00215, 00143, 00217)

Round 1, 2026-11-13, standard gate, level 1 (`reviewer-opus-high`), blind; charters 1, 2 and 3. **NOT clean: 1 MAJOR**
(security; implementation and domain returned CLEAN).

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MAJOR | `MpLocalTimeSlot.kt:167-198` | `splitWithGaps` has no limit on the slots it returns: a day in 1 ms steps is 86.4 million objects from one call (security 1) | fix | The maintainer: "yes to maxSlots and the default". `maxSlots = 100_000`, counted before anything is built (50250f27) |
| 1 | MINOR | `MpLocalTimeSlot.kt:181` | `completeDay` by 1 h gives 23 slots: 24:00 cannot be represented (domain M2) | maintainer | Open: the maintainer wants a way to represent 24:00; nothing changed in this branch |
| 1 | MINOR | ULTRA-00215 notes | "Unchanged for all inputs that used to terminate" is false: durations and gaps were taken modulo 24 h (domain M1) | fix | Notes and changelog text corrected (0153b932) |
| 1 | MINOR | `MpLocalTimeSlot.kt:157` | The KDoc left out the boundary results (domain M3) | fix | b12f920b |
| 1 | MINOR | `MpTimezone.kt`, `TimezoneEngineNative.kt:33` | `supportedIds` lacks current canonical ids, now uncached on native (impl 1, domain M4) | fix | Four ids added; `EST`, `HST`, `MST` stay out, java.time refuses them (9ebc12c0) |
| 1 | MINOR | `TimezoneEngineJs.kt:105` | JS caches grow with case variants of unlisted ids (impl 2, domain M6, security 2; older than the branch) | fix | Only listed ids are cached on JS (9ebc12c0) |
| 1 | MINOR | native | Spelling variants of a zone id resolve on native only (domain M5) | follow-up | ULTRA-00317, the maintainer: "yes follow up" |
| 1 | MINOR | `MpLocalTime.kt:11` | The public constructor stores an out-of-range value (security 3) | open | A breaking change; deserialised values are normalised; noted for the 24:00 work |
| 1 | NIT | `time_limit.jvm.kt` | The timed-out thread keeps running (impl NIT) | fix | Interrupts, KDoc says what remains (68f8b436) |

Verified after round 1 (the track): jvmTest 2543, jsBrowserTest 1906, linuxX64Test 1898, 0 failures.
