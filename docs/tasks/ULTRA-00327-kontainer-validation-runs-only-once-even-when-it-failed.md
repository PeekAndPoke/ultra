# ULTRA-00327: Kontainer: validation runs only once, even when it failed

> **Status (2026-11-14): BUILT** on `wt/kontainer`, batch K2, not merged; a strict gate follows.
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

## Plan as built (track D, batch K2, 2026-11-14)

- `KontainerBlueprint` replaces the `validated` flag with a remembered result:
  `lazy(LazyThreadSafetyMode.SYNCHRONIZED)` holding the validation message, or null when valid.
  - Every `create` (and the checked path of `clone`) throws a new `KontainerInconsistent` with that message: the
    same type and message each time, with a stack trace pointing at the current caller.
  - The success path is one volatile read, as cheap as the flag.
- **Order unchanged (K1):** the result is checked before the caller's override block runs.
- **Concurrency:** the first caller validates under the lazy's lock. Any other caller blocks until it is done and
  gets the same result. Before, the CAS loser skipped validation and got a kontainer.
- Only a validation failure (`KontainerInconsistent`) is remembered. Any other exception from `validate()` would be a
  defect; it is not cached, so the next `create` validates again.

## Built (batch K2, 2026-11-14)

**Status: built on `wt/kontainer`, not merged; a strict gate follows.**

As planned: the validation result is a `lazy(SYNCHRONIZED)` value holding the message, or null when valid. Every
`create` of an invalid blueprint throws `KontainerInconsistent` with the same message, still before the override block.

**What a second thread sees while the first validates:** it blocks on the lazy's lock until the result is there, then
gets the same result. For an invalid blueprint that is the error; for a valid one, a kontainer.
- Pinned by two tests that release 8 threads at once: all 8 get the same error, or all 8 get a kontainer.
- On the old code the invalid-blueprint test was RED: the CAS losers skipped validation and got kontainers.
- The blocking itself cannot be watched from outside, as validation runs no user code. So the mutant that lets every
  thread validate at once (Q3, `PUBLICATION`) is equivalent: validation is deterministic and has no side effect.

Pins turned, before -> after:
- `ValidationCharacterizationSpec`, "CURRENT BEHAVIOUR: after a failed validation the next create succeeds ...":
  - second `create` succeeded and `get` threw `InvocationTargetException` -> every `create` throws the same
    `KontainerInconsistent` message
- New tests:
  - a later `create` does not run its override block either
  - concurrent invalid creates
  - concurrent valid creates
- `funktor/insights` `KontainerFixture.brokenBlueprint` KDoc: "only the first create fails" -> "every create
  fails". Text only; its one test uses the first `create`.

Mutation:

| Mutant | Result |
|---|---|
| Q1: throw only before the result is cached (later creates pass) | RED, 2 / 555 |
| Q2: throw only once it is cached (first create passes) | RED, 35 / 555 |
| Q3: `PUBLICATION` instead of `SYNCHRONIZED` | SURVIVED, 0 / 555: equivalent, see above |

Changelog (behaviour change, no compile signal): `ultra/kontainer`: a blueprint remembers its validation result.
Every `create` of an invalid blueprint throws `KontainerInconsistent` with the same message, before the override block
runs. Before, only the first did: later creates succeeded, and `get` failed with `InvocationTargetException` or worse.
A `create` arriving while another one validates now waits for the result; before, it skipped validation
(ULTRA-00327).

Commits:
- `8dffdf22`: today's pins
- `4b404195`: the plans
- `01cbd47d`: the fix, with tests first (8 red before it, all green after)
- `2281c29d`: a `List` cycle with two candidates, added for mutant A2

Suites (XML, `--rerun`, 0 failures):

| Suite | Tests |
|---|---|
| `:ultra:kontainer:test` | 556 |
| `:ultra:log:jvmTest` | 74 |
| `:ultra:vault:jvmTest` | 287 |
| `:funktor:insights:jvmTest` | 118 |
| `:funktor:cluster:jvmTest` | 162 |
| `:funktor:all:jvmTest` | 289 |

The repo-wide compile sweep is green (459 tasks, 0 errors). No blueprint in the repo turned red.

## The work

- The flag is set only after a successful validation (or the failure is remembered and rethrown). A blueprint that failed validation fails on every `create()` with the same `KontainerInconsistent`.
- Tests first: the "CURRENT BEHAVIOUR" pin turns into the wanted expectation (RED before the fix); mutation check.
- CHANGELOG: a behaviour change without a compile signal.

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's word.
