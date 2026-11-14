# ULTRA-00211: MpZonedDateTime loses the offset in a DST overlap

> **Status (2026-11-13): IMPLEMENTED, awaiting the review gate** (track A of ULTRA-00316, batch 2, with ULTRA-00210).
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

- [x] Maintainer picks the representation: the instant is the truth (2026-11-13)
- [x] Overlap round-trip test through every decode path above, mutation-checked
- [x] `MpZonedDateTime.atStartOfDay()` agrees with `MpLocalDate.atStartOfDay(tz)` (Toronto 1919 row)

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
    two values built from a gap time and from the time it resolves to are equal under either rule, because `datetime`
    is derived from the instant (corrected in review round 1).
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

**Not changed:** `toIsoString()` format, `MpZonedDateTimeRange` API. (`compareTo` was planned unchanged; the maintainer
decided a tie-break on 2026-11-14, see the implementation notes.)

## Implementation notes

- `MpZonedDateTime` (`ultra/datetime/src/commonMain/kotlin/MpZonedDateTime.kt`) is a plain `class` with a private
  constructor `(instant: MpInstant, timezone: MpTimezone)`; `datetime` is derived once at construction
  (`TimezoneEngine.toLocalDateTime`). Built only by `of(datetime, timezone)` (local resolution, unchanged rules),
  the internal `of(instant, timezone)` (used by `MpInstant.atZone`) and the private `withLocal` (the offset-keeping
  operations; `ofLocal` was folded into it in review round 1).
