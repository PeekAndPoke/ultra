# ULTRA-00335: Kontainer: a nullable parameter with several candidates injects null without a validation error

> **Status (2026-11-14): BUILT** on `wt/kontainer`, batch K2, not merged; a strict gate follows.
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
  (unchanged).

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

## The work

- First pin today's behaviour in `ultra/kontainer/src/test/kotlin/e2e/characterization/` as "CURRENT BEHAVIOUR" (track D
  does this with the round-1 fixes of ULTRA-00319), so the scenario is confirmed by a run.
- Decide: an ambiguous nullable parameter is a validation error like an ambiguous non-null one, or stays null and then does not count as a dependency.
- Tests first, mutation check, CHANGELOG (behaviour change without a compile signal).

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's standing word for Kontainer oddities ("fix each with its own ticket").
