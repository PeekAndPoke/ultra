# ULTRA-00294: Slumber: the date awakers return null on input they cannot read, so a malformed date in a request body is a 400

> **Status (2026-11-06): DONE.** gate PASS after round 1. The maintainer, 2026-11-06: "We have to catch the exception and report a 400. Where exactly would we do this conversion of the exception? New ticket and tackle it right away."
**Security-critical:** no (leaks nothing; a 500 instead of a 400). Red team: none for a fix (maintainer, 2026-11-06:
"no ticket for the fix itself only the underlying feature"); the scenario goes into ULTRA-00058 (error disclosure).

## Why this exists

Found in the ULTRA-00292 gate (finding 1.2). Some of Slumber's date awakers THROW on input they cannot read instead of
returning null, as every other awaker does. The throw is neither an `AwakerException` nor a constructor's `require`,
so `SlumberRestCodec` passes it on and a client's malformed date becomes a 500:

| Awaker | Input | Thrown |
|---|---|---|
| `MpLocalDateAwaker`, `MpLocalDateTimeAwaker` | `timezone` missing / not a string; an unknown zone | NPE / `ClassCastException`; `IllegalTimeZoneException` |
| `MpZonedDateTimeAwaker` | the same, even before `ts` is read | the same |
| `ZonedDateTimeAwaker` | an unknown zone id | `ZoneRulesException` / `DateTimeException` |
| kotlinx `LocalDateAwaker`, `LocalDateTimeAwaker` | an unknown zone id (a missing one defaults to UTC) | `IllegalTimeZoneException` |

## Where the conversion happens

**In the awakers, at the source.** An awaker's contract is to return null for data it cannot read. Slumber then
raises the `AwakerException` naming the failing path ("Value at path 'root.from' must not be null"), and
`SlumberRestCodec` already turns that into a 400 with a safe message (ULTRA-00292). Nothing changes in funktor-rest.

Not chosen: widening the catch in `SlumberRestCodec.awakeBody` to any exception. The same awakers read the
DATABASE, and a bug there must stay a 500; a blanket catch would also hide real server bugs behind a 400 (the
ULTRA-00292 gate rejected that for the same reason).

## Decide before implementing

Nothing new. Behaviour change, deliberately: such a value now reads like any other value Slumber cannot read. A
NULLABLE field reads null and a field WITH A DEFAULT takes its default (`DataClassAwaker`'s optional-parameter
fallback), instead of failing the whole object; any other field fails with `AwakerException` instead of an NPE or
`ClassCastException`. Valid input reads exactly as before. (Gate round 1, MINOR from two charters: the first version
of this text said every non-null field fails.)

Out of scope, for the maintainer: `MpTimezoneAwaker` accepts ANY string; an unknown zone fails later, at the first
calculation (a 500 in a handler). Validating it would make stored records with such a zone unreadable, which today
load fine.

## The work

- [x] One helper per zone kind in `builtin/datetime/common.kt`: the zone the entry names, or null for a missing,
      non-string or unknown one.
- [x] `MpLocalDate`, `MpLocalDateTime`, `MpZonedDateTime`, `ZonedDateTime` and the kotlinx `LocalDate` /
      `LocalDateTime` awakers use them; the `TODO(scan)` notes go.

## Acceptance

- [x] For every date type Slumber knows, a table of malformed inputs (zone missing, of the wrong type, unknown; `ts`
      missing, a string, extreme numbers) never throws anything but `AwakerException`; a nullable field reads null.
- [x] Through `SlumberRestCodec`, a body with such a date is a `CouldNotConvertException` naming the path, not the value.
- [x] Existing codec specs stay green (valid input unchanged); mutation-checked (codecs).

## Implementation notes

- `builtin/datetime/common.kt`: `javaZoneOrNull` (`ZoneId.of`, catching `DateTimeException`), `kotlinxZoneOrNull`
  (`TimeZone.of`, catching `IllegalTimeZoneException`), `mpTimezoneOrNull` (`MpTimezone.of`, then checked against
  kotlinx, which every Mp zone calculation goes through; "Z" and "SYSTEM" keep their meaning).
- The Mp awakers read the zone first and return null without one; the kotlinx awakers keep defaulting a missing or
  non-string zone to UTC (documented) and return null for an unknown one.
