# ULTRA-00345: Kontainer: an override naming a registered dynamic exactly replaces that one (exact match wins)

> **Status (2026-11-14): DESIGNED.** Decided by the maintainer; track D builds it.
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))
> **Plan:** ULTRA-00316 (close the open epics from the foundation upwards)

**Found by:** the delta review of ULTRA-00344 (eager overrides again), 2026-11-14.

## Why this exists

An override is given by a class; the kontainer finds the service it replaces by asking which registered dynamics
that class is assignable to. With a base and a more specific service both registered, the specific one can never be
overridden:

```kotlin
open class UserProvider
class AdminUserProvider : UserProvider()

val blueprint = kontainer {
    dynamic(UserProvider::class)
    dynamic(AdminUserProvider::class)
}

blueprint.create { with(UserProvider::class) { UserProvider() } }            // works
blueprint.create { with { AdminUserProvider() } }                            // ServiceAmbiguous
blueprint.create { with(AdminUserProvider::class) { AdminUserProvider() } }  // ServiceAmbiguous
```

"Service AdminUserProvider is ambiguous. It has multiple candidates: UserProvider, AdminUserProvider": the lookup
never checks whether one candidate is the class itself. The maintainer: "certainly not wanted".

## Decided (maintainer, 2026-11-14)

"Yes the exact match wins, if not exact match use the current method".

- The class given to `with` is itself a registered dynamic: the override replaces that service and nothing else.
- No exact match: as today. One registered dynamic the class fits is replaced; several is `ServiceAmbiguous`; none
  is refused as unexpected.
- Not changed: injection. A service injecting `UserProvider` in the blueprint above stays a validation error
  ("parameter is ambiguous", ULTRA-00334).

## The work

- The base-type lookup for overrides prefers the exact class; one place, used by the pre-check in `createChecked`
  and by the factory.
- Tests: the three calls above; `with { SuperAdmin() }` with `SuperAdmin : AdminUserProvider` not registered stays
  `ServiceAmbiguous`; the same at `clone`; mutation checks.
- Changelog: `## Unreleased`, new behaviour.

## Acceptance

- The table of the decision holds at `create` and `clone`; kontainer and the suites that build a blueprint are
  green; the gate is closed.

## History

- 2026-11-14: created on the maintainer's decision.
