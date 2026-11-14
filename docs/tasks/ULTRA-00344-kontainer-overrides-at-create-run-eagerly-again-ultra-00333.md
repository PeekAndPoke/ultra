# ULTRA-00344: Kontainer: overrides at create run eagerly again (ULTRA-00333 reverted)

> **Status (2026-11-14): BUILT** on `wt/kontainer`, not merged; a strict gate follows.
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))
> **Plan:** ULTRA-00316 (close the open epics from the foundation upwards)

## Why this exists

ULTRA-00333 (batch K1, merged `f575dd29`, not released) made the lambda of `create { with { X() } }` lazy: it ran
at the first request of the service from that kontainer. Discussing what should happen when such a lambda throws
showed the cost: a failure moves from `create` to whichever service first injects the override, on whichever
thread; a retry after a throw can heal, but can also leave one service that saw the failure next to another that
got a value; a clone runs its parent's lambdas later and concurrently; the kontainer keeps lambdas and a lock per
override. Every override in the repo is a ready value or a cheap wrapper (`with { call.currentUserProvider() }`
builds a `UserProvider.lazy { }`), so laziness saved no work.

## Decided (maintainer, 2026-11-14)

"Ok then back to eager and if the user need laziness it can be built in place, like UserProvider.lazy {} ..."

- The lambda of `create { with { ... } }` is a factory that runs exactly once per kontainer, at `create`.
- A throwing override fails `create`: a kontainer exists completely or not at all. No retry, nothing remembered.
- `clone()` runs the lambdas again for the clone, as before K1, so a clone has its own instances.
- A caller who needs laziness builds it in place, as `UserProvider.lazy { }` does.

## The work

- Revert the laziness of ULTRA-00333 only: `ServiceProviderFactory` runs the override at create;
  `ServiceProvider.ForOverride` holds the instance again; the provider lock and the "until it has returned a
  value" state go.
- Everything else of K1 stays: the redefinition rule (ULTRA-00331), `extend` (ULTRA-00330), `clone` checking like
  `create` through the one entry point (ULTRA-00332), the built-ins (ULTRA-00336), the checker keying its cache
  with a copy (ULTRA-00337), and the K2 order: validation before the override block.
- Tests: the eager behaviour pinned as decided (not as "CURRENT BEHAVIOUR"); the lazy specs removed or turned.
- `CHANGELOG.md` `## Unreleased`: the ULTRA-00333 entry goes, since eager is what the last release did; what
  remains different from the last release is said under the tickets that cause it.

## Acceptance

- `create { with { X() } }` runs the lambda at `create`, once; `clone()` runs it once more for the clone; a
  throwing lambda fails `create` or `clone`.
- Kontainer and the suites that build a blueprint are green; the gate is closed.

## Built (track D, 2026-11-14)

**Status: built on `wt/kontainer`, not merged; a strict gate follows.**

- **The revert.** `ServiceProvider.kt` and `ServiceProducer.kt` are byte-identical to `b57e9e1d` (master before K1):
  - `forOverride(definition, instance: Any)`
  - `ForOverride` is a data class holding the instance
  - the pending producer is gone

  `ServiceProviderFactory` runs `provider()` for each override in its init, as before K1, with a comment. The
  provider lock, the "until it has returned a value" state and the texts about it are gone. The KDoc of `with` says
  the lambda runs at create, once per kontainer.
- **Order in `createChecked`:**
  1. validation (remembered, ULTRA-00327)
  2. built-in check (ULTRA-00336)
  3. dynamics check (ULTRA-00332)
  4. instantiate, where the factory resolves each override's base type and runs its lambda

  So a refused override's lambda never runs, at `create` or `clone` (pinned). Validation also comes before the
  caller's override block (K2).
- **A throwing lambda** fails `create` or `clone` with its own exception; no kontainer results and nothing is
  remembered: the next `create` with a working lambda succeeds (pinned).
- **Kept from K1 and K2:** everything else. The redefinition rule, `extend`, the built-ins, the one entry point
  `createChecked`, the checker key copy, and the remembered validation are unchanged; their tests are untouched and
  green.
- **Retention (ULTRA-00337):** a live kontainer still holds its override lambdas (`ServiceProviderFactory.dynamics`),
  because `clone` runs them again. It also holds the values. A collected kontainer leaves nothing reachable from the
  blueprint: both retention tests are green on the eager code.
- **Callers:** nothing in the repo depends on laziness. Every override lambda listed under ULTRA-00333 ran eagerly in
  release 0.118.0, and no production file outside the kontainer changed after the K1 merge. No docs page mentions lazy
  overrides.

Tests (9 red on the lazy code first, all green after):
- `DynamicsAtCreateCharacterizationSpec`, decided, no longer "CURRENT BEHAVIOUR":
  - runs at create, once per kontainer, even when never requested
  - an unrequested override has its instance (`instances`, debug info, dump)
  - a throwing lambda fails `create` with its own exception, no kontainer, nothing remembered
  - a throwing lambda fails `clone`
  - a refused override's lambda never runs, at create (non-dynamic, built-in) and at clone
  - mutable state is read at create
  - the definition names the instance's class from create
  - `clone` runs the lambda once more and gets its own instance
- `LazyOverrideSpec` removed: its concurrency and retry cases have no subject any more.
- `KontainerRetentionSpec`: the provider test removed (the provider never holds a lambda again); the two kontainer
  tests renamed, still green.
- `ToolsCharacterizationSpec`: the clue text is "created at create" again.
- `ScopeGuardsSpec`: `forOverride(definition, instance = ...)` again.
- `funktor/insights` `KontainerWireNamesSpec`:
  - the end-to-end test is back to its pre-K1 form
  - "an override never requested is not in the slice" is turned: it is in the slice, as `DynamicOverride`

Mutation (failed / 561; each restore checked with `cmp`, the generated docs file included):

| # | Mutant | Result |
|---|---|---|
| O1 | Lambda not run at create (the lambda object handed out) | RED, 24 |
| E2 | Lambda run twice | RED, 3 |
| E3 | Clone shares the parent's instance (the builder memoizes each lambda) | RED, 3 |
| E4 | Clone does not run it (clone drops the parent's overrides) | RED, 3 |
| E5 | Exception swallowed | RED, 2 |
| E6 | Lambdas run before the checks | RED, 4 |

Note: the first run of O1 was under the key `E1`, which collided with an older mutant of the same name in the runner;
that run executed the `extend` mutant. It was renamed and re-run, and the runner now has no duplicate keys.

Commit `8d49702e`. Suites (XML, `--rerun`, 0 failures):

| Suite | Tests |
|---|---|
| `:ultra:kontainer:test` | 561 (562 before) |
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

The kontainer suite went from 562 to 561: `LazyOverrideSpec` (2) and the provider retention test (1) went, and two
tests came in (a throwing lambda fails `clone`; a refused override never runs). The repo-wide compile sweep is green
(459 tasks, 0 errors).

**`CHANGELOG.md` `## Unreleased`:**
- The ULTRA-00333 entry goes, whole.
- Still true against 0.118.0 as worded: ULTRA-00319 (`ForOverride(definition, instance)` is what it says), ULTRA-00327,
  ULTRA-00332, ULTRA-00336 (both entries), ULTRA-00337.
- One sentence could be added to ULTRA-00332: no override lambda runs before a refusal at `clone`. In 0.118.0 a
  refused clone ran the parent's override lambdas first and then failed with `ServiceNotFound`.

## History

- 2026-11-14: created on the maintainer's decision, after the discussion of the retry in ULTRA-00333.
