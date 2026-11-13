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

- `MpLocalTimeSlot.splitWithGaps` now steps in milliseconds of day (`Long`) instead of `MpLocalTime.plus`, which
  wraps at midnight. A slot is emitted only while `durationMs <= endMs - currentFromMs`; the next step stops when the
  gap does not fit (`gapMs > endMs - currentToMs`). Differences rather than sums, because `Duration.INFINITE` is
  `Long.MAX_VALUE` ms.
- Found along the way, same hang: a duration under one millisecond (`500.microseconds`) passed the old
  `duration <= ZERO` guard but added 0 ms per step. The guard now checks whole milliseconds, so it yields no slots.
- A slot ending at 23:59:59.999 (`completeDay`) hung too: the last step wrapped to 00:00. Now its last slot is 22:00-23:00.
- Behaviour for all inputs that used to terminate is unchanged (the existing minute-table cases still pass).
- Tests are bounded by `withinTimeLimit` (expect/actual in the test sources): the JVM runs the block on a daemon thread
  and fails after the limit; JS and native call it directly.
- Red-team scenario (collect only, not executed): an endpoint that passes a caller-chosen duration into
  `splitWithGaps`. Not filed as its own ticket (track agents create none); the coordinator decides.

## Test evidence

- New cases in `MpLocalTimeSlotSpec` (commonTest, so JVM, JS and native): 24 h, 25 h, infinite and sub-millisecond
  durations, zero and negative duration, a step that fits exactly with the next one wrapping, a duration + gap that
  wraps, an infinite gap, a gap ending exactly at `to`, a negative and a zero gap, `completeDay` by 1 h and by 3 h + 3 h
  gap, an invalid slot.
- Before the fix (bounded run, JVM): 10 of the new cases failed with `OutOfMemoryError` from the endless list growth
  (2522 tests, 10 failures) — the others (zero/negative duration, infinite gap, invalid slot) already terminated.
- After the fix, from the XML: jvmTest 2522 tests / 0 failures; jsBrowserTest 1887 / 0; linuxX64Test 1875 / 0
  (`MpLocalTimeSlotSpec` 71 / 0 on each).
- Mutation-checked:
  - removed the whole-millisecond guard → red: zero, sub-millisecond and negative duration (+ the existing
    "duration < 1" case), 4 failures.
  - loop condition as a sum (`currentFromMs + durationMs <= endMs`) → red: infinite duration, 1 failure.
  - removed the gap `break` → stays green: an EQUIVALENT mutant. Without it the next loop check already sees a negative
    remainder, and an overflowing gap wraps to a negative remainder as well. Kept as the explicit guard.

## Changelog text

- `ultra/datetime`: `MpLocalTimeSlot.splitWithGaps` no longer loops forever when a step or a step plus gap crosses
  midnight (e.g. a duration of 24 h, or a slot ending at 23:59:59.999), nor on a duration under one millisecond; such
  steps now end the split (ULTRA-00215).

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|

**Verdict:**

## History

- 2026-10-21: numbered ULTRA-00215 (was `docs/tasks/20261019-mplocaltimeslot-splitwithgaps-loop.md`).
- 2026-11-13: implemented on track A (datetime) of ULTRA-00316; awaiting the review gate.
