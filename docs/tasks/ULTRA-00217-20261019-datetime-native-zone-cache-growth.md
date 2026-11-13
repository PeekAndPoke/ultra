# ULTRA-00217: ultra/datetime: native zone cache grows with path variants of zone ids

> **Status (2026-11-13): IMPLEMENTED, awaiting the review gate** (track A of ULTRA-00316, worktree `datetime`).
>
> **Part of:** ULTRA-00255 (Ultra libraries epic)

**Plan:** none — out-of-scope finding from round 3 of the `/feature-review` of ULTRA-00216 (datetime own epoch conversions) (2026-10-19)
**Security-critical:** yes (unbounded memory growth from attacker-controlled input) → red-team note below

## Spec

**Pre-existing**, introduced by the round-1 MINOR batch of
ULTRA-00209 (kotlinx datetime js timezones) (the native per-id `TimeZone` cache; its
ledger row says "NOT measured, NOT independently verified").

`ultra/datetime/src/nativeMain/kotlin/TimezoneEngineNative.kt` caches every zone id that `TimeZone.of(id)` resolves,
keyed by the RAW string. kotlinx-datetime 0.8 native (`TzdbOnFilesystem.rulesForId`, reviewer-read from the kotlinx
sources) rejects only absolute paths and `..`; `Path.fromString` drops empty components and keeps `.`. So
`Europe//Berlin`, `Europe/Berlin/`, `Europe/./Berlin`, `Europe/././Berlin`, … all open the same TZif file and each
becomes a permanent cache entry; the copy-on-write insert also copies the whole map (O(n) per insert).

Scenario: a Kotlin/Native server decoding user-supplied zone ids (`MpZonedDateTimeSerializer` → `MpTimezone.of` →
`atZone`) grows the map without bound. JVM is NOT affected (no ultra cache; `ZoneRulesProvider` rejects the variants),
JS is not either (Intl rejects them). This repo's servers are JVM, so today's exposure is nil.

- [x] Verify by experiment on linuxX64 (`TimeZone.of("Europe/./Berlin")` resolves?) — YES, see Implementation notes
- [x] Accept only canonical ids before caching (no empty / `.` components, or membership in `MpTimezone.supportedIds`),
      and/or cap the map — membership in `supportedIds`; rejecting the variants is NOT done (needs a decision, below)
- [x] Test on native: variants are rejected (or at least not cached), the map stays bounded — not cached
- [ ] Red-team scenario (collect only, never execute here): flood a native endpoint with distinct id spellings

Related MINORs from the same gate, for the maintainer list (not this task's scope unless chosen):
- JS: a wall time past the JS Date limit (last/first 18 h of the range, year ±275 760) yields offset 0 silently
  (`TimezoneEngineJs.kt` `intlOffsetSecondsAt`: `setUTCHours` → NaN → `toInt()` 0).
- JVM/native: on 999999999-12-31 in a zone with recurring rules the +1-day probe leaves java.time's year range →
  raw `java.time.DateTimeException`.

## Implementation notes

- **Probe (2026-11-13, linuxX64, kotlinx-datetime as pinned in the repo), a temporary native spec, since removed.**
  `TimeZone.of` resolves `Europe/./Berlin`, `Europe/././Berlin`, `Europe//Berlin`, `Europe/Berlin/` and
  `./Europe/Berlin`, and keeps the spelling as the id. It rejects `Europe/../Europe/Berlin`, the absolute path
  `/usr/share/zoneinfo/Europe/Berlin` and `europe/berlin` (case-sensitive filesystem). Through ultra's public path,
  `MpInstant.atZone(MpTimezone.of("Europe/./Berlin"))` gave the Berlin wall time with the zone id `Europe/./Berlin`,
  and every spelling became its own permanent cache entry (`Europe/` + `./` × n: unbounded). The reviewer's reading
  was right; the comment "bounded by the system's tz database" was wrong.
- Fix (`ultra/datetime/src/nativeMain/kotlin/TimezoneEngineNative.kt`): only ids in `MpTimezone.supportedIds` (590)
  are cached; any other id that resolves is returned uncached. Chosen over a canonical-path check because it bounds the
  map absolutely on every filesystem: on a case-insensitive one (APFS default on macOS; mingw untested) case variants
  would resolve too, and a path check would not catch them. Junk spellings also cannot crowd real zones out, as a
  size cap would let them.
- Cost: a valid zone missing from `supportedIds` (one newer than the list) is re-read on every lookup on native —
  correct, only slower.
- Behaviour is unchanged: the variants still resolve on native, while JVM and JS reject them. **Needs a decision:**
  reject non-canonical spellings on native with `IllegalTimeZoneException` for parity? It is a behaviour change on
  native, and on a case-insensitive filesystem it would also need a case check (JS does that via Intl).
- `cachedZoneIds()` (internal, nativeMain) exposes the cache keys for the test.

## Test evidence

- New `ultra/datetime/src/nativeTest/kotlin/TimezoneEngineNativeSpec.kt`: a supported id is cached; 203 path variants
  of `Europe/Berlin` still resolve to the Berlin wall time and leave the cache keys unchanged.
- Before the fix: the variants test failed (the key set gained the variants); linuxX64Test 1878 / 1 failure.
- After the fix, from the XML: linuxX64Test 1878 / 0 failures (`TimezoneEngineNativeSpec` 2 / 0); jvmTest 2523 / 0;
  jsBrowserTest 1888 / 0. `compileTestKotlinLinuxArm64` and `compileTestKotlinMingwX64` compile; the macOS and
  mingw tests cannot run on this linux host.
- Mutation-checked:
  - membership check removed → red: "path variants … are not cached".
  - nothing cached (check made always true) → red: "a supported zone id is cached".
  - not run: the cache-hit line; dropping it changes only speed, which no test observes.

## Changelog text

- `ultra/datetime` (native): the zone cache holds only the ids in `MpTimezone.supportedIds`. kotlinx/native resolves
  path variants such as `Europe/./Berlin`, and each spelling used to add a permanent entry, so user-supplied zone ids
  could grow it without bound (ULTRA-00217).

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|

**Verdict:**

## History

- 2026-10-21: numbered ULTRA-00217 (was `docs/tasks/20261019-datetime-native-zone-cache-growth.md`).
- 2026-11-13: probed (variants resolve) and implemented on track A (datetime) of ULTRA-00316; awaiting the review gate.
