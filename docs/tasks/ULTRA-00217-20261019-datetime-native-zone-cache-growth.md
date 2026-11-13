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
- Fix (`ultra/datetime/src/nativeMain/kotlin/TimezoneEngineNative.kt`): only ids in `MpTimezone.supportedIds` (587 at the time; 591 since review round 1)
  are cached; any other id that resolves is returned uncached. Chosen over a canonical-path check because it bounds the
  map absolutely on every filesystem: on a case-insensitive one (APFS default on macOS; mingw untested) case variants
  would resolve too, and a path check would not catch them. Junk spellings also cannot crowd real zones out, as a
  size cap would let them.
- Cost: a valid zone missing from `supportedIds` (one newer than the list) is re-read on every lookup on native —
  correct, only slower.
- **Review round 1.** `supportedIds` matched roughly the 2021 tz database. Diffed against the system tzdata 2026e
  (`tzdata.zi`): missing canonical zones were `America/Ciudad_Juarez`, `America/Coyhaique`, `Europe/Kyiv`,
  `Pacific/Kanton`, `EST`, `HST`, `MST` (and `Factory`, not a real zone). Added the first four; the JVM, the browser
  (ChromeHeadless 157) and native all accept them. **`EST`, `HST`, `MST` were refused by the JVM** (`ZoneRulesException:
  Unknown time-zone ID`: java.time's tzdb provider leaves them out, only `ZoneId.SHORT_IDS` maps them to fixed
  offsets), so they stay out; JS and native accepted them. Links missing from the list (`Etc/UCT`, `GMT+0`, `GMT-0`,
  `ROC`) were left out on purpose.
- **JS, same growth, review round 1.** `TimezoneEngineJs.kt` kept two caches (formatter, interval) per id that Intl
  accepted, and Intl matches ids case-insensitively; the case check rejects variants of LISTED ids only, so every case
  spelling of an unlisted id (`etc/uct`, `ETC/UCT`, …) added two permanent entries. Now only ids in `supportedIds` get an
  interval cache; other ids are asked from Intl uncached (`intlOffsetSecondsAt(newFormatter(id), …)`, the same value the
  cache would memoise). The formatter cache is gone: each interval cache captures its formatter. Chosen over
  canonicalising through `resolvedOptions().timeZone`, which differs between ICU versions (e.g. `Europe/Kyiv` vs
  `Europe/Kiev`) and would make the cache key depend on the browser.
- Fixed-offset ids (`UTC+1`, `GMT-5`) never reach either platform cache: `TimezoneEngine` resolves them in common code
  (`fixedOffsetSecondsOrNull`).
- Spelling variants still resolve on native; refusing them is follow-up ULTRA-00317 (not done here).
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
- Review round 1: new `jsTest/kotlin/TimezoneEngineJsCacheSpec.kt` (a listed id, `Pacific/Kanton`, is cached; all 64
  case spellings of the unlisted `Etc/UCT` resolve to UTC and leave the cache keys unchanged) and four native cases
  (each newly listed id is cached). RED first: JS 1 failure (the keys grew), native 7 (all seven candidates uncached).
  After: jvmTest 2543 / 0, jsBrowserTest 1906 / 0, linuxX64Test 1898 / 0; arm64 and mingw test code compiles.
  Mutation-checked:
  - JS: the `!in supportedIds` branch disabled (every id cached) → red, the case-variants test.
  - JS: the store skipped for `Pacific/Kanton` → red, the listed-id test. (Skipping the store for every id, or for
    `Europe/Berlin`, made `DatetimeJsBenchmarkSpec` so slow that Chrome missed Karma's ping, no XML: red, but not
    attributable to one test, hence the narrower mutant.)
  - `Europe/Kyiv` removed from the list → red on native, "the zone id Europe/Kyiv is supported and cached".
- Review round 2 (MINORs, before the merge):
  - Item 1, JS hot path: `platformOffsetSecondsAt` looks in `offsetCaches` first and tests `supportedIds` membership only
    on a miss, as native's `zoneFor` does. Refactor, no behaviour change. jsBrowserTest 1906 / 0. Mutation-checked
    again on the new structure: membership test removed → red (case variants); store skipped for `Pacific/Kanton` →
    red (listed id cached).
  - Item 2, one memo slot for the last unlisted id that resolved (JS: its interval cache with its formatter; native:
    its `TimeZone`), keyed by the exact string and replaced by the next unlisted id: the caches hold the listed ids plus
    one. Before, one conversion of an unlisted id (the system zone when it is `EST`, `ROC`, `Etc/UCT`, a zone newer than
    the list) built a formatter / read the zone file per lookup: RED first, 110 builds (JS) and 110 file reads (native)
    for 10 days of `atZone` + `atStartOfDay`; now 1. Tests: a flood of unlisted ids (JS: 64 case spellings of
    `Etc/UCT`; native: `EST`, `HST`, `MST`, `ROC`, `Etc/UCT`) leaves the listed cache unchanged and the slot holding the
    last; repeated lookups reuse the slot (counted with `intlFormattersBuilt` / `zoneFileReads()`).
    After: jvmTest 2543 / 0, jsBrowserTest 1907 / 0, linuxX64Test 1900 / 0. Mutation-checked: memo read removed → red
    (JS 1, native 1: reuse); memo store removed → red (JS 2, native 2: flood and reuse).

## Changelog text

- `ultra/datetime` (native): the zone cache holds only the ids in `MpTimezone.supportedIds`. kotlinx/native resolves
  path variants such as `Europe/./Berlin`, and each spelling used to add a permanent entry, so user-supplied zone ids
  could grow it without bound (ULTRA-00217). Other ids that resolve (zones newer than the list, legacy links such as
  `ROC`) are not cached and are slower on native. Spelling variants still resolve on native (ULTRA-00317 will refuse
  them).
- `ultra/datetime` (JS): the zone cache holds only the ids in `MpTimezone.supportedIds`. Intl matches ids
  case-insensitively, so each case spelling of an unlisted id used to add permanent entries; other ids are now asked
  from Intl uncached (ULTRA-00217).
- `ultra/datetime`: `MpTimezone.supportedIds` adds `America/Ciudad_Juarez`, `America/Coyhaique`, `Europe/Kyiv` and
  `Pacific/Kanton`. `EST`, `HST` and `MST` stay out: java.time does not know them as region ids (ULTRA-00217).

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|

**Verdict:**

## History

- 2026-10-21: numbered ULTRA-00217 (was `docs/tasks/20261019-datetime-native-zone-cache-growth.md`).
- 2026-11-13: probed (variants resolve) and implemented on track A (datetime) of ULTRA-00316; awaiting the review gate.
- 2026-11-13: review round 1 fixes (four ids added, JS cache bounded); awaiting round 2.
