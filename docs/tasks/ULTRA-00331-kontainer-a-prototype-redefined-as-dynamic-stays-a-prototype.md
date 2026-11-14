# ULTRA-00331: Kontainer: a prototype redefined as dynamic stays a prototype

> **Status (2026-11-14): NOT STARTED.** 
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

**Found by:** the characterization specs of ULTRA-00319 (Kontainer injection type split), step 1, 2026-11-14.

## Why this exists

A prototype redefined as dynamic stays `Prototype`: singletons that inject it are not promoted, and `create { with { } }` refuses an override for it.

Pinned as it is today in `ultra/kontainer/src/test/kotlin/e2e/characterization/RedefinitionCharacterizationSpec.kt`, in a test
whose name starts with "CURRENT BEHAVIOUR". The split of ULTRA-00319 keeps this behaviour unchanged; the fix is this
ticket (maintainer, 2026-11-14: "fix each with its own ticket").

## Decided (maintainer, 2026-11-14)

The maintainer weighed throwing ("The initial author wants the service to be a prototype so making it a singleton
must fail") against allowing the upgrade to a dynamic singleton, and agreed to the rule below: "ok yes we do it like
this."

**The redefinition rule, two sentences:**

1. The first definition fixes what the service is, singleton or prototype. The one exception stays: a plain static
   singleton can be replaced by anything.
2. Request binding is never lost and can always be added: if either definition is dynamic, the service is dynamic.

| Redefinition | Today | With the rule |
|---|---|---|
| `prototype` then `dynamic` | (Prototype, Static) | (Prototype, Dynamic) |
| `prototype` then `dynamicPrototype` | (Prototype, Static) | (Prototype, Dynamic) |
| every other pair | | unchanged |

Silent: no exception, no log line, as for `extend` (ULTRA-00330). Why no upgrade to a dynamic singleton: a prototype
promises every injector its own instance; services written against the first definition may keep state in it, and one
shared instance per request would mix that state without any failure. Why no exception: consistency with ULTRA-00330,
redefinition is the normal way to swap an implementation, and a safe resolution exists.

Consequence, accepted: `create { with { ... } }` for such a service is still refused (only dynamic singletons can be
overridden; the message says so). `extend` follows the same rule (ULTRA-00330).

## Plan as built (track D, batch K1, 2026-11-14)

- `KontainerBuilder.RedefinitionUpgrade.adjust(newDef, existingDef)`:
  - no earlier definition, or the earlier one is a static singleton: the later definition as it is
  - otherwise: type = the earlier type; kind = Dynamic if either is Dynamic, else Static
  - the producer is always the later one, as today
- The rule is associative, so applying it per redefinition (builder) or to a whole extension (`extend`, ULTRA-00330)
  gives the same result for the same sequence of definitions.
- Only `prototype` then `dynamic` and `prototype` then `dynamicPrototype` change, both to (Prototype, Dynamic); the
  other pairs are pinned unchanged.
- Silent; an override at create is refused for such a service, with the existing message and its dynamic prototype
  hint.
- Tests first:
  - the CURRENT BEHAVIOUR pins (`RedefinitionCharacterizationSpec`, `DynamicPrototypeSpec`) turn into the decided
    expectation
  - `InjectionTypeUpgradeSpec` "A Prototype service must not be downgraded to a dynamic" changes with the decision
- Callers: no production code redefines a prototype as dynamic (searched).

## The work

- Decide the redefinition table for all pairs of (ServiceType, ServiceKind), `dynamicPrototype` included, and make builder and `extend` follow it (see ULTRA-00330).
- Tests first: the "CURRENT BEHAVIOUR" pin turns into the wanted expectation (RED before the fix); mutation check.
- CHANGELOG: a behaviour change without a compile signal.

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's word.
