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

## Plan as built (track D, batch K1, 2026-11-14)

- **Fix:** `DynamicsChecker.getUnexpected` looks up with the given set and stores a copy (`given.toSet()`) as the key.
- **Test first** (`KontainerRetentionSpec`):
  - A kontainer gets an override capturing a marker.
  - Every reference is dropped, inside a helper function so no local keeps one.
  - A `WeakReference` to the marker must clear within a bounded number of GC attempts. It is RED before the fix.
- **A second test:** an override that was used, with the kontainer collected, leaves neither its lambda's capture nor
  its value reachable.

## Built (batch K1, 2026-11-14)

As planned. Confirmed first: both retention tests were RED before the fix (the captured marker was never collected),
green after.

Tests, `KontainerRetentionSpec`:
- an unused override, kontainer dropped: the lambda's capture is collected
- a used override, kontainer dropped: neither the capture nor the value stays reachable
- a held override provider after first use: the capture is collected (ULTRA-00333)

Mutation: KC, the cache keyed by the live view again: RED, 2 / 524.

Changelog (fix): `ultra/kontainer`: the dynamics check no longer keeps the first request's override lambdas, and
what they capture, alive for the life of the blueprint (ULTRA-00337).

Commits:
- `9fe4095b`: the plans
- `a4977d16`: today's built-in behaviour pinned
- `dc73ee47`: tests first and the fix, 115 tests red before it and all green after
- `55ff797f`, `cf5358fc`, `d6ed8dd9`: follow-ups

Suites after K1 (XML, `--rerun`, 0 failures):

| Suite | Tests |
|---|---|
| `:ultra:kontainer:test` | 525 (420 before K1) |
| `:ultra:log:jvmTest` | 74 (no log file touched) |
| `:ultra:vault:jvmTest` | 287 |
| `:funktor:insights:jvmTest` | 118 (117 before) |
| `:funktor:all:jvmTest` | 289 |

Repo-wide compile sweep: green, 459 tasks, 0 errors.

## The work

- Confirm first: a test that creates a kontainer with an override capturing a marker object, drops every reference,
  and shows the marker is still reachable from the blueprint (a `WeakReference` that does not clear after GC).
- Fix: key the cache with a copy (`given.toSet()`).
- Mutation check; CHANGELOG (fix).

## Acceptance

- The marker is collected; `:ultra:kontainer:test` green.

## History

- 2026-11-14: created on the maintainer's standing word for Kontainer oddities ("fix each with its own ticket").
