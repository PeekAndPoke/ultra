# ULTRA-00335: Kontainer: a nullable parameter with several candidates injects null without a validation error

> **Status (2026-11-14): NOT STARTED.** 
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

## Why this exists

A nullable constructor parameter whose type has two or more candidate services is injected as `null`, and validation reports nothing (`ParameterProvider.kt:160-169`, `TypeLookup.kt:108-116`). The service still counts as depending on those candidates, so it is promoted to SemiDynamic when one of them is dynamic, and `semiDynamicBecauseOf` can name a dynamic service it never receives. Found by reading (domain reviewer, round 1 of ULTRA-00319); unchanged by the split; not yet pinned by a test.

## The work

- First pin today's behaviour in `ultra/kontainer/src/test/kotlin/e2e/characterization/` as "CURRENT BEHAVIOUR" (track D
  does this with the round-1 fixes of ULTRA-00319), so the scenario is confirmed by a run.
- Decide: an ambiguous nullable parameter is a validation error like an ambiguous non-null one, or stays null and then does not count as a dependency.
- Tests first, mutation check, CHANGELOG (behaviour change without a compile signal).

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's standing word for Kontainer oddities ("fix each with its own ticket").
