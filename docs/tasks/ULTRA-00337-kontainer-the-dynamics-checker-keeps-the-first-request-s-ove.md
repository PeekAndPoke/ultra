# ULTRA-00337: Kontainer: the dynamics checker keeps the first request's override lambdas alive

> **Status (2026-11-14): NOT STARTED.** 
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

## Why this exists

`KontainerBlueprint.create` passes `dynamicServices.overrides.keys` to `DynamicsChecker.getUnexpected`, which stores
that object as the key of a `ConcurrentHashMap` (`DynamicsChecker.kt:28`, `getOrPut`). The key is the live `keySet()`
view of the builder's `LinkedHashMap`, so it holds the whole map, including the `() -> Any` override lambdas. For each
distinct set of override classes, the first request's lambdas, and through them whatever they capture (an
`ApplicationCall` with its headers and cookies, the current user), stay reachable from the blueprint until the process
ends.

It is retention only: nothing reads the lambdas back and no other request can see them; it is bounded to one map per
distinct class set. Found by reading (domain and security reviewer, round 2 of ULTRA-00319); the same on master; not
yet confirmed by a test.

## Decided (maintainer, 2026-11-14)

"yes to storing the copy as the cache key": `DynamicsChecker.getUnexpected` keys its cache with a copy of the classes
(`given.toSet()`), not with the live key view of the request's override map. Test first: the retention is proven
(an override capturing a marker, all references dropped, the marker still reachable), then the fix. Built with the
lazy overrides of ULTRA-00333, where the same care applies: after first use the lambda is dropped and only the value
is kept.

## The work

- Confirm first: a test that creates a kontainer with an override capturing a marker object, drops every reference,
  and shows the marker is still reachable from the blueprint (a `WeakReference` that does not clear after GC).
- Fix: key the cache with a copy (`given.toSet()`).
- Mutation check; CHANGELOG (fix).

## Acceptance

- The marker is collected; `:ultra:kontainer:test` green.

## History

- 2026-11-14: created on the maintainer's standing word for Kontainer oddities ("fix each with its own ticket").
