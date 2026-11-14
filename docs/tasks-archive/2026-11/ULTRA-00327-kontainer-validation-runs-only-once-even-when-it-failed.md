# ULTRA-00327: Kontainer: validation runs only once, even when it failed

> **Status (2026-11-14): DONE.** Batch K2, merged into master as `8778c4a9`: strict gate round 1 CLEAN. Behaviour changed; the docs follow in ULTRA-00338 (kontainer docs).
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
| Validate on every create instead of remembering | equivalent: same outcome, only the cost differs |
| D3 (gate round 1): the guard in `createChecked` deleted (the public `ServiceProviderFactory` path) | RED, 1 / 561 |

`validate()` builds a throwaway kontainer, and a blueprint with `trackKontainers` tracks it. That is not observable
from outside (the tracker is internal), so it does not tell the two equivalent mutants apart.

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

## K2 gate round 1 (strict, blind, reviewer-fable-xhigh)

The same ledger is written into ULTRA-00327, 00328, 00334 and 00335.

**Verdicts:** implementation CLEAN (6 MINOR); domain and security CLEAN (6 MINOR); no CRITICAL, no MAJOR. Neither
reviewer ran anything. The close-out has no production change (tests and texts only; commits `54b8e8c1`,
`a9ccf8c0`, plus the ticket commit).

**Found CLEAN:**
- Both reviewers traced all seven parameter providers against master: kinds and outcomes are unchanged outside
  rules 1 to 4.
- Rule 3 (an ambiguous single-service injection gets no cycle edges) cannot hide a real cycle.
- The nullable survey of seven is exact; funktor-demo adds none.
- There is no `List` cycle in the repo.

### Findings and outcomes

1. **The "exactly one announced type" conjunct was unpinned.** New test: `PartOwner(all: List<Part>)` with
   `MidPart(owner)` and its subclass `LeafPart`, all registered, is a cycle. Mutant D1 ("ambiguous when any announced
   type has several candidates"): RED, 1 / 561.
2. **The composite in its own list.** New tests:
   - `CompositeHandler(all: List<HandlerBase>) : HandlerBase` is `Circular dependency detected: CompositeHandler ->
     CompositeHandler`.
   - Characterization row: with `Lazy<List<HandlerBase>>` it boots, and the list contains the composite itself.
   - Mutant D2 (the owner left out of its own edges): RED, 2 / 561.
3. **The guard in `createChecked`.** New test through the public `ServiceProviderFactory(...)` constructor and
   `newKontainer` on an invalid blueprint that never ran `create`: `KontainerInconsistent`. Mutant D3 (guard deleted):
   RED, 1 / 561.
4. **Rule 2 for the other forms.** One test: a `List` cycle is found when the owner is a factory, a dynamic or a
   prototype. Mutant L2 (cycle check skips `List`), re-run: RED, 5 / 561.
5. **`Lazy<Absent?>` with no candidate is null.** Added to the existing test. Mutant D5 (that `ForLazyService` arm
   turned into an error): RED, 2 / 561.
6. **Literal class names** in the expectations K2 added (`ValidationCharacterizationSpec`,
   `InjectionFormsCharacterizationSpec`); master's older ones are left.
7. **Evidence.** The other suites that build a blueprint were run: `funktor:core`, `rest`, `auth`, `messaging`,
   `logging`, `codegen`, `saas`, and `funktor-demo:server` (it has a `test` task). All green; counts below.
8. **ULTRA-00334 changelog** is worded for the general case, with the `A(i: I)`, `B(a: A) : I`, `C : I` example and
   the parameter errors the cycle used to hide. Both are pinned by a new test, which mutant A1 kills as well (RED,
   3 / 562).
9. **ULTRA-00328 changelog:** it can stop an app at boot; the composite in its own list; `Lazy<List<T>>` only defers
   the cycle; the `Lazy<T>` hint helps only when `.value` is read after construction.
10. **ULTRA-00328 "Built":** "all other promotion and injection-form characterization tests passed untouched"; the
    one that changed is the ambiguous nullable pin, turned by ULTRA-00335.
11. **ULTRA-00335 "Built":** the `Lazy<T?>` claim with no candidate is now pinned (item 5); mutant recorded.
12. **ULTRA-00327 mutation table:** "validate on every create instead of remembering" is recorded as equivalent next
    to Q3. `validate()` tracks its throwaway kontainer, which is not observable from outside.
13. **This ledger and the status line.**

### Counts after round 1 (XML, `--rerun`, 0 failures)

| Suite | Tests |
|---|---|
| `:ultra:kontainer:test` | 562 |
| `:ultra:log:jvmTest` | 74 |
| `:ultra:vault:jvmTest` | 287 |
| `:funktor:insights:jvmTest` | 118 |
| `:funktor:cluster:jvmTest` | 162 |
| `:funktor:all:jvmTest` | 289 |
| `:funktor:core:jvmTest` | 775 |
| `:funktor:rest:jvmTest` | 124 |
| `:funktor:auth:jvmTest` | 322 |
| `:funktor:messaging:jvmTest` | 67 |
| `:funktor:logging:jvmTest` | 14 |
| `:funktor:codegen:test` | 104 |
| `:funktor:saas:jvmTest` | 50 |
| `:funktor-demo:server:test` | 52 |

The kontainer suite had 561 tests in the run of all fourteen suites; it went to 562 with the item 8 test, re-run
alone. No blueprint in the repo turned red in any of these suites.

## The work

- The flag is set only after a successful validation (or the failure is remembered and rethrown). A blueprint that failed validation fails on every `create()` with the same `KontainerInconsistent`.
- Tests first: the "CURRENT BEHAVIOUR" pin turns into the wanted expectation (RED before the fix); mutation check.
- CHANGELOG: a behaviour change without a compile signal.

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's word.
- 2026-11-14: DONE.
