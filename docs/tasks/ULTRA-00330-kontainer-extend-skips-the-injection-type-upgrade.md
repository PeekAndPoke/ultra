# ULTRA-00330: Kontainer: extend skips the injection type upgrade

> **Status (2026-11-14): NOT STARTED.** 
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

**Found by:** the characterization specs of ULTRA-00319 (Kontainer injection type split), step 1, 2026-11-14.

## Why this exists

In `base.extend { singleton(X) }`, a prototype or dynamic `X` becomes a plain `Singleton`: `extend` does not apply the type upgrade that a redefinition inside one builder applies. The base blueprint's singletons that inject `X` lose their promotion to SemiDynamic, so a service that was per request silently becomes shared across requests.

Pinned as it is today in `ultra/kontainer/src/test/kotlin/e2e/characterization/RedefinitionCharacterizationSpec.kt`, in a test
whose name starts with "CURRENT BEHAVIOUR". The split of ULTRA-00319 keeps this behaviour unchanged; the fix is this
ticket (maintainer, 2026-11-14: "fix each with its own ticket").

## The work

- `extend` applies the same redefinition rule as the builder, or refuses a redefinition that lowers the type.
- Tests first: the "CURRENT BEHAVIOUR" pin turns into the wanted expectation (RED before the fix); mutation check.
- CHANGELOG: a behaviour change without a compile signal.

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's word.
