# Karango KSP: `@JvmInline value class` property support

**Status:** TODO — scheduled follow-up of `20260724-slumber-value-class-support.md`.
**Why:** slumber now serializes value classes (a value-class FIELD stores/reads fine). But the Karango
KSP generates the type-safe query-path accessors, and it does not yet understand value-class-typed
properties — so you cannot FILTER by a value-class id (`FILTER(entity.userId EQ someUserId)`). Needed
before Tier-1 stored ids (`OrgMember.userId`, `AuthRecord.ownerId`, `Organisation.slug`) can become
value classes (see `20260724-value-class-ids-audit.md`).
**Type:** framework (codegen).

## Where
- `karango/ksp/src/main/kotlin/KarangoKspProcessor.kt` — the processor generating `AqlPropertyPath`
  accessors per entity property.
- Analog to study: how the processor types a `Ref<T>` property and primitive properties (a value class
  is a wrapper over a scalar, exactly like `Ref` is a wrapper serializing to an id string).
- Tests: `karango/ksp/src/test/kotlin/FieldSelectionCodeGenSpec.kt`, `NestedClassCodeGenSpec.kt`.

## Scope
- Detect a value-class-typed property (KSP: the property's declaration is `@JvmInline value class`),
  EXCLUDING kotlin stdlib value classes (mirror slumber's `isUserValueClass` — Duration/UInt/Result).
- Generate an `AqlPropertyPath` accessor that:
  - compares at the DB level against the UNDERLYING stored scalar (the value class serializes to its
    underlying via slumber), and
  - is TYPE-SAFE at the API level — a filter should accept the value class (`entity.userId EQ UserId("…")`),
    not a bare `String`. Decide the generated path type (`AqlPropertyPath<UserId, UserId>` vs exposing
    `.value`) by what makes `EQ`/`IN`/ordering type-check cleanly — follow the `Ref<T>` precedent.
- Nested value classes and value-class fields inside nested objects.
- Value class over a NON-string underlying (Int/Long) — path compares against the number.

## Test evidence (REQUIRED — full round trip, both a codegen test and a live DB test)
- [ ] Codegen: `FieldSelectionCodeGenSpec`-style — an entity with a value-class property generates the
      expected `AqlPropertyPath` accessor (typed, excludes stdlib value classes).
- [ ] LIVE round trip (real ArangoDB, the existing karango integration harness): store an entity whose
      id field is a value class → `FILTER(entity.field EQ theValueClass)` returns it → read back →
      assert the value-class field equals the original. Include a NEGATIVE (a different value → no match)
      and a value-class-over-Int case.

## Review
- [ ] 3-agent gate (impl+style / codegen-domain / security) → loop to zero → commit.
