# ULTRA-00355: Kontainer: the override given last wins, also across clone

> **Status (2026-11-25): IN PROGRESS.** Maintainer chose the fix; implementation next.

## Why this exists

Found by the ULTRA-00338 docs review and confirmed by a probe: when two overrides hit one dynamic service under
different classes, a class given again keeps its FIRST place, so it can lose to an override given before it.

- `create { with(Base){A}; with(Derived){B} }.clone { with(Base){C} }` yields B: the clone's new override is
  silently not used.
- `create { with(Base){A}; with(Derived){B}; with(Base){C} }` yields B.

Cause: `DynamicOverrides.merge` is `this.overrides + other.overrides` and the builder writes `overrides[srv] = ...`;
both keep an existing key's position in the LinkedHashMap. `ServiceProviderFactory` builds the override map with
`associate`, so the last entry in iteration order wins per overridden service.

## Decision (maintainer, 2026-11-25)

"Fix: last given wins". A class given again moves to the end: the override given last wins, and a clone's overrides
take precedence over the parent's. This makes the docs' "the later one wins" and "new overrides take precedence" true.

## The work

- Tests first, RED against the current code: both examples above, plus the plain cases that must stay (one override,
  two classes in order, clone overriding the same class as the parent).
- Builder: remove then put. `merge`: `(this.overrides - other.overrides.keys) + other.overrides`.
- Mutation check of both spots; kontainer suite green.
- CHANGELOG (coordinator): Fixes.

## Acceptance

- The override given last wins in one `create`, and across `create` plus `clone`; every other case unchanged.

## History

- 2026-11-25: created on the maintainer's decision.
- 2026-11-25: IN PROGRESS.
