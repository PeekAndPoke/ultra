# MpLocalTimeSlot.splitWithGaps loops forever across midnight

**Status:** TODO
**Plan:** none — out-of-scope finding from the `/feature-review` of `docs/tasks-archive/2026-10/20261018-datetime-js-performance.md` (round 1, 2026-10-19)
**Security-critical:** yes, mildly (a caller-controlled duration hangs the thread and fills memory) → red-team note below

## Spec

**Pre-existing** (the old `%` in `MpLocalTime.ofMilliSeconds` wrapped the same way). `MpLocalTimeSlot.splitWithGaps`
(`ultra/datetime/src/commonMain/kotlin/MpLocalTimeSlot.kt:173-180`) steps `currentTo = currentFrom.plus(duration)`
while `currentTo <= to`. `MpLocalTime.plus` wraps at midnight, so a step that crosses midnight lands EARLIER and the
loop never ends: `MpLocalTimeSlot(08:00, 20:00).splitWithGaps(24.hours)` adds the same slot forever (OOM).
`duration + gap` that wraps past midnight does the same.

- [ ] Stop when a step would cross midnight (compare in milliseconds of day, or guard `currentTo > currentFrom`)
- [ ] Tests: `24.hours`, `25.hours`, a duration + gap that wraps, a slot ending at 23:59:59.999 — all terminate
- [ ] Red-team scenario (collect only): an endpoint that passes a user-chosen duration into `splitWithGaps`

## Implementation notes

## Test evidence

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|

**Verdict:**
