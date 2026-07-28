# Monko: `@JvmInline value class` filter support

**Status:** DONE 2026-07-24. **Reframed from the original premise** — see below.
**Enabled by:** `20260724-slumber-value-class-support.md` (DONE).
**Type:** framework (runtime DSL, NOT codegen).



## Commits & files changed

<!-- Generated 2026-07-28 from `git log --follow --name-status` over this task file.
     Commits that merely renamed the doc into the archive (R100) are excluded, since
     their code belongs to whatever task shipped alongside the move. -->

| Commit | Date | Files | Subject |
|---|---|---:|---|
| `b028e476` | 2026-07-24 | 10 | feat(monko): @JvmInline value class support in filter/update DSL |

Archived by `0f29d239` (rename only — that commit's code belongs to another task).

### Files changed (10)

**karango/core**
- `karango/core/src/test/kotlin/e2e/common.kt`
- `karango/core/src/test/kotlin/e2e/crud/E2E-Crud-ValueClassId-Filter-Spec.kt`
- `karango/core/src/test/kotlin/testdomain/testValueClassRecord.kt`

**karango/ksp**
- `karango/ksp/src/test/kotlin/ValueClassCodeGenSpec.kt`

**monko/core**
- `monko/core/src/main/kotlin/lang/dsl/filters.kt`
- `monko/core/src/main/kotlin/lang/dsl/updates.kt`
- `monko/core/src/main/kotlin/lang/dsl/value_class.kt`
- `monko/core/src/test/kotlin/io/peekandpoke/monko/lang/dsl/ValueClassFilterSpec.kt`
- `monko/core/src/test/kotlin/io/peekandpoke/monko/lang/dsl/ValueClassUpdateSpec.kt`

**monko/ksp**
- `monko/ksp/src/test/kotlin/ValueClassCodeGenSpec.kt`

## ⚠️ Empirical reframing (verified 2026-07-24, verification-first)
The original premise was "the Monko KSP must learn value-class properties before you can filter by a
value-class id." **That premise was wrong.** Verified by probe tests against the current code:

- **KSP needs NO change.** The Monko KSP already renders a value-class property with its value-class
  type — `append<RealmId, RealmId>("realm")` — which is exactly the type-safe accessor we want
  (`entity.realm eq RealmId(...)` type-checks; a bare `String` is rejected). The value class itself
  gets no generated file (it is not data/abstract/sealed → the blacklist skips it). Confirmed by
  `monko/ksp` `ValueClassCodeGenSpec` (golden regression guard).
- **The real gap is at RUNTIME.** Unlike Karango (whose `EQ` bind var is slumbered at execution),
  Monko filter values BYPASS slumber — they are handed raw to `com.mongodb.client.model.Filters`,
  whose codec registry has no codec for a value class → `CodecConfigurationException` at query
  execution. A probe (`ValueClassFilterSpec`) reproduced this for both String- and Int-backed value
  classes at `.toBsonDocument()` (the exact driver-encoding boundary).

## What was implemented
- `monko/core/src/main/kotlin/lang/dsl/value_class.kt` — `unwrapValueClass(value)`: reduces a
  SCALAR-backed `@JvmInline value class` to its underlying scalar (reads the PRIMARY-CTOR backing
  property, recurses for value-class-over-value-class, excludes `kotlin.*` stdlib mirroring slumber's
  `isUserValueClass`). Fast-paths plain scalars (no reflection on the hot path). A non-value-class
  value (String, Int, data class, collection, null) is returned unchanged. **Guard:** a NON-scalar
  backing (a value class over a date/enum/object — which slumber would store in a different shape than
  a reflected raw value) is REJECTED loudly at build time with an actionable message, rather than
  silently building a query that matches nothing. (True parity for such types would need slumber
  routing, which the pure DSL has no codec for — deferred; no planned id needs it, all are String-backed.)
- `monko/core/src/main/kotlin/lang/dsl/filters.kt` — every comparison/membership operator
  (`eq/ne/gt/gte/lt/lte/isIn/nin/all`) now passes its value(s) through `unwrapValueClass` before
  `Filters.*`. The ordered operators (`gt/gte/lt/lte`) use a non-null variant `unwrapOrderedValue`
  (their driver overloads require non-null; a value class wrapping null is not a valid ordering key).
- `monko/core/src/main/kotlin/lang/dsl/updates.kt` — the value-carrying update operators
  (`setTo/push/pull/addToSet`) unwrap identically (found during review): update values ALSO bypass
  slumber (`MonkoDriver.updateMany` applies the raw `Bson`, unlike `insertOne`/`replaceOne`), so a
  value-class field written via a partial update would throw the same `CodecConfigurationException`.
  This closes the write-via-update path so the feature covers read AND write.
- **Framework-wide behavior change:** ALL value-class comparisons/updates in Monko now unwrap, not
  just ids. Correct + desired (consistent with the slumber-based write path), low-risk (non-value-class
  values untouched → no regression; verified by the pre-existing `FiltersDslSpec`/`UpdatesDslSpec`).

## Test evidence
- [x] Codegen golden: `monko/ksp` `ValueClassCodeGenSpec` — value-class property renders
      `append<RealmId, RealmId>` (not scalar-reduced), no file generated for the value class itself.
- [x] Unit (DB-free, module norm — monko/core tests do not open sockets): `monko/core`
      `ValueClassFilterSpec` (8 cases: eq/ne/gt/isIn/nin, value-class-over-value-class recursion,
      non-value-class regression) + `ValueClassUpdateSpec` (setTo/addToSet/pull + a loud-rejection
      case for a non-scalar-backed value class). Each asserts the BSON encodes to the underlying
      scalar via `.toBsonDocument().toJson()` — the same encoding the driver performs at execution.
- [x] LIVE MongoDB round trip — **deferred to the RealmId migration** (user decision 2026-07-24).
      `monko/core` is deliberately DB-free; the live-Mongo proof arrives for free with Step 1
      (`RealmId` on `AuthRecord`), whose `funktor/auth` both-backend e2e filters `r.realm eq
      RealmId(...)` against live Mongo + Arango. No synthetic scaffolding.

## Review record (3-agent gate, 2026-07-24 — all opus/high)
- **Impl+style / Domain / Security** all ran over the diff. No CRITICAL/HIGH. Convergent findings,
  verified against the code, fixed:
  - **Update DSL gap (2 reviewers, MEDIUM):** `setTo/push/pull/addToSet` bypassed slumber identically
    → fixed (`updates.kt` unwraps). Verified `MonkoDriver.updateMany:242` applies the raw `Bson`.
  - **Non-scalar backing divergence (3 reviewers, MED/MED/LOW):** reflection unwrap ≠ slumber for a
    value class over a specially-serialized type → could silently match nothing → fixed with a loud
    build-time guard + honest KDoc (full slumber routing deferred; no id needs it).
  - **Per-call reflection (LOW):** fixed with a scalar fast-path.
- **Not fixed (rationale):** (a) a nullable-backed value class holding null unwraps to `{$eq:null}` —
  this is CONSISTENT with storage (`VcNullable(null)` slumbers to null), so the null-match is correct;
  the eq/ordered asymmetry is justified (can't order by null). (b) `funktor/rest` `ApiStatusPages`
  surfaces `cause.message` in prod → a driver `CodecConfigurationException` could leak an internal
  class name on a 500 — PRE-EXISTING, broad, out of scope for this diff (flagged to the user).
- Gate verdict: **PASS** (no open CRITICAL/HIGH; unit tests green; live-Mongo proof lands with RealmId).

## Cross-ref
- Karango counterpart: `20260724-karango-ksp-value-class.md` (needed ZERO change — works as-is).
- Migration that consumes this: `20260724-value-class-ids-migration.md` (KSP-first prerequisite is
  now MOOT — no KSP change; the only prerequisite was this Monko runtime fix, now DONE).