- The path in the answer: "Value at path 'root.at' must not be null" for a field `at`.
- An e2e through a real endpoint is not added: `RequestBodyErrorsSpec` (ULTRA-00292) pins that a
  `CouldNotConvertException` from a body is a 400 end to end, and `SlumberRestCodecSpec` pins that a malformed
  date is one.

## Test evidence

- `DateAwakersMalformedInputSpec` (new, Slumber): 11 map-reading date types plus `LocalTime`, `MpLocalTime`, `ZoneId`,
  `MpTimezone` against a table of malformed inputs; refused at the root and in a non-null field, null in a nullable
  one; never any exception but `AwakerException`, extreme `ts` included.
- `SlumberRestCodecSpec`: a zone unknown (a marker string), a number, or missing: `CouldNotConvertException` naming
  'root.at', not the value.
- Slumber JVM 1249/0, funktor-rest 122/0, funktor-auth 293/0, funktor-all 257/0 (both backends), 2026-11-06.
- Mutants, each red for the intended reason: P1 Mp zone not validated, P2/P6 the unsafe cast back in
  `MpLocalDate`/`MpLocalDateTime`, P3 the same in `MpZonedDateTime` (red in Slumber AND the REST spec, which saw the
  raw `IllegalTimeZoneException`), P4 the java zone exception passes, P5 an unknown kotlinx zone read as UTC.

## Review record — the LEDGER

Round 1 (level 1, `reviewer-high` ×3, 2026-11-06), one gate for ULTRA-00294, ULTRA-00295 and the cap-12 commit
a9c292b1 (ULTRA-00291 finding 1.8). All three charters CLEAN.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR (domain, security) | `CHANGELOG.md`, ULTRA-00294 "Decide" | Says a non-null date field fails; one WITH A DEFAULT takes its default (`DataClassAwaker.kt:87-108`) | fixed | Verified in the code; both texts now name null, default and failure |
| 1 | MINOR (domain) | `RealmTokenConfig.kt:51`, `CHANGELOG.md` | "12 keeps every mailed link" holds only while concurrent sign-ups cannot win one re-send slot twice | fixed (wording) | The race is ULTRA-00130, next in line; KDoc and CHANGELOG qualify the claim and name it |
| 1 | NIT (impl) | `DateAwakersMalformedInputSpec.kt:58` | `zoned` taken by position from `mapped` | fixed | An explicit list |
| 1 | NIT (impl, domain) | `DateAwakersMalformedInputSpec.kt:109` | The valid-input case cannot tell a zone from UTC; no offset, "Z" or "SYSTEM" case for the Mp awakers | fixed | 22:00Z: April 4 in UTC and "Z", April 5 in Bucharest, "+03:00", "UTC+03:00"; "SYSTEM" reads; mutant P7 (zone ignored) red |
| 1 | NIT (impl) | `ActivationHardeningE2eSpec.kt:594` | The second-use e2e is refused by the lookup, not the count | no change | True and intended: the count is pinned by the unit race test on both backends; the e2e pins sequential reuse |
| 1 | NIT (domain) | `AuthRealmActivationBaseSpec.kt:631` | "never reach the cap": they reach it, never exceed it | fixed | |

Probed and CLEAN (from the three reports): the exceptions each helper catches are the only ones its factory throws
for a String; `mpTimezoneOrNull` checks with the same kotlinx zones the calculation uses, offsets, "Z", "SYSTEM",
"GMT0", "UT" included, no valid id newly refused; extreme `ts` values; messages name the path, never the value;
`SlumberRestCodec`'s catch not widened; no new cost per request; no auth/saas/all record holds a nullable or defaulted
date of these types. `RemoveResult.count` is 1 only for the deleting request on Karango (`REMOVE … RETURN OLD`, a
missing document → count 0) and Mongo (`deletedCount`); the refusal is the same 403 as an unknown token. The cap
arithmetic (11 re-sends by 161h20, the next at 185h20) and the trim keeping exactly `keep`. SURPLUS: nothing.

**Verdict:** gate PASS after round 1. After the fixes: Slumber 1249/0, funktor-auth 293/0; mutant P7 red.

## History

- 2026-11-06: created on the maintainer's word, from ULTRA-00292 finding 1.2.
- 2026-11-06: built, gate PASS after round 1, DONE. No docs ticket: no public API changed.
- 2026-11-06: DONE.
