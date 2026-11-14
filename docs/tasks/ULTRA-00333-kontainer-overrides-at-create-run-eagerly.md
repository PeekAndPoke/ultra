# ULTRA-00333: Kontainer: overrides at create run eagerly

> **Status (2026-11-14): NOT STARTED.** 
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

**Found by:** the characterization specs of ULTRA-00319 (Kontainer injection type split), step 1, 2026-11-14.

## Why this exists

The lambda in `create { with { X() } }` runs at create, even when `X` is never requested from that kontainer; `clone()` runs it again.

Pinned as it is today in `ultra/kontainer/src/test/kotlin/e2e/characterization/DynamicsAtCreateCharacterizationSpec.kt`, in a test
whose name starts with "CURRENT BEHAVIOUR". The split of ULTRA-00319 keeps this behaviour unchanged; the fix is this
ticket (maintainer, 2026-11-14: "fix each with its own ticket").

## The work

- Decide: keep eager (simple, an override is a value) and document it, or make it lazy. If it stays, the KDoc of `with` says so.
- Tests first: the "CURRENT BEHAVIOUR" pin turns into the wanted expectation (RED before the fix); mutation check.
- CHANGELOG: a behaviour change without a compile signal.

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's word.
