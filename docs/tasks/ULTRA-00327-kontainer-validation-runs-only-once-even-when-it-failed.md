# ULTRA-00327: Kontainer: validation runs only once, even when it failed

> **Status (2026-11-14): NOT STARTED.** 
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

**Found by:** the characterization specs of ULTRA-00319 (Kontainer injection type split), step 1, 2026-11-14.

## Why this exists

After a failed validation the next `create()` of the same blueprint succeeds: the "validated" flag is set before `validate()` runs. `get` on that kontainer then throws `InvocationTargetException` where the caller should have got the validation error again.

Pinned as it is today in `ultra/kontainer/src/test/kotlin/e2e/characterization/ValidationCharacterizationSpec.kt`, in a test
whose name starts with "CURRENT BEHAVIOUR". The split of ULTRA-00319 keeps this behaviour unchanged; the fix is this
ticket (maintainer, 2026-11-14: "fix each with its own ticket").

## Decided (maintainer, 2026-11-14)

"Yes remember the validation result and fail on every create when not valid."

The validation runs once and its result is remembered: a blueprint that failed validation throws the same
`KontainerInconsistent` with the same message on every `create()`. The success path stays as cheap as today.

Overrides do not matter for this (the maintainer asked, checked in the code on 2026-11-14): `validate()`
(`KontainerBlueprint.kt:220`) checks the definitions for cycles and builds one kontainer with NO overrides to ask every
provider whether its dependencies can be met. Both depend only on the definitions, which never change. The check that
does depend on overrides ("Unexpected dynamics were provided") runs on every `create()` after the validation and stays
as it is. An override cannot make an invalid blueprint valid: it can only replace a declared dynamic singleton.

## The work

- The flag is set only after a successful validation (or the failure is remembered and rethrown). A blueprint that failed validation fails on every `create()` with the same `KontainerInconsistent`.
- Tests first: the "CURRENT BEHAVIOUR" pin turns into the wanted expectation (RED before the fix); mutation check.
- CHANGELOG: a behaviour change without a compile signal.

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's word.
