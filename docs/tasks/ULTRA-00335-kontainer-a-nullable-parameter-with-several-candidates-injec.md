# ULTRA-00335: Kontainer: a nullable parameter with several candidates injects null without a validation error

> **Status (2026-11-14): NOT STARTED.** 
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

## The work

- First pin today's behaviour in `ultra/kontainer/src/test/kotlin/e2e/characterization/` as "CURRENT BEHAVIOUR" (track D
  does this with the round-1 fixes of ULTRA-00319), so the scenario is confirmed by a run.
- Decide: an ambiguous nullable parameter is a validation error like an ambiguous non-null one, or stays null and then does not count as a dependency.
- Tests first, mutation check, CHANGELOG (behaviour change without a compile signal).

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's standing word for Kontainer oddities ("fix each with its own ticket").
