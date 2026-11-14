# ULTRA-00346: Kontainer: the ServiceProviderFactory constructor becomes internal

> **Status (2026-11-14): DESIGNED.** Decided by the maintainer; track D builds it.
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

## History

- 2026-11-14: created on the maintainer's decision.
