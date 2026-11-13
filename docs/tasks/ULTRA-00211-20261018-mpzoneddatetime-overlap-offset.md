# ULTRA-00211: MpZonedDateTime loses the offset in a DST overlap

> **Status (2026-10-18): NOT STARTED.**
>
> **Part of:** ULTRA-00255 (Ultra libraries epic)

**Plan:** none — out-of-scope finding from the `/feature-review` of ULTRA-00209 (kotlinx datetime js timezones) (round 1, 2026-10-18)
**Security-critical:** no — but persistence (mutation-check bar)

## Spec

**Pre-existing, not introduced by the zone-engine change** (kotlinx behaved the same before it). Found by two
reviewers independently.

`MpZonedDateTime` holds only `datetime` (local) + `timezone`; its instant is recomputed in the init
(`ultra/datetime/src/commonMain/kotlin/MpZonedDateTime.kt:151`, `datetime.toInstant(timezone)`) with the EARLIER
offset. An instant in the SECOND half of a fall-back overlap therefore cannot be represented:

- `MpInstant(1667093400000).atZone(Europe/Berlin)` (2022-10-30T02:30+01:00) → `toInstant()` = `1667089800000`
  (02:30+02:00), one hour early.
- Every decode path takes that route: `MpZonedDateTimeSerializer.deserialize`, slumber `MpZonedDateTimeAwaker`
  (`ultra/slumber/src/jvmMain/kotlin/builtin/datetime/mp/MpZonedDateTimeCodec.kt`), `ZonedDateTime.mp`
  (`ultra/datetime/src/jvmMain/kotlin/conversion.kt`). A load + re-save writes the wrong `ts` — silent data change.
- So the engine's "preferred offset" in date-based arithmetic is observable on `MpInstant.plus` but lost again by
  `MpZonedDateTime.plus`.

Related, same root (MINOR): `MpZonedDateTime.atStartOfDay()` is `copy(datetime = midnight)` — gap-shift semantics —
while `MpLocalDate.atStartOfDay(tz)` uses java.time's end-of-gap rule. Toronto 1919-03-31: 01:00 vs 00:30; and in
zones whose gap starts at midnight the `datetime` keeps a non-existent 00:00.

Design fork for the maintainer: store the instant (or the offset) in `MpZonedDateTime` and derive the local fields,
like java's `ZonedDateTime`, vs. keep local+zone and add an offset field. Either changes `equals`/`copy` semantics
and possibly the wire format (`MpDateTimeRawData` already carries `ts`, so decode could keep the instant).

- [ ] Maintainer picks the representation
- [ ] Overlap round-trip test through every decode path above, mutation-checked
- [ ] `MpZonedDateTime.atStartOfDay()` agrees with `MpLocalDate.atStartOfDay(tz)` (Toronto 1919 row)

## Plan as built

Maintainer (2026-11-13): the instant is the truth; the stored and wire format do not change; round trips through every
serializer give back the same instant for both sides of an overlap and for a gap (mutation-checked, persistence path).
Shipped together with ULTRA-00210.

**Commits to (expensive to reverse marked ⚠):**

- ⚠ `MpZonedDateTime` holds `(instant, timezone)`; `datetime` (and every local field) is derived from them once, at
  construction. It stays a `data class`, but with a **private** constructor (`@ConsistentCopyVisibility`), so:
  - ⚠ the public constructor `MpZonedDateTime(datetime, timezone)`, `copy(...)` and `component1/2` are gone (no repo
    caller of the constructor; `copy` and destructuring callers, if any, show up at compile time);
  - ⚠ `equals`/`hashCode` compare instant + zone, not local + zone: the two sides of an overlap are no longer equal;
    two values built from a gap time and from the time it resolves to are now equal.
- Builders: `MpInstant.atZone(zone)` keeps the instant exactly. `MpZonedDateTime.of(datetime, zone)` and
  `MpLocalDateTime.atZone(zone)` resolve a local time as today: an overlap takes the earlier offset, a gap moves
  forward by its length, and `datetime` is then the resolved wall time (a gap's 02:30 reads back as 03:30).
- Decode paths read `ts` as the instant: `MpZonedDateTimeSerializer`, `ZonedDateTime.mp`, `MpZonedDateTime.jvm`
  (`ZonedDateTime.ofInstant`), and slumber's `MpZonedDateTimeAwaker` (`ultra/slumber`, outside this module: a one-line
  change the maintainer's round-trip decision requires).
- Wire format unchanged: `{ts, timezone, human}`; `human` (`toIsoString()`) still carries no offset, so both sides of an
  overlap render the same `human` (debug-only, never read back).
- `atStartOfDay()` = `MpLocalDate.atStartOfDay(zone)` (java.time's end-of-gap rule; Toronto 1919 row).
  `atStartOfHour/Minute/Second()` and `atTime()` keep the current offset when it is still valid (java's
  `truncatedTo`/`with` rule), so the second half of an overlap truncates within its own half.
  `atStartOfMonth/Year()` resolve like `of`.

**Decided here (the ticket left it open):** keep `data class` with a private constructor rather than a plain class
(generated `equals`/`hashCode` on the two stored fields); `atStartOfHour/Minute/Second` and `atTime` keep the offset
like java.time.

**Not changed:** `compareTo` (already by instant), `toIsoString()` format, `MpZonedDateTimeRange` API.

## Implementation notes

## Test evidence

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|

**Verdict:**

## History

- 2026-10-21: numbered ULTRA-00211 (was `docs/tasks/20261018-mpzoneddatetime-overlap-offset.md`).
