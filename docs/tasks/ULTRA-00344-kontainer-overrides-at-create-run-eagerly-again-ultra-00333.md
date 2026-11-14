# ULTRA-00344: Kontainer: overrides at create run eagerly again (ULTRA-00333 reverted)

> **Status (2026-11-14): BUILT** on `wt/kontainer`, not merged: gate round 1 CLEAN, a delta review follows.
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

## Gate round 1 (strict, reviewer-fable-xhigh, all three charters)

**Verdict:** CLEAN, with no CRITICAL, no MAJOR and 8 MINOR.
- The revert is complete.
- The three decided checks come first.
- Each lambda runs once per kontainer.
- The exception propagates unwrapped.
- No partial state remains.

The reviewer ran nothing; every item was tested first. One item changed production code (one line, plus a KDoc
sentence), so a short delta review follows. Commit `e8bf9a5e`.

### Findings and outcomes

1. **M1, the ambiguity refusal came after earlier lambdas had run.** Survived its test: with
   `create { with { ...; Other() }; with { Inner() } }` (`Inner : Outer`, both dynamic), `Other`'s lambda ran and
   then `ServiceAmbiguous` was thrown. The same happened at `clone`, where the parent's lambda ran again.
   - Both new tests were RED (1 run, and 2 at clone).
   - Fixed: `createChecked` resolves every override's base type after the dynamics check and before `instantiate`.
     The exception type and message are unchanged (pinned literally).
   - `ServiceNotFound` from the lookup cannot be reached there: the dynamics checker and the lookup read the same set,
     so a key that passes the checker has at least one base type.
   - Order when several refusals apply to one call:
     1. validation, which at `create` runs even before the override block
     2. the built-in check (the first built-in key)
     3. the dynamics check (all unexpected keys, named together)
     4. the ambiguity check (the first ambiguous key)

     No override lambda runs before any of them.
2. **M2, the lambda before the base-type lookup.**
   - The ambiguity tests count runs in the ambiguous lambda and in an earlier valid one: both 0.
   - G2 (swap lookup and lambda in the factory): SURVIVED. It is equivalent now, since the pre-check refuses any
     ambiguous key before the factory runs.
   - G1 (pre-check removed): RED, 2.
   - G3 (both together, two files, run by hand with the same backup and `cmp` protocol): RED, 2.
3. **M5, the order of the lambdas.** Pinned:
   - at create, the order of the first `with` per service
   - a second `with` for the same class replaces the lambda in place, and the first never runs
   - at clone, the parent's order first, a clone override for a parent's key at that key's position, new keys last

   G4 (reversed order): RED, 2. G5 (a replaced lambda also run): RED, 1.
4. **M3, two overrides for one service under different classes.** Pinned as CURRENT BEHAVIOUR, unchanged: with
   `dynamic(Base)` and `create { with(Base::class) { First() }; with(Derived::class) { Derived() } }`, both lambdas
   run, the later wins, and `First()` is built for nothing. A question for the maintainer:
   - keep only the last override per service before running
   - refuse two overrides for one service
   - leave it as it is
5. **M4, the public `ServiceProviderFactory(...)` constructor** runs the override lambdas without the checks, and
   `newKontainer` runs them again for the kontainer it returns. Unchanged. One KDoc sentence on the class says so, and
   a CURRENT BEHAVIOUR pin records it: 1 run at create, 2 after the constructor, 3 after `newKontainer`. A question
   for the maintainer: make the constructor internal?
6. **M6 and M7:** the stale comment "ULTRA-00333: overrides are lazy." is deleted; master's comment "ULTRA-00332:
   clone runs the same check as create." is restored above its test.
7. **The changelog advice**, corrected below.
8. **M8**, the stale line in ULTRA-00338: the coordinator's, at the merge.

### Mutation runs, round 1 (failed / 565)

| # | Mutant | Result |
|---|---|---|
| G1 | Base-type pre-check removed | RED, 2 |
| G2 | Lambda before the base-type lookup in the factory | SURVIVED, equivalent (the pre-check comes first) |
| G3 | G1 and G2 together | RED, 2 |
| G4 | Override lambdas run in reverse order | RED, 2 |
| G5 | A replaced lambda also run | RED, 1 |

### Counts after round 1 (XML, `--rerun`, 0 failures)

| Suite | Tests |
|---|---|
| `:ultra:kontainer:test` | 565 (561 before; 4 new tests) |
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

The repo-wide compile sweep is green (459 tasks, 0 errors).

### Changelog advice (replaces the advice above)

- **Goes:** the whole ULTRA-00333 entry.
- **Still true against 0.118.0, as worded:** ULTRA-00319, ULTRA-00327, ULTRA-00332, both ULTRA-00336 entries,
  ULTRA-00337.
- **The refusals at `create` and `clone`:**
  - the remembered validation result (`KontainerInconsistent`)
  - the built-in check (`KontainerInconsistent`)
  - the dynamics check (`KontainerInconsistent`)
  - the ambiguity check (`ServiceAmbiguous`)
  - `ServiceNotFound` from the lookup, unreachable after the dynamics check

  With round 1's fix, no override lambda runs before any of them, at `create` or at `clone`. A lambda that throws is
  not a refusal; it fails with its own exception.
- **So the ULTRA-00332 entry can take this sentence**, true for every refusal: "No override lambda runs before a
  refusal, at `create` or at `clone`. In 0.118.0 a refused clone ran the parent's override lambdas first, and an
  override matching two dynamics was refused only after the lambdas listed before it had run."

## History

- 2026-11-14: created on the maintainer's decision, after the discussion of the retry in ULTRA-00333.