- **Equality lives in one place**: the explicit `equals` / `hashCode` in `MpZonedDateTime` (instant + zone).
  **Confirmed by the maintainer (2026-11-14)**: they stay instant + zone. They are pinned by two tests (`MpZonedDateTimeOverlapSpec`, "the two sides are different
  values…" and "equality: instant and zone decide…"). **The equality choice only decides the two sides of an
  overlap**: a value built from a gap time equals the value at its resolved instant under BOTH choices, because
  `datetime` is derived from the instant (corrected in review round 1).
- `isSameInstant(other: MpAbsoluteDateTime)` (maintainer, 2026-11-14): a default method on the interface
  `MpAbsoluteDateTime` (`toInstant() == other.toInstant()`), so it works for `MpInstant` and `MpZonedDateTime` and
  across the two. (`compareTo` got the tie-break later the same day, see below.)
  New `MpAbsoluteDateTimeSameInstantSpec` (commonTest, 4 tests: 09:00 Berlin / 08:00 London same instant, not equal;
  the overlap halves are not the same instant; `MpInstant` vs `MpZonedDateTime` both directions; a value against
  itself). RED first as a compile failure (new API). Mutants: `isSameInstant` = `this == other` → red 2;
  = `true` → red 2.
- Plain class rather than the planned data class with a private constructor: explicit `equals`/`hashCode` make that
  one place visible, and there is no `copy` to keep private.
- Decode paths read `ts` as the instant: `MpZonedDateTimeSerializer.deserialize`, slumber `MpZonedDateTimeAwaker`
  (`ultra/slumber`), `ZonedDateTime.mp` and `MpZonedDateTime.jvm`. The two JVM converters go by epoch second +
  nanos (exact over the whole range; through epoch millis they clamped or threw beyond about ±292 million years), with
  the nanos cut to whole milliseconds in both directions (maintainer, 2026-11-14: "Truncate MpZonedDateTime to
  milliseconds please to avoid confusion"). With a floored second and nanos in 0..999,999,999 the cut is a floor, as
  `toEpochMilli()` was (pinned: 1 ns before the epoch gives -1 ms).
- `atStartOfDay()` delegates to `MpLocalDate.atStartOfDay(timezone)`; `atStartOfMonth()`/`atStartOfYear()` take the
  start of their first day the same way (decided here: the plan said "resolve like `of`"; the start-of-day rule is the
  consistent one when midnight falls into a gap). `atStartOfHour/Minute/Second()` and `atTime()` keep the current offset
  where it is valid (`withLocal`, with the current offset as the preferred one).
- Call sites adapted outside the module (mechanical, no behaviour change):
  `kraft/semanticui/src/jsMain/kotlin/forms/field_input.kt` 692, 719, 765, 792: `x.copy(timezone = tz)` →
  `MpZonedDateTime.of(x.datetime, tz)` (what `copy` did: keep the local time, resolve it in the new zone).
- Behaviour fix outside the module (maintainer-approved): `ultra/slumber/.../mp/MpZonedDateTimeCodec.kt`, the awaker
  builds from `ts` with `atZone`.
- Apia loop (maintainer, 2026-11-14, "yes fix it now"): `atStartOfNext` / `atStartOfPrevious` (both overloads; the
  kotlinx ones delegate) step on the date: `toLocalDate().atStartOfNext/Previous(day).atStartOfDay(timezone)`. The
  `MpLocalDate` variants have the same meaning for the starting day (next: strictly after today; previous: today
  included), so the ordinary results are unchanged: 27 Europe/Berlin rows across both 2022 DST switches, computed with
  the old code by a throwaway probe and pinned (`MpZonedDateTimeWeekdaySpec`). On a date the zone skips the result is
  that date's `atStartOfDay`, i.e. the start of the day after it: from Apia 2011-12-31 the previous FRIDAY is
  2011-12-31T00:00+14:00 (a Saturday); from 2011-12-29 the next FRIDAY is the same (the old loop skipped to 2012-01-06).
  RED first: 7 of 38 on the JVM (6 by the 5 s time limit, "next FRIDAY" with the old 2012-01-06). After: 38 / 0 on
  JVM, JS and native. Mutant: the old `atStartOfPrevious` loop restored → red 6, all "Did not finish within 5s".
- `compareTo` tie-break (maintainer, 2026-11-14, "yes to tie-break"): instant, then local date-time, then zone id,
  like java.time's `ChronoZonedDateTime`, so `compareTo == 0` exactly when `equals`. `compareTo` is declared against
  `MpZonedDateTime` only (`ComparableTo<MpZonedDateTime>`); `MpInstant` has its own `ComparableTo<MpInstant>`, and no
  comparison between the two exists, so nothing about comparing with an `MpInstant` changes.
  Callers checked — what changes meaning, only for two values at the same instant in different zones:
  - `a <= b && a >= b` and `ComparableTo.isEqualTo` no longer mean "same instant"; `isSameInstant` does.
  - `MpZonedDateTimeRange`: its predicates compared zoned endpoints with `<`, `<=`, `in`, `minOf`/`maxOf` and so
    picked up the tie-break (delta round 3, MAJOR). Fixed: the range compares its endpoints by instant everywhere
    (`hasStart`, `hasEnd`, `isOpen`, `isValid`, `compareTo(period)`, `contains`, `intersects`, `touches`, `mergeWith`,
    which keeps this range's endpoint on a tie as `minOf`/`maxOf` did), so every result is what it was before the
    tie-break. `isAdjacentTo` uses `==` (instant + zone), unchanged; `cutAway` already went through `MpInstantRange`.
  - `DateTimeRangeConverter` `create`: clamps by instant; on a tie Genesis / Doomsday stay, as before (fixed in delta
    round 3; it means "by instant" on purpose).
  - Not affected: `MpInstantRange` (instants), Kronos, kraft and funktor (no comparisons of zoned values found).
  - Two tests pinned the old meaning (`MpZonedDateTimeSpec` "Construction - parse(isoString, timezone)", 6 lines, and
    "Comparison", 1 line: `shouldBeEqualComparingTo` across zones); adapted to a `shouldBeSameInstantAs` helper.
  New `MpZonedDateTimeCompareSpec` (commonTest, 4) and `MpZonedDateTimeCompareJvmSpec` (`sortedSetOf`, 1): RED first
  5 of 5. After: jvmTest 2730 / 0, jsBrowserTest 2076 / 0, linuxX64Test 2068 / 0. Mutants: zone-id step dropped → red
  3 (Berlin/Paris, mixed sort, compareTo-equals table); local step dropped → red 2 (UTC/Berlin, mixed sort).

### Docs lines for the DOCS follow-up

- `docs-site/src/pages/ultra/datetime/js-timezones.astro` 31-36: still true, but add that an `MpZonedDateTime` built
  from an instant keeps it, so both sides of an overlap are distinct values (`MpInstant.atZone`), and that a time built
  from a gap reads back as the resolved time (02:30 → 03:30 in `.hour`/`.datetime`).
- `docs-site/src/pages/ultra/datetime/mp-zoned-date-time.astro`: two lines are false, and were before this change
  (corrected in delta round 2; my first list said none): line 41 shows
  `MpZonedDateTime.parse("2025-06-15T14:00:00+02:00[Europe/Berlin]")`, which the parser refuses (probe: "Could not parse
  MpZonedDateTime from '…'"); lines 155-156 show `toIsoString()` as `"2025-06-15T14:00:00.000+02:00[Europe/Berlin]"`, but
  it never carried the offset (probe: `2025-06-15T14:00:00.000[Europe/Berlin]`). A constructor or `copy` is not shown
  anywhere; worth a line that `MpZonedDateTime` is built with `atZone` / `of`, and that `toString()` now shows the offset.
- `docs-site/src/data/llms/datetime.md`: no line becomes false.

## Test evidence

- `MpZonedDateTimeOverlapSpec` (commonTest, JVM/JS/native; Berlin 2022-10-30 both halves of 02:30, 2022-03-27 02:30
  gap, Toronto 1919-03-31): RED first, 9 of 14 on each platform (second half collapsed onto the first, sides equal,
  +1 h collapsed back, truncation in the wrong half, gap kept 02:30, Toronto 01:00 instead of 00:30). Two more tests
  added with the fix (offset kept through `atStartOfMinute/Second`/`atTime`; equality and hash codes): 16 / 0.
- `ZonedDateTimeOverlapJvmSpec`: RED 2 of 6 (`ZonedDateTime.mp`, `.jvm`, second half); 6 / 0.
- `ultra/slumber` `MpZonedDateTimeOverlapCodecSpec`: RED 1 of 3 (second half); 3 / 0.
- After the first build: datetime jvmTest 2660 / 0, jsBrowserTest 2010 / 0, linuxX64Test 2002 / 0; slumber jvmTest
  1252 / 0.
- After delta round 2 (Apia fix, tie-break, `isSameInstant`, millisecond converters, sign pins): JVM (jvmTest)
  2733 / 0, JS (jsBrowserTest) 2077 / 0, native (linuxX64Test) 2069 / 0.
- Final run on the tree after delta round 4 (2026-11-14 15:21, `console/wt-gradle.sh` with `--rerun` on each test
  task): JVM (datetime jvmTest) 2760 / 0, JS (datetime jsBrowserTest) 2104 / 0, native (datetime linuxX64Test)
  2096 / 0, slumber jvmTest 1254 / 0.
- Delta round 2 mutants: `ZonedDateTime.mp` keeps the nanos → red 4 (range rows, cut test, floor pin); `.jvm`
  keeps the nanos → red 2 (three fraction digits, floor pin); `toString` sign always `+` → red 1 (New York -05:00 and
  local mean time -04:56:02, checked on JVM, JS and native: all three agree on -04:56:02).
- Mutation-checked (JVM; each restored with `cmp`):
  - serializer decodes via the local time → red 1 (kotlinx round trip, second half);
  - slumber awaker via the local time → red 1 (slumber round trip, second half);
  - `ZonedDateTime.mp` via the local time → red 1; `MpZonedDateTime.jvm` via `ZonedDateTime.of(local)` → red 1;
  - `MpInstant.atZone` via the local time → red 9;
  - `equals` by local time + zone → red 2 (sides unequal, equality test);
  - offset-keeping dropped (`withLocal` = `of`) → red 2 (hour truncation, minute/second/atTime);
  - `atStartOfDay` via `of(midnight)` → red 1 (Toronto).

## Changelog text

- `ultra/datetime`, behaviour change: `MpZonedDateTime` keeps its instant (ULTRA-00211). Both sides of a DST overlap
  are now distinct values: `MpInstant.atZone`, the kotlinx serializer, the slumber codec, `ZonedDateTime.mp` and
  `MpZonedDateTime.jvm` used to move the second half one hour back (e.g. Berlin 2022-10-30T02:30+01:00 became +02:00),
  and a load + save wrote the wrong `ts`. The stored and wire format are unchanged.
- `ultra/datetime`, behaviour change (no compile signal): `MpZonedDateTime` equality compares instant and zone, no
  longer local time and zone, so the two sides of a DST overlap are no longer equal. To compare by point in time,
  whatever the zone, use `isSameInstant` (ULTRA-00211).
- `ultra/datetime`, behaviour change (no compile signal): `MpZonedDateTime.compareTo` orders by instant, then by local
  date-time, then by zone id (like java.time), so it is `0` exactly when `equals`. For two values at the same instant
  in different zones, `a <= b && a >= b` and `isEqualTo` no longer mean "same instant" (use `isSameInstant`), a
  `sortedSetOf` keeps both, and `minOf` / `maxOf` / sorting order them by local time and zone.
  `MpZonedDateTimeRange` compares its endpoints by instant in every ordering predicate, unchanged; `isAdjacentTo` and
  `equals` go by equality, as before. kraft's generic comparable form rules
  (`inRange`, `greaterThanOrEqual`, …) on `MpZonedDateTime` bounds in another zone now decide the exact boundary instant
  by local time and zone (ULTRA-00211).
- `ultra/datetime`, new API: `MpAbsoluteDateTime.isSameInstant(other: MpAbsoluteDateTime)`, true when both are the
  same point in time, for `MpInstant` and `MpZonedDateTime` and across the two (ULTRA-00211).
- `ultra/datetime`, behaviour change (no compile signal): a time in a DST gap now reads back as the resolved time:
  `MpZonedDateTime.of(2022-03-27T02:30, Berlin).hour` is 3, not 2 (its instant was already 03:30). So a value built
  from a gap time now equals the value at its resolved instant (ULTRA-00211).
- `ultra/datetime`, behaviour change (no compile signal): `MpZonedDateTime.toString()` shows the offset, e.g.
  `MpZonedDateTime(2022-10-30T02:30:00.000+01:00[Europe/Berlin])`, so the two sides of an overlap print differently.
  `toIsoString()`, the stored `human` field and the wire format are unchanged (ULTRA-00211).
- `ultra/datetime` (JVM), behaviour change (no compile signal): `MpZonedDateTime.jvm` and `ZonedDateTime.mp` are exact
  over the whole range: beyond about ±292 million years `.jvm` clamped and `.mp` threw `ArithmeticException`. Both stay
  at millisecond precision (ULTRA-00211).
- `ultra/datetime`, behaviour change (no compile signal): `MpZonedDateTime.atStartOfDay()`, `atStartOfMonth()` and
  `atStartOfYear()` follow `MpLocalDate.atStartOfDay(zone)` (java.time): when midnight falls into a gap the day starts at
  the end of the gap (Toronto 1919-03-31: 00:30, was 01:00). `atStartOfHour/Minute/Second()` and `atTime()` keep the
  current offset in an overlap (ULTRA-00211).
- `ultra/datetime`, fix: `MpZonedDateTime.atStartOfPrevious` no longer loops forever when the previous day does not
  exist in the zone (Pacific/Apia and Pacific/Fakaofo skipped 2011-12-30). `atStartOfNext` and `atStartOfPrevious` step
  on the date; on a skipped date they return the start of the day after it, like `atStartOfDay` (from Apia 2011-12-29,
  the next FRIDAY is 2011-12-31T00:00+14:00, a Saturday; it was 2012-01-06; Pacific/Kiritimati skipped 1994-12-31 the
  same way). Second fixed case: where a DST gap ends at or crosses midnight the old loop skipped a whole week
  (America/Nuuk 2024-03-29T23:30, next SATURDAY: was 2024-04-06, now 2024-03-30T00:00-02:00). Ordinary results are
  unchanged (ULTRA-00211).
- **Before you update** (removed API): the public constructor `MpZonedDateTime(datetime, timezone)` — use
  `MpZonedDateTime.of(datetime, timezone)` or `instant.atZone(timezone)`; `MpZonedDateTime.copy(...)` — e.g.
  `x.copy(timezone = tz)` becomes `MpZonedDateTime.of(x.datetime, tz)`; destructuring (`component1/2`)
  (ULTRA-00211).

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR | `MpZonedDateTime.kt:49-57` | `internal fun ofLocal` has one caller | FIXED | folded into `withLocal`; mutant "no preferred offset" in `withLocal` → red 2 |
| 1 | MINOR | `MpZonedDateTime.kt:246` | fully qualified `kotlin.math.abs` | FIXED | imported |
| 1 | MINOR | `MpZonedDateTime.toString()` | the two sides of an overlap print the same | FIXED | offset added (`+01:00`, `+hh:mm:ss` with seconds); `toIsoString()`/`human`/wire unchanged. RED first: 2 tests (Berlin halves; UTC + Berlin LMT 1890 `+00:53:28`). Repo pins adapted: `MpZonedDateTimeSpec.kt:155` only. Mutants: offset dropped → red 3; seconds dropped → red 1 |
| 1 | MINOR | `jvmMain/conversion.kt:96-107` | `.jvm` / `.mp` go through epoch millis: clamp / `ArithmeticException` beyond ±292 M years | FIXED | by epoch second + nanos both ways. RED first: ±10^16 s (year ±317 M) and nanos, 3 tests. Mutants: either side back to millis → red 3 |
| 1 | MINOR | `MpZonedDateTime.kt:349` | `atStartOfHour()` into a gap can be later than the receiver | FIXED (KDoc + pin) | Toronto 1919-03-31T00:45 → 01:00, pinned as current behaviour |
| 1 | MINOR | ticket | gap equality attributed to the equality choice | FIXED | it holds under both choices (`datetime` derives from the instant); changelog and notes corrected |
| 1 | – | `MpZonedDateTime.kt:397-406` | `atStartOfPrevious` loops forever in Pacific/Apia (2011-12-30 skipped) | CONFIRMED, FIXED 2026-11-14 (maintainer: "yes fix it now") | probe (bounded copy + real call under a 5 s limit): from 2011-12-31 (Sat) `minus(1 DAY).atStartOfDay()` resolves back to 12-31 00:00+14:00 every step; real call "Did not finish within 5s" (JVM); bounded copy identical on JS. Pre-existing. `atStartOfNext` terminates (moves forward: 2012-01-06); `MpLocalDate` variants terminate (2011-12-30 exists as a date) |
| 1 | – | `compareTo` vs `equals` | `sortedSetOf(t.atZone(UTC), t.atZone(Berlin))` keeps one | CONFIRMED; tie-break decided 2026-11-14 (maintainer: "yes to tie-break"), built in delta round 2 | probe: `compareTo` 0, `equals` false, sorted set 1, hash set 2 |
| 1 | – | slumber `MpZonedDateTimeCodec.kt:25` | any Number is a `ts` | CONFIRMED, no change | probe: `NaN` and `-0.7` → 1970-01-01T00:00Z, `Infinity` and `1.5e30` → clamped to +292278994-08-17 |

Stayed CLEAN (round 1, blind, reviewer-opus-high; implementation, domain and security charters all CLEAN, no
CRITICAL/MAJOR): no construction path loses the instant; no remaining caller of the removed API in the repo; old
stored documents decode as before; `human` is never read on decode; no production code uses an `MpZonedDateTime` as a
map, set or cache key; the test arithmetic was recomputed by the domain reviewer.

| 2 (delta) | MINOR | `MpZonedDateTime.toString()` | the offset sign has no test; a sign-always-`+` mutant survives | FIXED | New York winter `-05:00` and local mean time before 1883 `-04:56:02`, same on JVM, JS and native; the sign mutant → red 1 |
| 2 (delta) | MINOR | `ZonedDateTimeOverlapJvmSpec.kt:25-29` | title says "to the nanosecond", the test checks millis | FIXED | dropped from the title |
| 2 (delta) | MINOR | `MpZonedDateTimeOverlapSpec.kt:81` | comment "pending the maintainer" stale | FIXED | "confirmed 2026-11-14" |
| 2 (delta) | MINOR | this ticket | notes show the millis converter paths, name `ofLocal`, counts from before the delta | FIXED | notes and test evidence updated, totals per platform |
| 2 (delta) | MINOR | changelog | the JVM-conversion entry named only one direction of the precision change | CLOSED by the maintainer's decision | both converters stay at millisecond precision; the entry says exact range, millisecond precision |
| 2 (delta) | MINOR | `ZonedDateTime.mp` / `.jvm` | sub-millisecond precision crosses the converters (`zoned.jvm.toInstant() != zoned.toInstant().jvm`) | CLOSED by the maintainer's decision (2026-11-14) | both cut to whole milliseconds; tests: cut in both directions, `isSameInstant` with `toInstant().mp`, floor before 1970; mutants red |
| 2 (delta) | MINOR | DOCS list | `mp-zoned-date-time.astro` 41 and 155-156 false | FIXED (listed) | verified by reading and by a probe; both false before this change |
| 2 (delta) | MINOR | test evidence | totals after the delta per platform | FIXED | above |

Delta round 2 (reviewer-fable-medium): implementation CLEAN (4 MINOR), domain and data CLEAN (6 MINOR); outcomes in
the rows above. `MpInstant` itself is unchanged: `MpInstant.now()` carries sub-millisecond precision on the JVM and
native and whole milliseconds on JS (probe, 200 samples each), for the maintainer to decide separately.

| 3 (delta) | MAJOR | `MpZonedDateTimeRange.kt` 74-90, 145-191 | predicates compare zoned endpoints and pick up the tie-break | FIXED | by instant everywhere; RED first 12 of 13 (+ slumber 1 of 1); not RED: `beginningAt(x).atZone(Berlin)` (Berlin's local time at Doomsday is later than UTC's, right by accident). Added `compareTo(period)` across zones and `mergeWith` keeping this range's endpoint on a tie. Mutants: `isValid` zoned → red 1; `touches` zoned → red 1; `hasEnd` zoned → red 5 |
| 3 (delta) | MINOR | changelog | "every ordinary result is unchanged" false where a DST gap ends at or crosses midnight | FIXED | Nuuk 2024-03-29T23:30 next SATURDAY: old loop 2024-04-06 (confirmed by a probe), new 2024-03-30T00:00-02:00; pinned; changelog names it |
| 3 (delta) | MINOR | weekday spec | no row for midnight in a gap or a repeated day outside Apia 2011 | FIXED | Toronto 1919 (next/previous MONDAY → 00:30), Sao Paulo 2018 (midnight gap), Apia 1892-07-04 (repeated day) pinned. The `of(date.atTime(Min), zone)` mutant dies on the two Toronto rows (01:00 instead of 00:30); on Sao Paulo and Apia 1892 it gives the same result as `atStartOfDay`, so those rows cannot kill it |
| 3 (delta) | MINOR | weekday spec | no Kiritimati row (1994-12-31 skipped) | FIXED | previous SATURDAY / FRIDAY from 1995-01-01 (old loop never ended), next SATURDAY from 1994-12-30 (old 1995-01-07) |
| 3 (delta) | – | `DateTimeRangeConverter.kt:40-41` | (found in the module search) `maxOf(Genesis, from)` / `minOf(Doomsday, to)` on zoned values | FIXED | by instant, Genesis / Doomsday kept on a tie as before; RED first (the tie kept `from`) |

| 4 (delta) | MINOR | `MpZonedDateTimeRange.kt:153` | mutant: `contains(range)` `to` half back on zoned values survives | FIXED (test) | `[10:00Z, 12:00Z]` UTC contains `[11:00Z, 12:00Z]` Berlin; the mutant → red 1 |
| 4 (delta) | MINOR | `DateTimeRangeConverter.kt:43` | mutant: `to` clamp back to `minOf(Doomsday, to)` survives | FIXED (test) | zone `-01:00`, to 9999-12-31T23:00 (Doomsday's instant) → Doomsday; the mutant → red 1 |
| 4 (delta) | MINOR | `MpZonedDateTimeRange.kt:197` | mutant: `mergeWith` not touching `sortedBy { it.from }` survives | FIXED (test) | `[10:00Z Berlin, 05:00Z]` and `[10:00Z UTC, 20:00Z]` → `[this, other]`; the mutant → red 1 |
| 4 (delta) | MINOR | `isAdjacentTo`, spec header, changelog | texts overstate "no predicate depends on the zone"; `isAdjacentTo` goes by equality | FIXED (texts + pin) | behaviour unchanged from master, the maintainer decides; pinned as current behaviour (one instant in two zones: touches, not adjacent); KDoc, spec header and changelog sentence corrected |

Delta round 4 (reviewer-fable-xhigh, all charters): CLEAN, 0 CRITICAL, 0 MAJOR, 4 MINOR; this batch changes tests and
texts only (the `isAdjacentTo` KDoc is the one production-file edit, a comment).

Delta round 3 (reviewer-fable-medium, all charters): NOT CLEAN, 1 MAJOR, 3 MINOR; all fixed above. Stayed clean: the
Apia fix, `compareTo` itself, the JVM converters. Module search for other comparisons of zoned values: `DateTimeRangeConverter`
(by instant on purpose, fixed); `MpInstantRange` (instants); `MpLocalDateRange` and `MpLocalTimeSlot` (local types);
Kronos (none); `MpZonedDateTimeRange.formatDdMmmYyyyHhMm` uses `==` of `atStartOfDay()` (full equality on purpose:
same day in the same zone).

**Verdict:** round 1 CLEAN (no CRITICAL, no MAJOR); MINORs applied above. Delta round 2 CLEAN, MINORs applied. Delta
round 3 NOT CLEAN (1 MAJOR), fixed; awaits the next round. Equality by instant + zone confirmed by
the maintainer (2026-11-14); `isSameInstant` added on his word.

## History

- 2026-10-21: numbered ULTRA-00211 (was `docs/tasks/20261018-mpzoneddatetime-overlap-offset.md`).
- 2026-11-13: implemented on track A, batch 2 (with ULTRA-00210); equality by instant + zone awaits the maintainer.
- 2026-11-14: review round 1 CLEAN, MINORs applied; equality by instant + zone confirmed; `isSameInstant` added.
