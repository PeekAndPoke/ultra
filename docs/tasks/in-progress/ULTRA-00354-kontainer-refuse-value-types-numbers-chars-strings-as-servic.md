# ULTRA-00354: Kontainer: refuse value types (numbers, chars, strings) as services

> **Status (2026-11-25): IN PROGRESS.** Implemented on `wt/kontainer-values`; gate PASS, ready to merge.

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

## Implementation notes

- One list, by Java class: `valueTypes` in `ultra/kontainer/src/main/kotlin/KontainerBuilder.kt:23`, the eight
  primitives in both forms (`javaPrimitiveType` and `javaObjectType`), plus `UByte`, `UShort`, `UInt`, `ULong`,
  `String`, `Unit`. `isValueType` (`:30`) compares `cls.java`, so `int` and `java.lang.Integer` are refused each on
  its own entry, not through `KClass` equality. `refuseValueType` (`:35`) throws `KontainerInconsistent`
  "kotlin.Int is a value type and cannot be registered", next to the built-in refusal of ULTRA-00336.
- Call sites, all at the defining call:
  - `ServiceBuilder.add` (`:101`): the factory forms of all four builders (the inline factories call it).
  - `ServiceBuilder.invoke(srv)` (`:114`): before `ServiceProducer.forClass`. Otherwise `Unit` fails first as
    "Class 'kotlin.Unit' has no primary constructor" (seen in the RED run; pinned by mutant M11).
  - `ServiceBuilder.invoke(srv, impl)` (`:127`, `:128`): both sides, before `forClass(impl)`.
  - `instance(value)` (`:748`) and `instance(srv, value)` (`:768`).
  - `with` at create and clone: `KontainerBlueprint.createChecked` (`KontainerBlueprint.kt:233`), after the built-in
    check and before the dynamics checker: "kotlin.Int is a value type and cannot be overridden; name the service class
    instead: with(Service::class) { ... }" (hint added in gate round 1).
- JVM forms reaching the builder: `Int::class` is `int`; `Int::class.javaObjectType.kotlin`, `5::class` (so
  `instance(5)`) and the reified `with<Int>` are `java.lang.Integer`. The unsigned types are value classes with one
  form: `UInt::class.java` and `5u::class.java` are both `kotlin.UInt` (`javaPrimitiveType` is null), pinned by the
  fixture test.
- `with` before the change did NOT refuse reliably, so the check is not redundant. On an empty blueprint every value
  type was refused, but only as "Unexpected dynamics were provided: kotlin.Int". Next to a dynamic of a base type
  (`dynamic(Any::class)`, `Number`, `Comparable`) the object form fitted it by Java assignability and was accepted as
  an override of that base, while the primitive form was refused. In the RED run the shared blueprint even showed the
  aliasing in `DynamicsChecker`'s cache (keyed by a `Set<KClass>`): after the `int` forms, the `Integer` forms were
  refused too, while `String`, `UInt` and the other single-form types were accepted.
- Scope of the impl side: checked only where the impl is a class (`singleton(Srv::class, Impl::class)`). A factory
  that returns a value (`dynamic(Number::class) { 0.5 }`) and `instance(Number::class, 5)` register a `Number`
  service, which the ticket allows; both are pinned as accepted. To decide at the gate if that reading is wrong.
- Tests: new `ultra/kontainer/src/test/kotlin/e2e/ValueTypeServicesSpec.kt` (28 tests): 18 registration forms (4
  builders x by class, by factory, value type as service, value type as impl; `instance(value)`,
  `instance(cls, value)`), each over all 22 class forms in one map assertion, plus "after the call" if the throw came
  late; module and extend; `with` at create and clone on an empty blueprint and next to a dynamic `Any`; the reified
  `with<Int>`; allowed `Number`, `CharSequence`, `IntArray`, `Array<String>`, an own value class, `Comparable` by
  factory, an override of `Number`; `get` / `getOrNull` / `getAll(Int)` in both forms stay not found / null / empty.
- `ProvisionPathsCharacterizationSpec`: the five aliasing pins became three, `:219` (`instance(5)` refused with the
  message), `:228` and `:349` (a `Number` service: `getOrNull`, `getAll`, `Lookup`, `List` injection find it, `Int`
  finds nothing, in either order).
- `ExactOverrideMatchSpec:192` and `:207`: the two `dynamic(Int::class)` cases moved to `Count : Quantity`. The first
  keeps the shape (subclass registered first, base by factory after, `with<Count>` replaces `Count`); its
  primitive-only property (equal `KClass`, unassignable Java classes) no longer exists. The second keeps "the checker
  asks Java assignability": `with { Quantity() }` with only `Count` registered is refused as unexpected.
