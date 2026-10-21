# ULTRA-00211: MpZonedDateTime loses the offset in a DST overlap

**Status:** TODO
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

## Implementation notes

## Test evidence

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|

**Verdict:**

## History

- 2026-10-21: numbered ULTRA-00211 (was `docs/tasks/20261018-mpzoneddatetime-overlap-offset.md`).
