# ULTRA-00352: Kontainer: getAll, List and Lookup throw ServiceAmbiguous when one candidate extends another

> **Status (2026-11-24): IN PROGRESS.** Track F: tests first, fix, mutation-checked; waiting for the gate

## Why this exists

Found by the ULTRA-00351 audit and pinned as current behaviour by its characterization spec
(`ultra/kontainer/src/test/kotlin/e2e/characterization/ProvisionPathsCharacterizationSpec.kt`).

```kotlin
interface PathsBase
open class PathsMiddle : PathsBase
class PathsLeaf : PathsMiddle()
// both PathsMiddle and PathsLeaf registered
```

`getAll(PathsBase)` finds both candidates, then resolves each again by its own class (`Kontainer.getAll` calls
`get(it)`, which runs `superTypeLookup.getDistinctFor(it)`). `PathsMiddle` matches itself and `PathsLeaf`, so the call
throws `ServiceAmbiguous` instead of returning both. The same for a `List<PathsBase>` injection, `Lazy<List<PathsBase>>`
on first access, and `Lookup<PathsBase>.get(PathsMiddle::class)` (the `LazyServiceLookup` map entries,
`TypeLookup.kt`). Validation does not catch it (`ForListOfServices.validate` is always valid).

## Decided (maintainer, 2026-11-24)

"ok yes fix it in an extra ticket". This is a behaviour change: calls that threw now return each candidate's own
service.

## The work

- Each candidate already IS a registered key: hand out its own provider directly (`factory.getProvider(candidate)`),
  without the second resolution, in `getAll`, the List and Lazy List injections and the Lookup map. Keep the order of
  `getAllCandidatesFor`.
- `getOrNull` and `get` stay as they are (ULTRA-00351 showed that the second resolution in getOrNull is observable for
  aliased primitive keys; out of scope here).
- Tests first: turn the ULTRA-00351 pins of the throw into the wanted behaviour (RED before the fix); check that a
  plain hierarchy without the overlap behaves as before; mutation-check.
- CHANGELOG under Fixes (a behaviour change without a compile signal).

## Implementation (Track F, wt/kontainer)

- `Kontainer.getCandidate(candidate, context)` (internal): `factory.getProvider(candidate).provide(this, context)`.
  `getAll` (and so the List and Lazy List injections) and the `LazyServiceLookupBlueprint` map entries
  (`TypeLookup.ForSuperTypes.getLookupBlueprint`) use it. `get`, `getOrNull`, `getProvider` and `use` are unchanged.
- Tests (`ProvisionPathsCharacterizationSpec`): the two ULTRA-00351 pins of the throw replaced by
  - getAll returns each candidate's own service in candidate order (registration order Leaf, Middle), also for
    `getAll(PathsMiddle)`; RED before the fix (ServiceAmbiguous).
  - List, Lazy List and Lookup injections hand out the same instances as getAll; RED before the fix.
  - get, getProvider and getOrNull of the overlapping super type unchanged (ServiceAmbiguous with the same message,
    null); green before and after.
  - getAll and the List injection with an overridden dynamic and a prototype candidate: the override, a new prototype
    each time; green before and after (nothing else changes).
  - Aliased keys: `getAll(Number)` after `getOrNull(Int)` returns `[5]`; before the fix it threw
    `ServiceNotFound: Service kotlin.Int was not found` (the second resolution read the empty shared `Int`/`Integer`
    entry). A side effect of the decided fix in the same direction: getAll no longer depends on what was asked first.
    `getOrNull`'s order-dependent answers stay pinned as before.
- Mutants (each applied to the fixed code, kontainer suite with `-x generateDocs`, restored with `cp` and `cmp`):

| Mutant | RED |
|---|---|
| getAll back to `get(it, context)` | getAll order, List/Lazy List/Lookup, aliased getAll (3) |
| Lookup map back to `kontainer.get(it, context)` | List/Lazy List/Lookup (1) |
| getAll candidates `reversed()` | getAll order, List/Lazy List/Lookup, override/prototype, BaseTypeInjection, LazyInjection (5) |
| `getCandidate` takes the provider of a fresh kontainer (ignores this kontainer's overrides) | override/prototype and 4 existing (5) |

- Kontainer suite 621 / 0 / 0 (XML), generateDocs without changes to tracked files.
- Measured (JMH, same settings as ULTRA-00351, 2026-11-24, load average 3 to 5 from other builds; before: master
  39db4ddd, after: d957e729). Other benchmarks are not touched by the change.

| Benchmark | ns/op before | ns/op after | delta | B/op before | B/op after |
|---|---:|---:|---:|---:|---:|
| getAll5 | 104.1 +- 1.3 | 64.6 +- 3.1 | -38% | 64 | 64 |
| lazyLookupGet | 29.4 +- 0.2 | 18.1 +- 0.1 | -38% | 0 | 0 |
| request | 4,746.5 +- 279.9 | 4,280.7 +- 68.5 | -10% | 5,496 | 5,512 |

## Acceptance

- The four forms return both candidates, in candidate order; every other kontainer and consumer suite green unchanged.

## History

- 2026-11-24: created on the maintainer's word.
- 2026-11-24: IN PROGRESS. Track F: tests first, fix, mutation-checked; waiting for the gate
