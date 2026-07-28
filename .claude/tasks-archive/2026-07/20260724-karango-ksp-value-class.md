# Karango: `@JvmInline value class` filter support

**Status:** DONE 2026-07-24 — **needed ZERO production change** (works as-is). See below.
**Enabled by:** `20260724-slumber-value-class-support.md` (DONE).
**Type:** framework (verification + regression tests only).



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
The original premise was "the Karango KSP must learn value-class properties before you can filter by a
value-class id." **That premise was wrong — Karango already works end-to-end with no change:**

- **KSP needs NO change.** A value-class property already renders with its value-class type —
  `append<RealmId, RealmId>("realm")` — the type-safe accessor we want (`entity.realm EQ RealmId(...)`
  type-checks; a bare `String` is rejected). The value class itself gets no generated file (not
  data/abstract/sealed → blacklisted). Confirmed by `karango/ksp` `ValueClassCodeGenSpec`.
- **Runtime needs NO change.** The `EQ`/`GT`/... comparison value is stored raw in the bind-var map,
  then the whole map is `codec.slumber(...)`-ed as `Map<String, Any?>` at execution
  (`KarangoDriver`) → `AnySlumberer` dispatches on the runtime class → `ValueClassSlumberer` unwraps
  `RealmId("b2b")` → `"b2b"`, matching the stored scalar. So the filter matches with no special code.

(Contrast Monko, whose filter values bypass slumber and DID need a runtime fix —
`20260724-monko-ksp-value-class.md`.)

## What was implemented (tests only)
- `karango/ksp/src/test/kotlin/ValueClassCodeGenSpec.kt` — codegen golden guard: value-class property
  renders `append<RealmId, RealmId>` (incl. the L1..L5 nested-list variants), not scalar-reduced.
- `karango/core/src/test/kotlin/testdomain/testValueClassRecord.kt` — a `@Vault` test entity
  `TestVcRecord(realm: TestRealmId, score: TestScore, label: String)` (String- and Int-backed value
  classes; `TestScore` is `Comparable` for ordered filters) + repo, registered in the e2e `database`.
- `karango/core/src/test/kotlin/e2e/crud/E2E-Crud-ValueClassId-Filter-Spec.kt` — 3 LIVE ArangoDB
  round trips.

## Test evidence
- [x] Codegen golden (`karango/ksp` `ValueClassCodeGenSpec`).
- [x] LIVE ArangoDB round trips (`E2E-Crud-ValueClassId-Filter-Spec`): (1) `FILTER(doc.realm EQ
      RealmId("b2b"))` returns exactly the match and the value-class fields survive the round trip as
      value classes; (2) a non-existent id returns nothing; (3) ordered `GT` on the Int-backed value
      class compares against the underlying number.

## Review record (3-agent gate, 2026-07-24 — all opus/high)
- Reviewed together with the Monko diff (see `20260724-monko-ksp-value-class.md` for the full record).
- Karango has ZERO production change — findings all landed on the Monko fix. The Karango test entity
  + live round-trip spec were confirmed as genuine (live ArangoDB store→filter→read, incl. a negative
  and an Int-backed ordered filter). Gate verdict: **PASS**.

## Cross-ref
- Monko counterpart: `20260724-monko-ksp-value-class.md` (needed a runtime DSL unwrap).
- Migration that consumes this: `20260724-value-class-ids-migration.md`.
