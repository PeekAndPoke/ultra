# ULTRA-00336: Kontainer: the built-in Kontainer service can be overridden at create

> **Status (2026-11-14): NOT STARTED.** 
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

## Why this exists

The built-in `Kontainer` service is declared dynamic (`KontainerBuilder.kt`), so `create { with<Kontainer> { other } }` is accepted: `get(Kontainer::class)` and every service that injects `Kontainer` then get the foreign kontainer, and with it another request's services. The same holds for `InjectionContext`. Found by reading (security reviewer, round 1 of ULTRA-00319); the same on master; not yet pinned by a test.

## Decided (maintainer, 2026-11-14)

"Yes refuse the override for the built-in services. All three i suppose."

`create` and `clone` refuse an override of `Kontainer`, `InjectionContext` and `KontainerBlueprint` with
`KontainerInconsistent`, naming the class and saying that it is managed by the kontainer itself. `KontainerBlueprint`
is a static singleton and is already refused today as "unexpected"; it gets the same clear message as the other two.

Also check, by test first: what happens when a blueprint's own builder defines one of the three
(`singleton(Kontainer::class) { ... }`); the built-ins are added after the builder block ran (`KontainerBuilder.kt:527-546`).
Pin today's result and report it; refuse it the same way if it can replace a built-in.

## The work

- First pin today's behaviour in `ultra/kontainer/src/test/kotlin/e2e/characterization/` as "CURRENT BEHAVIOUR" (track D
  does this with the round-1 fixes of ULTRA-00319), so the scenario is confirmed by a run.
- Refuse an override of the built-in services at `create` and `clone` with `KontainerInconsistent`.
- Tests first, mutation check, CHANGELOG (behaviour change without a compile signal).

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's standing word for Kontainer oddities ("fix each with its own ticket").