- Docs: one sentence after "Existing instances" in `docs-site/src/pages/ultra/kontainer/services.astro` and
  `docs-site/src/data/llms/kontainer.md`.
- For the coordinator (not edited here): `CHANGELOG.md:47` (Unreleased, Fixes, ULTRA-00352) describes the aliased
  `instance(5)` key, which can no longer be registered; the Breaking changes entry is still to be written.

## Test evidence

- RED before the change (spec on unchanged code, XML): 28 tests, 24 failed, 4 passed (fixture, allowed types,
  `Number` override, lookups). Every registration form accepted every value type at the call ("after the call:
  accepted"), except `Unit` by class, refused as `InvalidClassProvided`; `with` gave "Unexpected dynamics" or
  accepted, as described above.
- Kontainer suite after the change (XML, `-x generateDocs`): 652 tests, 0 failures, 0 errors (654 before, the five
  aliasing pins became three).
- Mutants (each from a `cp` backup, restore checked with `cmp` and `git status`; failures from XML):

| # | Mutant | Result |
|---|--------|--------|
| M1 | `isValueType` always false (no check at all) | RED, 25 |
| M2 | no impl check in `invoke(srv, impl)` | RED, 5 |
| M3 | no check in `ServiceBuilder.add` (factory forms) | RED, 5 |
| M4 | no check in `invoke(srv)` | RED, 5 |
| M5 | no check in `instance(value)` | RED, 3 |
| M6 | no check in `instance(srv, value)` | RED, 2 |
| M7 | no override check in `createChecked` | RED, 5 |
| M8 | list without the object forms (`java.lang.Integer` ...) | RED, 25 |
| M9 | list without the primitive forms (`int` ...) | RED, 22 |
| M10 | exact override match removed (for the moved `Count` case) | RED, 7, the moved case among them |
| M11 | `invoke(srv)` checks after `forClass` | RED, 4 (`Unit` named as a class without constructor) |
| M12 | `DynamicsChecker` assignability reversed (for the moved "unexpected" case) | RED, 33, the moved case among them |

- Full suite (`console/wt-gradle.sh --full-suite`, clean build, 16 min): BUILD SUCCESSFUL; XML over 55 modules,
  19147 tests, 0 failures, 0 errors (kontainer 652). No value-type registration elsewhere in the repo; no tracked
  file changed by `generateDocs`.

## Review ledger

Round 1, standard gate, three charters (implementation and code style, domain, security), for ULTRA-00354 and
ULTRA-00355 together. Zero CRITICAL, zero MAJOR: gate PASS in round 1. This ticket's rows:

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR (domain) | `ultra/kontainer/src/main/kotlin/KontainerBlueprint.kt:233` | The override refusal names the value type as if it were the target: with `dynamic(Number::class) { 0.5 }`, `create { with { 5 } }` says "kotlin.Int is a value type and cannot be overridden" while Number was meant, per request at runtime | fix | Hint appended: "...; name the service class instead: with(Service::class) { ... }"; `ValueTypeServicesSpec` expects it for every class and JVM form |
| 1 | MINOR (impl) | `ultra/kontainer/src/test/kotlin/e2e/ValueTypeServicesSpec.kt:32`, `:70`, `:72` | The expected messages are built with the production `getName()`, so a regression of it stays green | fix | The fixture carries a literal name per class (`"kotlin.Int"` ...); labels and expected messages use it; the `getName` import is gone |
| 1 | security | | NO FINDINGS | | |

CLEAN in round 1, one line each:
- Bypass paths: none; every registration form and `with` at create and clone reach the checks.
- Allowed types are not refused: `Number`, `CharSequence`, arrays, value classes of one's own, a factory returning a
  value, `instance(Number::class, 5)`.
- Request isolation: overrides stay per kontainer; a clone does not change its parent.
- Concurrency of `merge`: it builds new immutable maps and shares no mutable state.
- Messages carry class names only, no values.
- Pins changed by the decisions: none weaker; each changed pin asserts the new behaviour as strictly as before.

## History

- 2026-11-25: created on the maintainer's word; type list chosen by the maintainer (value types, exact classes).
- 2026-11-25: IN PROGRESS.
- 2026-11-25: implemented on `wt/kontainer-values`; spec RED before, 12 mutants RED; status IN PROGRESS, review gate next.
- 2026-11-25: gate round 1 PASS (0 CRITICAL, 0 MAJOR); MINOR batch applied: the override refusal hints at the service
  class, the spec pins every name by literal. Kontainer 661 / 0 / 0 (XML). Ready to merge.
