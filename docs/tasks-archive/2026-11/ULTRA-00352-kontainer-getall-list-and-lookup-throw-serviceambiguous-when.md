# ULTRA-00352: Kontainer: getAll, List and Lookup throw ServiceAmbiguous when one candidate extends another

> **Status (2026-11-25): DONE.** Merged into master (692e743a); gate PASS in round 1.

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
  - Aliased keys, the reverse direction (gate round 1): `getAll(Number)` or a Lookup entry FIRST, then
    `getOrNull(Int)` is now null and `get(Int)` throws `ServiceNotFound: Service kotlin.Int was not found`; before,
    the second resolution filled the shared `Int`/`Integer` entry with the instance's class, and both returned 5.
    Pinned with all four answers in one assertion; RED on the pre-fix code (getAll and the Lookup map back to `get`):
    `[(5, null), (5, null)]` instead of `[(null, ServiceNotFound...), (null, ServiceNotFound...)]`.
  - Injection forms are unaffected: the blueprint's `DependencyLookup` asks for the candidates of every injected
    candidate at build, which fills that entry for a `List<Number>` or `Lookup<Number>` injection before any request.
    Pinned: with a `List<Number>` holder registered, `getOrNull(Int)` and `get(Int)` find 5 (green before and after).
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

- Suites and compile sweep after both tickets: see ULTRA-00353 (all green, kontainer 624 / 0 / 0).

## Review ledger

Round 1, standard gate, three charters (implementation and code style, domain, security), Opus high (rung 1), for
ULTRA-00352 and ULTRA-00353 together. Zero CRITICAL, zero MAJOR: PASS. This ticket's rows (ULTRA-00353 has its own):

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR (impl, domain) | `ProvisionPathsCharacterizationSpec.kt:351` | Aliasing, the reverse direction: getAll or a Lookup entry no longer fills the shared `Int`/`Integer` entry, so a later `getOrNull(Int)` is null (was 5) and `get(Int)` throws (was 5) | fix | Pinned (RED on the old code, all four answers shown), comment reworded, named here and in the changelog line; injection forms unaffected, pinned |

CLEAN in round 1: get/getOrNull/getProvider/use code untouched; every candidate path uses getCandidate in candidate
order; getValue cannot miss for a candidate; overrides found via the base-type key; Lookup blueprint lambdas capture
only the KClass (request isolation); global singletons' List/Lookup resolve in the global kontainer; validation and
runtime agree better.

## Acceptance

- The four forms return both candidates, in candidate order; every other kontainer and consumer suite green unchanged.

## History

- 2026-11-24: created on the maintainer's word.
- 2026-11-24: IN PROGRESS. Track F: tests first, fix, mutation-checked; waiting for the gate
- 2026-11-25: gate round 1 PASS (0 CRITICAL, 0 MAJOR); MINOR batch applied: the reverse aliasing direction pinned and named.
- 2026-11-25: merged into master (692e743a); gate PASS in round 1, MINOR batch checked by hand.
- 2026-11-25: DONE.
