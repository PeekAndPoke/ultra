# ULTRA-00352: Kontainer: getAll, List and Lookup throw ServiceAmbiguous when one candidate extends another

> **Status (2026-11-24): DESIGNED.** Decided by the maintainer: fix it, as its own ticket after ULTRA-00351.

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

## Acceptance

- The four forms return both candidates, in candidate order; every other kontainer and consumer suite green unchanged.

## History

- 2026-11-24: created on the maintainer's word.
