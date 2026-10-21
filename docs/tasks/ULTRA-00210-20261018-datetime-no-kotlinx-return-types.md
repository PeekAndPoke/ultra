# ULTRA-00210: ultra/datetime: no kotlinx types as return values

> **Status (2026-10-18): NOT STARTED.**

**Plan:** none — maintainer decision during the `/feature-review` of ULTRA-00209 (kotlinx datetime js timezones) (2026-10-18): "really eliminate the return of any kotlinx datetime types. This will be a breaking change but it is fine."
**Security-critical:** no

## Spec

Rule (maintainer, 2026-10-18): kotlinx types appear in ultra/datetime's public API only as INPUTS and in explicit
CONVERTERS (`conversionKotlinx.kt`, `MpTimezone.kotlinx`, `TimeZone.mp`). This task removes the remaining return values.
Breaking for consumers — accepted; the maintainer updates them (no third parties).

Known return sites (read 2026-10-18; re-grep before starting, treat as a lower bound):

- `Month` — `MpLocalDate.month`, `MpLocalDateTime.month`, `MpZonedDateTime.month`
- `DayOfWeek` — `.dayOfWeek` on the same three
- `UtcOffset` — `MpTimezone.offsetAt`, `TimeZone.offsetAt` (`extensions.kt`); `offsetSecondsAt` / `offsetMillisAt`
  already return plain numbers
- anything else a public-signature scan finds (a script did this once: list non-private declarations whose RETURN type
  is a kotlinx type)

- [ ] Own `MpMonth` / `MpDayOfWeek` (or similar), with converters both ways; decide the names with the maintainer
- [ ] Inputs keep accepting kotlinx `Month` / `DayOfWeek` (rule: inputs are fine) — or switch them too? decide
- [ ] `offsetAt` returns an ultra type or is removed in favour of `offsetSecondsAt`
- [ ] Consumer migration notes for the release

## Implementation notes

## Test evidence

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|

**Verdict:**

## History

- 2026-10-21: numbered ULTRA-00210 (was `docs/tasks/20261018-datetime-no-kotlinx-return-types.md`).
