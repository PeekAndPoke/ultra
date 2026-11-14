# ULTRA-00335: Kontainer: a nullable parameter with several candidates injects null without a validation error

> **Status (2026-11-14): BUILT** on `wt/kontainer`, batch K2, not merged: gate round 1 CLEAN.
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

## Why this exists

A nullable constructor parameter whose type has two or more candidate services is injected as `null`, and validation reports nothing (`ParameterProvider.kt:160-169`, `TypeLookup.kt:108-116`). The service still counts as depending on those candidates, so it is promoted to SemiDynamic when one of them is dynamic, and `semiDynamicBecauseOf` can name a dynamic service it never receives. Found by reading (domain reviewer, round 1 of ULTRA-00319); unchanged by the split; not yet pinned by a test.

## Decided (maintainer, 2026-11-14)

"Yes the ambiguous error must be raised here." Nullable means "may be absent", nothing more:

| Candidates | Non-null `T` | Nullable `T?` today | Nullable `T?` decided |
|---|---|---|---|
| none | error: misses a dependency | `null` | `null` |
| exactly one | injected | injected | injected |
| two or more | error: ambiguous | `null`, silently | error: ambiguous |

The same for `Lazy<T?>`. The existing message names the colliding services. With the error, an ambiguous parameter
no longer makes a service SemiDynamic for a dynamic service it never receives.

A behaviour change that can stop an app at boot: before building, list every nullable service parameter in the repo
(funktor's optional injections, e.g. `RestCodec` with `Database?` and `EntityCache?`) with its candidates.

## Plan as built (track D, batch K2, 2026-11-14)

- `ParameterProvider.ForService.validate` and `ForLazyService.validate`: a nullable parameter with no candidate is
  fine (null), with one is fine, with two or more gets the existing "is ambiguous" message.
- `provide` is unchanged.
- Survey before building: the `funktor:all` app blueprint has seven nullable service parameters, each with exactly
  one candidate, so none turns red:
  - `AppConfigCollector`: `AppConfig?`, `AppInfo?`
  - `Mailing`: `Log?`
  - `RestCodec`: `Database?`, `EntityCache?`
  - `JwtSigningKeyOnAppStarting`: `Log?`
  - `BackgroundJobs`: `Lazy<EntityCache?>`

## Built (batch K2, 2026-11-14)

**Status: built on `wt/kontainer`, not merged; a strict gate follows.**

As planned: `ForService.validate` and `ForLazyService.validate` report two or more candidates as ambiguous, nullable
or not. `provide` is unchanged.

Survey before building (the `funktor:all` app blueprint): seven nullable service parameters, each with exactly one
candidate. All suites are green afterwards.

Pins turned, before -> after:
- `InjectionFormsCharacterizationSpec`, "CURRENT BEHAVIOUR: a nullable parameter with two candidates injects null
  without a validation error, and promotes": null and SemiDynamic -> `Parameter 'base' is ambiguous. The following
  services collide: SingleImpl, DynImpl`.
- "CURRENT BEHAVIOUR: a Lazy nullable parameter with two candidates yields null ..." (pinned first in `8dffdf22`):
  null -> the same ambiguity error.
- New: a nullable parameter with one candidate is injected and with none is null, for `T?` and `Lazy<T?>`
  (unchanged). `Lazy<Absent?>` with no candidate was added in gate round 1. Mutant D5 (that arm of
  `ForLazyService` turned into an error) is RED, 2 / 561.

Mutation:

| Mutant | Result |
|---|---|
| N35a: `T?` with several candidates allowed again | RED, 2 / 555 |
| N35b: `Lazy<T?>` with several candidates allowed again | RED, 1 / 555 |
| N35c: `T?` with no candidate turned into an error | RED, 3 / 556, with `generateDocs` skipped (an example also fails on it) |

Changelog (behaviour change, no compile signal, can stop an app at boot): `ultra/kontainer`: a nullable injection,
`T?` or `Lazy<T?>`, whose type has two or more candidate services is a validation error ("Parameter '...' is
ambiguous. The following services collide: ..."). Before, it injected null silently, and still made the service
SemiDynamic when a candidate was dynamic. With no candidate it is still null; with one it is injected. In the
`funktor:all` blueprint every nullable service parameter has exactly one candidate (ULTRA-00335).

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

- First pin today's behaviour in `ultra/kontainer/src/test/kotlin/e2e/characterization/` as "CURRENT BEHAVIOUR" (track D
  does this with the round-1 fixes of ULTRA-00319), so the scenario is confirmed by a run.
- Decide: an ambiguous nullable parameter is a validation error like an ambiguous non-null one, or stays null and then does not count as a dependency.
- Tests first, mutation check, CHANGELOG (behaviour change without a compile signal).

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's standing word for Kontainer oddities ("fix each with its own ticket").
