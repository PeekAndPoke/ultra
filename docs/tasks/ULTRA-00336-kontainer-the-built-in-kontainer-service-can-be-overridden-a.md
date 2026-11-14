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

**And they cannot be registered at all** (maintainer, 2026-11-14: "This also means it must not be possible to
register any of these at all."): a blueprint builder, a module or an `extend` block that defines `Kontainer`,
`InjectionContext` or `KontainerBlueprint` (`singleton`, `prototype`, `dynamic`, `dynamicPrototype`, `instance`, by
class or with a factory) is refused with `KontainerInconsistent`, naming the class and saying that it is managed by
the kontainer itself. Test first what happens today (the built-ins are added after the builder block ran,
`KontainerBuilder.kt:527-546`, so a user definition is probably replaced silently): pin it, then refuse it. Decide in
the plan whether the refusal comes when the definition is made or when the blueprint is built; either way before any
kontainer exists.

## The work

- First pin today's behaviour in `ultra/kontainer/src/test/kotlin/e2e/characterization/` as "CURRENT BEHAVIOUR" (track D
  does this with the round-1 fixes of ULTRA-00319), so the scenario is confirmed by a run.
- Refuse an override of the built-in services at `create` and `clone` with `KontainerInconsistent`.
- Tests first, mutation check, CHANGELOG (behaviour change without a compile signal).

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's standing word for Kontainer oddities ("fix each with its own ticket").
