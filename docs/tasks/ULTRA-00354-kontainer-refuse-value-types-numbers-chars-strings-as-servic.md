# ULTRA-00354: Kontainer: refuse value types (numbers, chars, strings) as services

> **Status (2026-11-25): DESIGNED.** Plan agreed with the maintainer; implementation next.

## Why this exists

Found in ULTRA-00351 and ULTRA-00352: `Int::class` (Java `int`) and `java.lang.Integer` are equal map keys with
different assignability, so the TypeLookup caches answer depending on which type was asked first (`instance(5)`, then
`getOrNull(Int::class)` versus `getAll(Number::class)`). Pinned in `ProvisionPathsCharacterizationSpec`.

Maintainer (2026-11-25): "I would actually disallow all primitive types like numbers chars strings etc from being
registered as services. This would not make a lot of sense." With no value type ever registered, the aliasing cannot
happen, and no cache change is needed.

## Decisions (maintainer, 2026-11-25)

- Refused, exact classes only: `Boolean`, `Byte`, `Short`, `Int`, `Long`, `Float`, `Double`, `Char` in both JVM forms
  (`int` and `java.lang.Integer` and so on), `UByte`, `UShort`, `UInt`, `ULong`, `String`, `Unit`.
- Allowed: `Number`, `CharSequence`, arrays, and every other type (no aliasing problem; a deliberate use is
  conceivable).
- Registration throws at once, naming the type, like the other registration errors: the mistake shows when the
  blueprint is built. Every registration form: `singleton`, `prototype`, `dynamic`, `dynamicPrototype`, `instance`,
  their two-type forms (`singleton(Srv::class, Impl::class)` with a value type on either side), and `with` at
  `create` / `clone`.
- `get(Int::class)` / `getOrNull` / `getAll` keep their current answers (not found, null, empty).
- A breaking change: CHANGELOG `## Unreleased`, Breaking changes.

## The work

- The check where registration already rejects things (the built-ins, ULTRA-00336), one list of refused classes.
- Tests first: a spec that every refused class, in both JVM forms, is rejected by every registration form, and that
  `Number`, `CharSequence` and an array are accepted. RED before the change.
- The aliasing pins in `ProvisionPathsCharacterizationSpec` become pins that `instance(5)` is refused; the two
  `ExactOverrideMatchSpec` cases (`dynamic(Int::class)`, line 190 and 205) move to types of their own.
- Run the full suite: a registration whose type is not visible at the call (`instance(config.name)`) shows only there.
- Docs: one sentence on the services page and the LLM mirror (with ULTRA-00338 if still open).

## Acceptance

- Every refused class is rejected by every registration form, with a message naming it; allowed types still register.
- Kontainer suite and the full suite green; the new spec RED against the old code.

## History

- 2026-11-25: created on the maintainer's word; type list chosen by the maintainer (value types, exact classes).
