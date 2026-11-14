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

## Decided (maintainer, 2026-11-14)

"yes make override lazy": the lambda of `create { with { ... } }` runs the first time the service is requested from
that kontainer, and its result is kept for that kontainer (at most one run per kontainer). Today it runs at `create`
(`ServiceProviderFactory.kt:22`), and again at `clone()`.

Behaviour changes without a compile signal, for the changelog: an override that throws now throws at the first `get`;
side effects run later, or never when the service is not used; a lambda reading something mutable sees the value at
first use; `clone()` no longer runs the lambda at once. Build it together with ULTRA-00337 (the dynamics checker keeps
the first request's override lambdas alive) and ULTRA-00332 (one entry point for `create` and `clone`).

## The work

- Decide: keep eager (simple, an override is a value) and document it, or make it lazy. If it stays, the KDoc of `with` says so.
- Tests first: the "CURRENT BEHAVIOUR" pin turns into the wanted expectation (RED before the fix); mutation check.
- CHANGELOG: a behaviour change without a compile signal.

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's word.
