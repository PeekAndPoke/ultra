# Plan: Remove korlibs-time dependency from ultra/datetime

## Context

The `korlibs-time` (v6.0.0) dependency exists solely for date/time formatting — 3 `format(String)` methods
across 3 files. There's already a TODO in Deps.kt: "get rid of this ... still needed for date formatting".
`kotlinx-datetime` (v0.6.2) is already a dependency but its formatting API uses a DSL builder, not format strings.

## Scope of korlibs usage

Only 3 files import korlibs:

| File                 | korlibs type            | Method                                             |
|----------------------|-------------------------|----------------------------------------------------|
| `MpLocalDate.kt`     | `korlibs.time.Date`     | `Date(year, month, day).format(formatString)`      |
| `MpLocalTime.kt`     | `korlibs.time.Time`     | `Time(h, m, s, ms).format(formatString)`           |
| `MpZonedDateTime.kt` | `korlibs.time.DateTime` | `DateTime.fromUnixMillis(ts).format(formatString)` |

## Format patterns actually used by consumers

```
"yyyy-MM-dd"                        — MpLocalDate (kraft mapping, funktor vault)
"dd MMM yyyy"                       — MpLocalDate (tests)
"HH:mm:ss"                          — MpLocalTime (kraft mapping)
"HH mm ss SSS"                      — MpLocalTime (tests)
"yyyy-MM-dd HH:mm"                  — MpZonedDateTime (tests)
"yyyy-MM-dd HH:mm:ss.SSS"          — MpZonedDateTime (funktor logging/cluster)
"yyyy-MM-ddTHH:mm:ss"              — MpZonedDateTime (kraft mapping)
"yyyy-MM-dd'T'HH:mm:ss.SSSZ"      — MpZonedDateTime (tests)
```

## Options

### Option A: Roll our own format() — RECOMMENDED

Write a small multiplatform `DateTimeFormatter` that handles the ~10 tokens actually used:
`yyyy`, `MM`, `MMM`, `dd`, `HH`, `mm`, `ss`, `SSS`, `Z`, literal chars, quoted literals (`'T'`).

**Pros:**

- Zero new dependencies
- Keeps the existing `format(String)` API — no breaking change for consumers
- ~80-100 lines of code for a token-based formatter
- Easy to test exhaustively since the token set is small and fixed

**Cons:**

- We maintain formatting code (but the scope is very small)

### Option B: Use kotlinx-datetime Format DSL

Replace `format(String)` with pre-built `DateTimeFormat` instances.

**Pros:**

- Uses official Kotlin library

**Cons:**

- **Breaking API change** — callers can no longer pass arbitrary format strings
- `kotlinx-datetime` 0.6.2 Format DSL doesn't support `MMM` (abbreviated month names) easily
- Every call site must be refactored

### Option C: Upgrade kotlinx-datetime to latest and use its Format API

**Cons:**

- Still a breaking change for format string callers
- Upgrading has its own risk surface

## Critical requirements

1. **100% compatible replacement** — the `format(String)` method signature stays identical.
   Every format pattern that works today must produce the exact same output.
2. **Never throw at runtime** — if the formatter encounters an unrecognized token or
   malformed pattern, it must fall back gracefully (e.g., pass the unrecognized token through
   as literal text). No exceptions, no crashes. A wrong format is better than a crash.
3. **Verify against korlibs output** — before removing the dependency, run both formatters
   side-by-side on all known patterns and assert identical output.

## Recommended approach: Option A

### Step 1: Create `MpDateTimeFormatter.kt` in commonMain

A simple token-based formatter:

```kotlin
object MpDateTimeFormatter {
    fun format(pattern: String, year: Int, month: Int, day: Int,
               hour: Int, minute: Int, second: Int, millis: Int,
               offsetString: String?): String
}
```

Tokens to support:

- `yyyy` → 4-digit year
- `yy` → 2-digit year
- `MM` → zero-padded month (01-12)
- `MMM` → abbreviated month name (Jan-Dec, English only)
- `dd` → zero-padded day (01-31)
- `HH` → zero-padded hour (00-23)
- `mm` → zero-padded minute (00-59)
- `ss` → zero-padded second (00-59)
- `SSS` → zero-padded milliseconds (000-999)
- `Z` → timezone offset (e.g., +0000, Z for UTC)
- `'...'` → literal quoted text (e.g., `'T'`)
- Any other char → passed through as literal

**Error handling:** Unknown tokens are passed through as literal text.
`format("Hello yyyy!")` → `"Hello 2022!"`. Never throws.

### Step 2: Dual-implementation phase (korlibs stays as reference)

In each of the 3 files, keep the korlibs implementation as a reference and add the new one:

```kotlin
// MpLocalDate.kt

/** Formats using the NEW implementation */
fun format(formatString: String): String {
    return MpDateTimeFormatter.format(formatString, year, monthNumber, day, ...)
}

/** Formats using korlibs (TEMPORARY — remove once verified) */
fun formatWithKorlibs(formatString: String): String {
    val klock = Date(year = year, month = monthNumber, day = day)
    return klock.format(formatString)
}
```

Same pattern for MpLocalTime and MpZonedDateTime.

### Step 3: Write compatibility tests that compare both outputs

For every known format pattern, assert that `format()` and `formatWithKorlibs()` produce
the exact same output:

```kotlin
"format() matches korlibs for yyyy-MM-dd" {
    val date = MpLocalDate.of(2022, 4, 11)
    date.format("yyyy-MM-dd") shouldBe date.formatWithKorlibs("yyyy-MM-dd")
}
```

Run these for all 8 consumer patterns, plus edge cases. Both implementations exist
simultaneously — if anything differs, the test catches it before we remove korlibs.

### Step 4: Once all compatibility tests pass, remove korlibs

- Delete all `formatWithKorlibs()` methods
- Remove korlibs imports from the 3 files
- Remove korlibs from build.gradle.kts and Deps.kt
- The compatibility tests become normal tests (just remove the korlibs comparison half)

### Step 4: Write comprehensive tests for the formatter

Test all 8 format patterns from the consumer analysis above, plus edge cases:

- Midnight, noon, end of year, leap day
- Single-digit months/days (padding)
- Millisecond precision (000, 001, 999)
- Quoted literals (`'T'`, `'Z'`)
- Unrecognized tokens (graceful fallback)
- Empty pattern → empty string
- Pattern with only literals → literals unchanged

### Step 4: Remove korlibs dependency

- Remove `implementation(Deps.KotlinLibs.korlibs_time)` from `ultra/datetime/build.gradle.kts`
- Remove `korlibs_time` from `Deps.kt`
- Remove the TODO comment

### Step 5: Verify no other modules use korlibs

Search the entire repo for korlibs imports.

## Files to modify

| File                                                                | Change                                  |
|---------------------------------------------------------------------|-----------------------------------------|
| New: `datetime/src/commonMain/kotlin/format/MpDateTimeFormatter.kt` | Token-based formatter                   |
| New: `datetime/src/commonTest/kotlin/MpDateTimeFormatterSpec.kt`    | Tests                                   |
| `datetime/src/commonMain/kotlin/MpLocalDate.kt`                     | Replace korlibs import + format() body  |
| `datetime/src/commonMain/kotlin/MpLocalTime.kt`                     | Replace korlibs import + format() body  |
| `datetime/src/commonMain/kotlin/MpZonedDateTime.kt`                 | Replace korlibs imports + format() body |
| `datetime/build.gradle.kts`                                         | Remove korlibs dependency               |
| `buildSrc/src/main/kotlin/Deps.kt`                                  | Remove korlibs_time constant + TODO     |

## Verification

1. `./gradlew :ultra:datetime:compileKotlinMetadata` — no korlibs references
2. `./gradlew :ultra:datetime:jvmTest` — all existing tests pass
3. Grep for `korlibs` across entire repo — zero hits
4. Build kraft examples — format calls still work
