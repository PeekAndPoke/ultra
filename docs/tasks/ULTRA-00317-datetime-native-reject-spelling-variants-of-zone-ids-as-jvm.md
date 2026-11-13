# ULTRA-00317: datetime (native): reject spelling variants of zone ids, as JVM and JS do

> **Status (2026-11-13): DESIGNED.** 
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

## Why this exists

Found in ULTRA-00217 (the native zone cache), 2026-11-13. On native, kotlinx resolves path variants of a zone id
(`Europe/./Berlin`, `Europe//Berlin`, `Europe/Berlin/`, `./Europe/Berlin`) and keeps the spelling as the id. JVM and
JS reject them. So `MpTimezone.of("Europe/./Berlin") != MpTimezone.of("Europe/Berlin")` on native, and a value a
native client serializes with such an id is refused when a JVM or JS peer converts it. An IANA id is a name, not a
path. The maintainer (2026-11-13): "yes follow up".

## The work

- [ ] On native, refuse an id with an empty or `.` path part or a leading or trailing `/` with the exception the
      other platforms throw. On a case-insensitive file system (macOS, mingw) also refuse a case variant.
- [ ] `TimezoneEngineNativeSpec` pins today's behaviour (the variants resolve, uncached): it flips.
- [ ] A test that `..` and absolute paths stay refused, so a kotlinx upgrade cannot widen what an id reaches.
- [ ] CHANGELOG: a behaviour change on native only.

## History

- 2026-11-13: created on the maintainer's word, from the review of track A (ULTRA-00316).
