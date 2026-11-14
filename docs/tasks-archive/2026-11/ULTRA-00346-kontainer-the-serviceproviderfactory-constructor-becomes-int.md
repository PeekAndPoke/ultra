# ULTRA-00346: Kontainer: the ServiceProviderFactory constructor becomes internal

> **Status (2026-11-14): DONE.** Merged into master as `f750497f`: strict gate round 1 CLEAN; the close-out was tests, texts and one visibility narrowing. Docs: ULTRA-00338 (kontainer docs). close-out done (see Gate round 1), not merged.
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

The changelog text is under Gate round 1, it replaces the one first written here.

## Gate round 1 (strict, reviewer-fable-xhigh, all three charters, with ULTRA-00345)

**Verdict: CLEAN.** No CRITICAL, no MAJOR, 5 MINOR, all closed out under ULTRA-00345. No public path constructs a
factory or runs an override lambda unchecked. The reviewer ran nothing. One MINOR touches this ticket's area:
`dynamicsBaseTypeLookUp` is now `private`, so `overriddenServiceOf` is the one resolver in the module. The surplus
`assertSoftly` in `ServiceProviderFactoryVisibilitySpec` is gone.

**G2 is equivalent single-threaded; the assumption, named:**
- `DynamicOverrides.Builder.build()` hands the builder's live map to `DynamicOverrides`.
- A builder leaked to another thread and mutated between the checks and the factory loop could bring an unchecked key
  into the loop.
- Today such a key would still be refused before its own lambda runs, because the factory resolves the base type
  first. G2 swaps exactly that order.
- No code change.

**For the maintainer:** `newKontainer` stays public and checked, but no caller outside the module has anything to pass
to it. Make it internal?

Counts: see ULTRA-00345, Gate round 1.

**Changelog text (`## Unreleased`, breaking change; replaces the one written at build):**
- `ultra/kontainer`: the `ServiceProviderFactory` constructor and `ServiceProviderFactory.dynamics` are internal;
  kontainers are made by `create` and `clone` only. The public constructor ran the override lambdas without the
  checks of `create` (ULTRA-00346).

## History

- 2026-11-14: created on the maintainer's decision.
- 2026-11-14: built by track D (`cfd272ab`), not merged; a strict gate follows.
- 2026-11-14: strict gate round 1 CLEAN; close-out by track D, not merged.
- 2026-11-14: DONE.
