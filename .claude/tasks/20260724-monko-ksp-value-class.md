# Monko KSP: `@JvmInline value class` property support

**Status:** TODO — scheduled follow-up of `20260724-slumber-value-class-support.md`.
**Why:** same as the Karango-KSP task, for MongoDB. slumber serializes value-class FIELDS already, but
the Monko KSP generates the `MongoPropertyPath` query accessors and does not yet understand value-class
properties — so you cannot FILTER by a value-class id. Needed before Tier-1 stored ids become value
classes (see `20260724-value-class-ids-audit.md`).
**Type:** framework (codegen).

## Where
- `monko/ksp/src/main/kotlin/MonkoKspProcessor.kt` — the processor generating `MongoPropertyPath`
  accessors per entity property.
- **Direct analog: `monko/ksp/src/test/kotlin/RefCodeGenSpec.kt`** — `Ref<T>` is already handled as a
  wrapper that serializes to an id string; a value class is the same shape (wrapper over a scalar).
  Model the value-class handling on the `Ref` codegen.
- Other tests: `DataClassCodeGenSpec.kt`, `FieldSelectionCodeGenSpec.kt`.

## Scope
- Detect a value-class-typed property, EXCLUDING kotlin stdlib value classes (mirror slumber's
  `isUserValueClass` — Duration/UInt/Result).
- Generate a `MongoPropertyPath` accessor that compares at the DB level against the UNDERLYING stored
  scalar while being TYPE-SAFE at the API level (a filter accepts the value class, not bare `String`).
  Decide the generated path type by following the `Ref<T>` precedent (`RefCodeGenSpec`).
- Nested value classes; value-class fields in nested objects; value class over Int/Long.

## Test evidence (REQUIRED — full round trip, codegen + live DB)
- [ ] Codegen: an entity with a value-class property generates the expected `MongoPropertyPath`
      accessor (typed; stdlib value classes excluded) — mirror `RefCodeGenSpec`.
- [ ] LIVE round trip (real MongoDB, the existing monko integration harness): store an entity whose id
      field is a value class → filter `eq(entity.field, theValueClass)` returns it → read back → assert
      equality. Include a NEGATIVE and a value-class-over-Int case.

## Review
- [ ] 3-agent gate (impl+style / codegen-domain / security) → loop to zero → commit.

## Cross-ref
- Do this in lockstep with `20260724-karango-ksp-value-class.md` so both backends gain the capability
  together (storage features in this repo are both-backend, `MatrixTest2d`).
