# Slumber: `@JvmInline value class` serialization support

**Status:** IN PROGRESS — started 2026-07-24. Framework feature in `ultra/slumber`.
**Motivation:** enable canonical value types (e.g. `Email`, `RealmId`) that are stored/read through
slumber (the DB codec + slumber-based API paths). Blocker was: slumber had no value-class codec, so a
value-class field failed with "no known way to slumber/awake".
**Type:** framework (serialization).

## Goal — behave EXACTLY like kotlinx.serialization (empirically confirmed 2026-07-24)
Probed kotlinx directly (`@Serializable @JvmInline value class`):
- **Wire shape:** a value class is emitted as its **underlying value via the inner type's codec** —
  `String -> "s"`, `Int -> 42`, a value class over a data class -> that data class's object. Never
  `{ "value": ... }`.
- **Decode:** constructs via the **primary constructor, no transform** (a mixed-case input is NOT
  normalized). Canonicalization on decode requires an explicit **custom serializer** — slumber has no
  "construct via factory" concept and does not need one.
- **Invariant we rely on:** values are stored CANONICAL (normalized at the write boundary, e.g.
  `Email.of(...)`), so what comes back is always already valid. No decode-time transform needed.

## Design (matches the above)
- `BuiltInModule` gains two `cls.isValue` branches:
  - `getAwaker`: `ValueClassAwaker(type)` — awake the raw scalar to the value class's single underlying
    type (via the INNER codec), then `primaryCtor.callBy(...)` (ctor made accessible — value classes
    have synthetic/private ctors). Wrapped by the normal `wrapIfNonNull`.
  - `getSlumberer`: `ValueClassSlumberer(type)` — unwrap to the underlying property and slumber THAT
    via its inner codec. **NOT** `wrapIfNonNull`-wrapped (see the reflection quirk below), mirroring
    the existing `KotlinXJsonNull` treatment.
- Both reuse `ReifiedKType` (single ctor param / underlying property; reifies generic type args).

## Reflection quirk found + handled
`prop.get(holder)` on a NULL nullable value-class field (`opt: VcEmail?` = null) returns
`VcEmail(value=null)` — a **non-null box wrapping null**, not a plain `null`. Runtime-class dispatch
then resolves the non-null slumberer and a `NonNullSlumberer` throws. Fix: the value-class slumberer is
not non-null-wrapped, so a null-underlying box slumbers to `null`. The awaker stays normally wrapped
(nullable types already pass `null` through).

## Test matrix (`ValueClassRoundTripSpec`)
- [x] String-backed -> plain string; Int-backed -> plain number (inner codec).
- [x] Nested in a data class (nested / list / int).
- [x] Nullable value-class field round-trips when absent (the quirk above).
- [x] Generic value class (`VcBoxed<String>`, `VcBoxed<Int>`) — `ReifiedKType` reifies the inner type.
- [x] Private-ctor + `of()` factory — slumber the canonical stored value; awake via ctor (no transform).
- [x] Value class over a NULLABLE underlying (`value class N(val v: String?)`) — `N(null)` round-trips.
- [x] Value class over a COMPLEX inner (a data class) — serializes as the inner object.
- [x] Value class as a Map KEY and as a Map VALUE.
- [x] Value class IMPLEMENTING an interface.

## Test evidence
- [x] `ValueClassRoundTripSpec` (11 cases) green; whole `:ultra:slumber:jvmTest` suite green (1215/0/0
      — no regressions).
- [x] Storage note: a value-class FIELD serializes/deserializes through the DB codec already (karango/
      monko use the default `SlumberConfig` incl. `BuiltInModule`), because storage runs through slumber.
      TYPE-SAFE QUERY PATHS over a value-class field (filtering by it) need KSP support — split into the
      Karango-KSP and Monko-KSP follow-up tasks (each with full DB round-trip tests).

## Follow-ups (separate tasks)
- **Karango KSP** value-class support — generate query-path accessors for value-class-typed properties
  (analogous to the `Ref`/`SoftDelete` handling); full store→filter-by→read round-trip tests.
- **Monko KSP** value-class support — same, for `MongoPropertyPath`.
- **Audit + standing rule:** wrap ids and similar in value classes (RealmId, OrgId, …) to prevent
  accidental assignment/comparison — findings task doc, then user codifies the rule.

## Review record — 3-agent gate (impl+style / serialization-domain / security), 2026-07-24 — LOOP CLOSED (round 2)

- **Round 1** — Security clean (reflective construction is NARROWER than the existing DataClass codec;
  no type-confusion — the type is schema-fixed, only the scalar is attacker-controlled; no new DoS —
  value-class nesting is compile-time bounded). Domain confirmed kotlinx parity for all user value
  classes. Findings fixed: **HIGH** — a NON-optional nullable value-class field failed to decode `null`
  (the awaker awaked the inner before the null check → threw); fixed by guarding the RAW input first.
  **MEDIUM** — the unconditional `cls.isValue` gate captured kotlin STDLIB value classes (`Duration`,
  `UInt`, `Result`) and would silently mis-serialize their backing field; fixed with `isUserValueClass`
  (excludes `kotlin.*`, fail-fast as before). Plus a test-gap fix (non-optional nullable field, collection
  nulls) and LOW nits (removed a no-op `javaMethod` line; `of()`-bypass KDoc caveat; VC-over-VC test).
- **Round 2** (fresh 3-agent gate) — Impl **no defects** (both fixes verified non-vacuous via the
  propagating DataClassAwaker path; 2 INFO: the second `inner==null` guard is now a defensive backstop;
  nullable-over-nullable is intrinsically ambiguous — kotlinx too). Domain **parity re-confirmed**, 1 LOW
  (kotlinx.* value classes aren't fail-fast-excluded — accepted + documented: only kotlin stdlib is
  special-cased; a divergent third-party value class must get its own `SlumberModule`). Security **empty**.
  **LOOP CLOSED** — the round-2 items were doc-only clarifications, applied. `ValueClassRoundTripSpec`
  15/15; whole `:ultra:slumber:jvmTest` 1219/0/0.

## Security caveat (documented; red-team item)
Awake constructs via the CTOR, bypassing an `of()` factory — validation that must survive deserialization
of untrusted input belongs in `init {}` (runs on the ctor), not only a factory. Red-team probe (record
alongside the value-class-ids adoption): define a value class validated only in `of()`, feed malformed
untrusted input through slumber decode, confirm the invalid instance is constructed.

## Status
DONE 2026-07-24 — review loop closed round 2; committing + archiving. Follow-ups (KSP tasks + the ids
audit) tracked in their own docs.
