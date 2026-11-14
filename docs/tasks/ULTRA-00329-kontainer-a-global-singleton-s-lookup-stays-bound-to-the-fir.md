# ULTRA-00329: Kontainer: a global singleton's Lookup stays bound to the first kontainer and keeps every prototype it creates

> **Status (2026-11-14): NOT STARTED.** 
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

**Found by:** the characterization specs of ULTRA-00319 (Kontainer injection type split), step 1, 2026-11-14.

## Why this exists

A global singleton that injects `Lookup<T>` keeps the lookup of the first kontainer it was created in. Prototypes fetched through it are created by, and recorded in, that kontainer's provider, also when the singleton is used from later kontainers: the provider's `instances` list keeps every one, and the singleton keeps that kontainer alive. Memory grows for as long as the lookup is used; a per-request service can reach into another request's kontainer.

Pinned as it is today in `ultra/kontainer/src/test/kotlin/e2e/characterization/InjectionFormsCharacterizationSpec.kt`, in a test
whose name starts with "CURRENT BEHAVIOUR". The split of ULTRA-00319 keeps this behaviour unchanged; the fix is this
ticket (maintainer, 2026-11-14: "fix each with its own ticket").

## Decided (maintainer, 2026-11-14)

Two effects, two causes (read from the code): the memory growth comes only from the introspection record (every
provider appends each created instance, with a reference to it, to `instances`, `ServiceProvider.kt:241-250`; readers
are the debug dump and the insights collector; the recording is always on); the retained first kontainer comes from
the lookup's own reference to its kontainer (`LazyServiceLookup`). `KontainerTracker` holds kontainers weakly.

The maintainer: "let us rethink how we collect the stats ... probably plain numbers / counters everywhere is totally
enough", and "ok to both":

1. **Counters in place of instance lists.** Each provider keeps a count, the time of the first and of the last
   creation, and the class name of the last created instance (a factory can return a subclass of the registered type).
   All fixed-size; no reference to an instance is kept. `KontainerTools` (dump, `DebugInfo`) and the insights collector
   and GUIs (`funktor/insights` `KontainerCollector`, `funktor/inspect` `KontainerTab`, `slices.kt`, the Vue GUI) show
   the count and the two times where they list every instance today. The insights wire shape changes: plan it with
   the insights track's rules (records on disk, the graph API).
2. **A global singleton's lazy injections (`Lookup<T>`, `Lazy<T>`, `Lazy<List<T>>`) bind to a blueprint-wide kontainer
   without request data**, not to the kontainer of the request that happened to create the singleton. By definition
   a global singleton reaches nothing dynamic, so both kontainers resolve the same services for it; prove that with
   the characterization specs.

Added from the K1 review (implementation reviewer, 2026-11-14, by reading): the retained first kontainer also keeps
that request's override lambdas and values reachable for the blueprint's lifetime (the kontainer holds them for
`clone`). That is the same class of retention as ULTRA-00337, by another path; change 2 above removes it.

## The work

- Decide what a `Lookup` in a global singleton is bound to (promotion to SemiDynamic already happens when `T` is dynamic; the gap is prototypes and the record of instances). At least: no unbounded record of prototype instances behind a long-lived lookup.
- Tests first: the "CURRENT BEHAVIOUR" pin turns into the wanted expectation (RED before the fix); mutation check.
- CHANGELOG: a behaviour change without a compile signal.

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's word.
