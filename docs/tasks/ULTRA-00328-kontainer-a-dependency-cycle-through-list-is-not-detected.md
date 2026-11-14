# ULTRA-00328: Kontainer: a dependency cycle through List is not detected

> **Status (2026-11-14): NOT STARTED.** 
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

**Found by:** the characterization specs of ULTRA-00319 (Kontainer injection type split), step 1, 2026-11-14.

## Why this exists

With `A(List<B>)` and `B(A)`, validation passes and `get(A)` ends in `StackOverflowError`. The cycle check does not follow `List<T>` parameters, although `List` counts as a dependency for promotion.

Pinned as it is today in `ultra/kontainer/src/test/kotlin/e2e/characterization/ValidationCharacterizationSpec.kt`, in a test
whose name starts with "CURRENT BEHAVIOUR". The split of ULTRA-00319 keeps this behaviour unchanged; the fix is this
ticket (maintainer, 2026-11-14: "fix each with its own ticket").

## The work

- Decide whether a cycle through `List`, `Lazy<List>` and `Lookup` is an error at validation (as for plain parameters) or allowed where the injection is lazy; a non-lazy `List` cycle must be reported at validation, not as a stack overflow at `get`.
- Tests first: the "CURRENT BEHAVIOUR" pin turns into the wanted expectation (RED before the fix); mutation check.
- CHANGELOG: a behaviour change without a compile signal.

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's word.
