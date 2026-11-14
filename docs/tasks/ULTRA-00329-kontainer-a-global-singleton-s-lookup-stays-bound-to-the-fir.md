# ULTRA-00329: Kontainer: a global singleton's Lookup stays bound to the first kontainer and keeps every prototype it creates

> **Status (2026-11-14): NOT STARTED.** 
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

**Found by:** the characterization specs of ULTRA-00319 (Kontainer injection type split), step 1, 2026-11-14.

## Why this exists

A global singleton that injects `Lookup<T>` keeps the lookup of the first kontainer it was created in. Prototypes fetched through it are created by, and recorded in, that kontainer's provider, also when the singleton is used from later kontainers: the provider's `instances` list keeps every one, and the singleton keeps that kontainer alive. Memory grows for as long as the lookup is used; a per-request service can reach into another request's kontainer.

Pinned as it is today in `ultra/kontainer/src/test/kotlin/e2e/characterization/InjectionFormsCharacterizationSpec.kt`, in a test
whose name starts with "CURRENT BEHAVIOUR". The split of ULTRA-00319 keeps this behaviour unchanged; the fix is this
ticket (maintainer, 2026-11-14: "fix each with its own ticket").

## The work

- Decide what a `Lookup` in a global singleton is bound to (promotion to SemiDynamic already happens when `T` is dynamic; the gap is prototypes and the record of instances). At least: no unbounded record of prototype instances behind a long-lived lookup.
- Tests first: the "CURRENT BEHAVIOUR" pin turns into the wanted expectation (RED before the fix); mutation check.
- CHANGELOG: a behaviour change without a compile signal.

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's word.
