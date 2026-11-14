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

## Plan as built (track D, batch K1, 2026-11-14)

- **Registration: refused at the defining call**, so the stack trace points at the line.
  - Covered: the `singleton`, `prototype`, `dynamic` and `dynamicPrototype` service builders (every overload goes
    through them) and both `instance()` forms, inside `kontainer { }`, a module and `extend { }`.
  - `KontainerInconsistent("<class> is managed by the kontainer itself and cannot be registered")`.
  - The kontainer's own registration of the three bypasses the check.
  - Today's behaviour is pinned first, in every form, as CURRENT BEHAVIOUR.
- **Override: refused at `create` and `clone`**, through the shared entry point of ULTRA-00332, before the dynamics
  check.
  - `KontainerInconsistent("<class> is managed by the kontainer itself and cannot be overridden")`.
  - `Kontainer` and `InjectionContext` leave the set of overridable dynamics.
- Matching is by class equality. All three classes are final, so no subclass can be registered under them.
- Callers: no code registers or overrides a built-in (searched).

## Built (batch K1, 2026-11-14)

As planned. Registration is refused at the defining call: a statement after the registration in the same block does
not run, which a test pins.

Today's behaviour, pinned first and confirmed by a run (commit `a4977d16`, 95 tests): every registration form of the
three was accepted silently and the built-in still provided its value. The form could change what it reported:
- a `prototype` registration made the built-in (Prototype, Static)
- a `dynamicPrototype` registration made it (Prototype, Dynamic)
- a `dynamic` registration made `KontainerBlueprint` (Singleton, Dynamic)

Overrides of `Kontainer` and `InjectionContext` were accepted at `create` and `clone`; `KontainerBlueprint` was
refused as "Unexpected dynamics were provided" at `create` and threw `ServiceNotFound` at `clone`.

Pins turned, before -> after:
- `BuiltInServicesCharacterizationSpec`:
  - 90 registration pins ("accepted, the built-in wins") -> refused with
    `KontainerInconsistent("<class> is managed by the kontainer itself and cannot be registered")`
  - 5 override pins -> refused with `KontainerInconsistent("<class> is managed by the kontainer itself and cannot be
    overridden")`
  - plus one test with a built-in and an unexpected class together: the built-in is named first.
- `DynamicsAtCreateCharacterizationSpec`, "CURRENT BEHAVIOUR: an override for the Kontainer itself is accepted ..." ->
  refused, same message.

Mutation (failed / 524):

| Mutant | Result |
|---|---|
| KB1 to KB4: refusal removed from `singleton` / `prototype` / `dynamic` / `dynamicPrototype` | RED, 18 each |
| KB5 and KB6: refusal removed from `instance(value)` / `instance(class, value)` | RED, 9 each |
| KB7: override refusal removed | RED, 7 |
| KB8, KB9, KB10: `Kontainer` / `InjectionContext` / `KontainerBlueprint` taken out of the built-in set | RED, 32 / 33 / 32 |

Removed while building: the built-ins were also taken out of the overridable set. That never mattered, since the
built-in check runs first on the only path that reads the set.

Changelog (behaviour change):
- `ultra/kontainer`: `Kontainer`, `InjectionContext` and `KontainerBlueprint` cannot be registered. Every form of
  `singleton`, `prototype`, `dynamic`, `dynamicPrototype` and `instance` is refused, in `kontainer { }`, a module and
  `extend`: `KontainerInconsistent("<class> is managed by the kontainer itself and cannot be registered")`, thrown at
  the defining call. Before, the registration was accepted silently and the built-in still provided its value, though
  the declared form could change its reported type and kind.
- `ultra/kontainer`: they cannot be overridden at `create` or `clone`:
  `KontainerInconsistent("<class> is managed by the kontainer itself and cannot be overridden")`. Before, overrides of
  `Kontainer` and `InjectionContext` were accepted, and services injecting `Kontainer` got the foreign one.
  `KontainerBlueprint` was refused as "Unexpected dynamics were provided" at `create` and with `ServiceNotFound` at
  `clone` (ULTRA-00336).

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

- First pin today's behaviour in `ultra/kontainer/src/test/kotlin/e2e/characterization/` as "CURRENT BEHAVIOUR" (track D
  does this with the round-1 fixes of ULTRA-00319), so the scenario is confirmed by a run.
- Refuse an override of the built-in services at `create` and `clone` with `KontainerInconsistent`.
- Tests first, mutation check, CHANGELOG (behaviour change without a compile signal).

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's standing word for Kontainer oddities ("fix each with its own ticket").
