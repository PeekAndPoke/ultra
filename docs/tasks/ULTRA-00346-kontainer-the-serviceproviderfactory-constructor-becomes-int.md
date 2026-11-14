# ULTRA-00346: Kontainer: the ServiceProviderFactory constructor becomes internal

> **Status (2026-11-14): BUILT.** Built by track D on `wt/kontainer` (`cfd272ab`), not merged; a strict gate follows.
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))
> **Plan:** ULTRA-00316 (close the open epics from the foundation upwards)

**Found by:** the review of ULTRA-00344 (eager overrides again), 2026-11-14.

## Why this exists

The public constructor `ServiceProviderFactory(blueprint, dynamics, ...)` runs the override lambdas without
validation and without the built-in, dynamics and ambiguity checks, and `newKontainer` then runs them again for the
kontainer it returns. It is reachable with `DynamicOverrides` taken from an existing kontainer's factory. It is the
one path around the rule that every refusal comes before any lambda.

## Decided (maintainer, 2026-11-14)

"make ctor internal". Kontainers are made by `create` and `clone` only.

## The work

- The constructor becomes `internal`. Check what else of the class has to follow (`newKontainer`,
  `Kontainer.getServiceProviderFactory()`, `dynamics`) so that no public path constructs a factory or runs lambdas
  unchecked; change only what that needs and report the rest.
- The "CURRENT BEHAVIOUR" pin of the public constructor path goes or turns into an internal test; the class KDoc
  sentence about the unchecked constructor goes.
- Callers in the repo outside the module: none expected; search and say.
- Changelog: `## Unreleased`, breaking change.

## Acceptance

- No public API constructs a `ServiceProviderFactory`; all suites that build a blueprint are green; the gate is
  closed.

## Built (track D, 2026-11-14)

**Status: built on `wt/kontainer`, commit `cfd272ab`, not merged; a strict gate follows.**

- **Changed:**
  - `class ServiceProviderFactory internal constructor(...)`
  - `internal val dynamics`: the override lambdas are no longer handed out, so no `DynamicOverrides` of an existing
    kontainer can be reused from outside the module
  - the class KDoc sentence about the unchecked constructor is gone
- **Stays public, and why that is harmless:**
  - `blueprint`, `getProvider`, `getAllProviders`, `isProviderCreated`: no override lambda runs through them
  - `newKontainer`: checked like `create`; outside the module there is no way to get a `DynamicOverrides` to pass,
    since its builder's `build()` is internal and `dynamics` is now internal too
  - `Kontainer.getServiceProviderFactory()` and `KontainerTools.factory`: insights reads providers through them
- **Callers outside the module:** only `funktor/insights` (`KontainerCollector`, `KontainerWireNamesSpec`,
  `KontainerCollectorSlumberSpec`), all through `tools.factory.getAllProviders()` or `getProvider(...)`. No caller of
  the constructor or of `dynamics` anywhere in the repo. `InsightsKontainerSliceSpec` in `funktor/all` names
  `ServiceProviderFactory.<init>` only inside a recorded JSON fixture from 2026-10-19; it is not compared against
  live code.

Tests:
- New `ServiceProviderFactoryVisibilitySpec` pins by reflection that every constructor is `INTERNAL` and that
  `ServiceProviderFactory::dynamics` is `INTERNAL` (both red before the change).
- The "CURRENT BEHAVIOUR" pin of the public constructor path in `DynamicsAtCreateCharacterizationSpec` is gone
  (removed with ULTRA-00345, `5762cddd`).
- `ValidationCharacterizationSpec`: the test that builds a factory directly stays as an internal test, renamed "The
  checked path through newKontainer validates an invalid blueprint that never ran create".

Mutation (failed / 574; each restore checked with `cmp`, the generated docs file included):

| # | Mutant | Result |
|---|---|---|
| Y1 | Constructor public again | RED, 1 |
| Y2 | `dynamics` public again | RED, 1 |

Suites: the same run as ULTRA-00345 (both commits), fourteen suites green by XML with `--rerun`, kontainer 574;
the repo-wide compile sweep is green (459 tasks, 0 errors). Table in ULTRA-00345.

**Changelog text (`## Unreleased`, breaking change):**
- Kontainer: the `ServiceProviderFactory` constructor and `ServiceProviderFactory.dynamics` are internal; kontainers
  are made by `create` and `clone` only. The public constructor ran the override lambdas without the checks of
  `create` (ULTRA-00346).

## History

- 2026-11-14: created on the maintainer's decision.
- 2026-11-14: built by track D (`cfd272ab`), not merged; a strict gate follows.
