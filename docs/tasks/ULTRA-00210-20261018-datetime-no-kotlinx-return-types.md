# ULTRA-00210: ultra/datetime: no kotlinx types as return values

> **Status (2026-10-18): NOT STARTED.**
>
> **Part of:** ULTRA-00255 (Ultra libraries epic)

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

## Plan as built

Maintainer (2026-11-13): everything datetime returns is an Mp type; `MpMonth` and `MpDayOfWeek` are new; no backward
compatibility (no deprecated bridges); the converters to and from kotlinx and java.time stay and cover the new types.
Shipped together with ULTRA-00211.

Re-grepped 2026-11-13 (non-private declarations whose return type is a kotlinx type, converters excluded): exactly the
sites the ticket lists. `EpochMath`/`TimezoneEngine` are `internal`.

**Commits to (expensive to reverse marked ⚠):**

- ⚠ `enum class MpMonth { JANUARY … DECEMBER }` with `number` (1-12) and `MpMonth.of(number)`; ⚠ `enum class
  MpDayOfWeek { MONDAY … SUNDAY }` with `isoDayNumber` (1-7) and `MpDayOfWeek.of(isoDayNumber)`. Same entry names
  and order as kotlinx and java.time, so `name`, `ordinal` and `toString()` are unchanged for callers. Both
  `@Serializable` (by name, like any enum) so they can be fields.
- ⚠ `month: MpMonth` and `dayOfWeek: MpDayOfWeek` on `MpLocalDate`, `MpLocalDateTime`, `MpZonedDateTime`.
- ⚠ `MpTimezone.offsetAt(MpInstant): UtcOffset` and `TimeZone.offsetAt(MpInstant): UtcOffset` (`extensions.kt`) are
  removed: `offsetSecondsAt` / `offsetMillisAt` already return plain numbers. No new offset type.
- Converters, both ways: `Month.mp`, `MpMonth.kotlinx`, `DayOfWeek.mp`, `MpDayOfWeek.kotlinx` (common);
  `java.time.Month.mp`, `MpMonth.jvm`, `java.time.DayOfWeek.mp`, `MpDayOfWeek.jvm` (JVM).
- Inputs: every function that takes a kotlinx `Month` or `DayOfWeek` also takes the Mp type (`of(year, month, day)`,
  `atStartOfNext/Previous(dayOfWeek)`, `getDatesInMonth(weekdays)`). The kotlinx-typed inputs stay, as the ticket's
  rule allows and as every `TimeZone`/`MpTimezone` pair in the API already does.

**Decided here (the ticket left it open):** the names `MpMonth` / `MpDayOfWeek` (maintainer's names); the members
`number` / `isoDayNumber` (kotlinx's names); `of(...)` throws `IllegalArgumentException` outside 1-12 / 1-7 (no
sensible fallback for a month number); inputs keep the kotlinx overloads (above); `offsetAt` removed rather than given
a new type; `UtcOffset.totalMillis` (an extension on a kotlinx receiver returning `Long`) stays.

**Behaviour changes without a compile signal:** `date.month.equals(Month.APRIL)` (an explicit `equals` call, not
`==`) silently becomes `false`; anything that stores the value via `toString()`/`name` is unchanged.

## Implementation notes

## Test evidence

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|

**Verdict:**

## History

- 2026-10-21: numbered ULTRA-00210 (was `docs/tasks/20261018-datetime-no-kotlinx-return-types.md`).
