# ULTRA-00333: Kontainer: overrides at create run eagerly

> **Status (2026-11-14): BUILT** on `wt/kontainer`, batch K1, not merged: gate round 1 CLEAN, delta round 2 CLEAN.
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

## Plan as built (track D, batch K1, 2026-11-14)

- **Laziness:**
  - `ServiceProviderFactory` no longer calls the override lambda; `ServiceProvider.ForOverride` holds it.
  - The first `provide` runs it under the provider's lock, keeps the value and drops the lambda. A lambda that throws
    keeps nothing, so the next request runs it again: it runs until it has returned a value, then never again (round
    1).
  - The base-type lookup of an override (`ServiceAmbiguous` for an ambiguous one) stays at create.
- **`clone()`:** does not run any lambda. The clone gets its own lazy cells from the same lambdas, as it gets fresh
  instances today.
- **The kontainer still holds its override lambdas,** in `ServiceProviderFactory.dynamics`, because `clone` needs
  them. "After first use only the value is kept" holds for the provider. A collected kontainer leaves nothing
  reachable from the blueprint (proven in ULTRA-00337).
- **Consequences without a compile signal, beyond the ticket's list:**
  - The definition of an override can no longer name the instance's class before it exists. `creates` is the class
    given to `with` (the same as today for `with { X() }`, the base class for `with(Base::class) { Derived() }`).
  - An override that was never requested has no instance. It reports none in `instances`, the dump and debug info,
    and it no longer appears in the insights record slice, which lists only services with instances.
- **Callers whose lambda now runs later:**
  - `with { call.currentUserProvider() }` (funktor-demo, the `funktor:all` test server): it only builds a
    `UserProvider.lazy { }`, so nothing observable moves.
  - `with { UserProvider.system() }`, `with { NullQueryProfiler }` (`AppKontainers.system()`) and
    `with { kronos }` (`WorkersFacade`): values, no side effect.
  - Test fixtures in `funktor/cluster` and `funktor/insights`: listed in the track report.

## Built (batch K1, 2026-11-14)

As planned, with one refinement: once an override has run, its definition names the instance's class and keeps the
code location of `create`, exactly as before. Only an override that was never requested differs: its definition
names the class given to `with`.

**Open point:** "after first use only the value is kept, not the lambda" holds for the provider, and a test pins that.
The kontainer itself still holds its override lambdas for its lifetime, because `clone` re-runs them for the clone's
own instances. A collected kontainer leaves nothing reachable (ULTRA-00337 tests). If the maintainer wants the
kontainer to drop them too, `clone` would have to change meaning.

Pins turned, before -> after:
- `DynamicsAtCreateCharacterizationSpec`, "CURRENT BEHAVIOUR: the override lambda runs at create ..." -> "The
  override lambda runs at the first request of the service, once per kontainer, never at create":
  - runs after create: 1 -> 0
  - after two gets: 1 -> 1
  - after a second `create`: 2 -> 1
- `DynamicsAtCreateCharacterizationSpec`, "Cloning keeps the overrides and runs their lambdas again": runs right
  after `clone()`: 2 -> 0; after a get in each kontainer: 2.
- `ToolsCharacterizationSpec`: one clue text, "created at create" -> "created at its first request".
- `ScopeGuardsSpec`: `forOverride(definition, instance = ...)` -> `forOverride(definition, create = { ... })`, the
  new signature.
- `funktor/insights` `KontainerWireNamesSpec`: the end-to-end wire test now also requests the overridden service.
  Without that, its never-requested override has no instance and is missing from the slice, the consequence named
  above. A new test there pins that consequence.
- New tests:
  - a never-requested override never runs and has no instance
  - a throwing override fails at the first get with its own exception
  - mutable state is read at first use
  - the definition's class before and after first use
  - the provider keeps only the value

Mutation (failed / 524 or 525):

| Mutant | Result |
|---|---|
| KL1: evaluated eagerly at create | RED, 5 |
| KL2: evaluated on every request | RED, 7 |
| KL3: the lambda kept after use | SURVIVED at first; RED, 1 after the provider test was added |

Changelog (behaviour change, no compile signal): `ultra/kontainer`: overrides at `create { with { ... } }` are lazy.
The lambda runs at the first request of that service from that kontainer, on whichever thread asks first, under the
provider's lock; it runs until it has returned a value, then never again. `clone()` no longer runs it at once.
Consequences:
- An override that throws now throws at the first `get`, and again at the next, as no value was kept.
- An override used as a gate, throwing at `create` to reject a request, no longer rejects requests that never ask for
  the service.
- A clone runs its parent's lambdas at the clone's first use, possibly after the parent's request ended and
  concurrently with the parent.
- A lambda that requests its own service ends in `StackOverflowError`.
- Side effects run later, or never when the service is not used.
- A lambda reading something mutable sees the value at first use.
- An override never requested has no instance: none in `instances`, the debug info or the dump, and none in the
  insights record slice.
- Until it has returned a value, its definition names the class given to `with`.
- Public signatures: `ServiceProvider.Provider.forOverride(definition, create: () -> Any)` (was `instance: Any`);
  `ServiceProvider.ForOverride` is no longer a data class and its constructor changed (ULTRA-00333).

Callers checked, nothing observable moves:
- `with { call.currentUserProvider() }` only builds `UserProvider.lazy { }`
- `with { UserProvider.system() }`, `with { NullQueryProfiler }` and `with { kronos }` are values

Commits:
- `9fe4095b`: the plans
- `a4977d16`: today's built-in behaviour pinned
- `dc73ee47`: tests first and the fix, 115 tests red before it and all green after
- `55ff797f`, `cf5358fc`, `d6ed8dd9`: follow-ups

Suites after K1 (XML, `--rerun`, 0 failures):

| Suite | Tests |
|---|---|
| `:ultra:kontainer:test` | 525 (420 before K1) |
| `:ultra:log:jvmTest` | 74 (no log file touched) |
| `:ultra:vault:jvmTest` | 287 |
| `:funktor:insights:jvmTest` | 118 (117 before) |
| `:funktor:all:jvmTest` | 289 |

Repo-wide compile sweep: green, 459 tasks, 0 errors.

## K1 round 1 (strict gate, blind, reviewer-fable-xhigh)

The same ledger is written into ULTRA-00330, 00331, 00332, 00333, 00336 and 00337.

### Verdicts

| Charter | Verdict |
|---|---|
| Implementation | CLEAN on both passes, 6 MINOR |
| Domain and security | CLEAN on both passes, 9 MINOR |

All six decisions are implemented as decided. The redefinition table was written out for all pairs, and only the two
decided pairs differ.

### Findings and outcomes

Every item was tested first; each one that claimed a defect survived its test. Commit `4456c8c0`.

1. **`create` ran the caller's override block before the first-use validation.** Survived: two new tests in
   `ValidationCharacterizationSpec` (block not run on an invalid blueprint; a throwing block does not postpone
   validation) were RED.
   - Fixed: `validateOnFirstUse()` runs before the block in `create`, and again (a no-op by then) in `createChecked`.
   - `clone` needed no change: a kontainer exists only after the first `create`, so validation is always done
     before a clone. On master, `clone` never validated either.
2. **The built-ins were still in the overridable set.** Survived: the internal `instantiate` accepted an override of
   `Kontainer` or `InjectionContext` (new test RED).
   - Fixed: excluded again; `Kontainer` and `InjectionContext` stay dynamic sources of promotion (pinned).
   - ULTRA-00336's plan bullet and "Built" sentence now say what the code does.
3. **Lazy override, unpinned properties:**
   - Concurrency: new test with two threads, B waits BLOCKED while A's lambda runs; one run, one instance.
     `synchronized` removed (C1): RED.
   - Retry after a throw: new test; the first get throws, the second returns, two runs. The mutant that drops the
     lambda before running it (C2): RED.
   - Behaviour kept as built. The texts now say "until it has returned a value, then never again" (KDoc of
     `ForOverride` and of both `with`, this ticket's sections and the changelog).
4. **`extend` was pinned for 6 of 16 pairs.** Survived: the reviewers' mutant (E1) was not caught by the 6.
   - New table test over all 16 base x extension pairs, with literal expectations for the service and for a
     singleton above it (kind, shared or not).
   - E1: RED, 3.
5. **Retention tests kept the blueprint alive only through an unused local.** Fixed with
   `Reference.reachabilityFence(blueprint)` after the assertions. Still green.
6. **Built-in messages:** full literals for the three built-ins in `BuiltInServicesCharacterizationSpec` and
   `DynamicsAtCreateCharacterizationSpec`.
7. **Changelog texts completed:**
   - ULTRA-00330: every pair that changes under `extend`.
   - ULTRA-00333: gate overrides, the thread and lock, clones running the parent's lambdas, self-request, the public
     signatures.
   - ULTRA-00336: what was really fixed by refusing registrations.
8. **Suites:** `:funktor:cluster:jvmTest` added. No test exercises `WorkersFacade.tick`, the one production caller
   of `clone { with { } }`.

Left as built, recorded for the maintainer:
- the kontainer keeps its override lambdas until it is collected (for `clone`)
- `extend` records no `overwrites` (a follow-up)
- the GUIs' "all services" view lists a never-requested override as the blueprint's `Dynamic`
- there is no check for two override keys resolving to one base type; the last wins, as before

### What stayed clean

- All six decisions as decided.
- The redefinition table for all pairs; only the two decided pairs differ.
- The built-in refusal in every registration form and context.
- `create` and `clone` through one checked entry point.
- Lazy overrides: once per kontainer when they succeed, value kept, lambda dropped from the provider.
- The checker key copy.
- No change for anything outside the decisions.

### Mutation runs, round 1 (failed / 546)

| # | Mutant | Result |
|---|---|---|
| V1 | Validation after the override block | RED, 2 |
| B2 | Built-ins back in the overridable set | RED, 1 |
| C1 | `synchronized` removed from the lazy override | RED, 1 |
| C2 | Lambda dropped before it ran (no retry) | RED, 1 |
| E1 | `extend` applies the rule only to singleton extensions | RED, 3 |

Every run backed up and compared with `cmp` the mutated file and the generated docs file; every restore was
identical.

### Counts after round 1 (XML, `--rerun`, 0 failures)

| Suite | Tests |
|---|---|
| `:ultra:kontainer:test` | 546 (525 before the round) |
| `:ultra:log:jvmTest` | 74 |
| `:ultra:vault:jvmTest` | 287 |
| `:funktor:insights:jvmTest` | 118 |
| `:funktor:cluster:jvmTest` | 162 (new in the table) |
| `:funktor:all:jvmTest` | 289 |

Repo-wide compile sweep: green, 459 tasks, 0 errors.

## K1 delta review, round 2 (reviewer-fable-xhigh, all three charters)

The same ledger is written into ULTRA-00330, 00331, 00332, 00333, 00336 and 00337.

**Verdict:** CLEAN, with zero CRITICAL, zero MAJOR and six MINOR. The reviewer recomputed the 16-pair `extend` table and
found every row right; eight pairs differ from master. The reviewer ran nothing. The close-out has no production
behaviour change (texts and tests only).

### Findings and outcomes

1. **KDoc of the overridable set, and ULTRA-00336 "Built":** "The built-ins stay dynamic sources of promotion" is
   false for `KontainerBlueprint`, which is static (pinned in `PromotionCharacterizationSpec`). Fixed: "`Kontainer`
   and `InjectionContext` stay dynamic sources of promotion", in the KDoc and in every ticket sentence.
2. **"First request" wording** in the KDoc of `ForOverride.definition` and `ForOverride.instances`, and in a comment
   in `ServiceProviderFactory`: after a throwing lambda, `instances` is still empty and the definition still names
   the class. Fixed: "until the override has returned a value".
3. **The live-view mutant of ULTRA-00337, re-run with the reachability fences:** RED, 2 / 546 (both retention
   tests). Restored, and `git status` was clean for `DynamicsChecker.kt` afterwards.
4. **`LazyOverrideSpec`:** the same-instance check also held for two nulls. Fixed: a not-null assertion on the first
   result.
5. **Status lines:** the six tickets still said NOT STARTED. Set to built, gate round 1 CLEAN, delta round 2 CLEAN.
6. **ULTRA-00330 changelog:** for `dynamic` then `prototype` or `dynamicPrototype`, two consequences were missing
   (the user of `dynamic` then `prototype` becomes one per kontainer; an override at create is now accepted). Added,
   and the text says plainly that eight pairs change.

### Surplus notes, kept as built

- `validateOnFirstUse()` in `createChecked` guards only the public `ServiceProviderFactory(...)` constructor path;
  every other path validated already.
- `.minus(builtInServices)` also subtracts `KontainerBlueprint`, which is static and never in the set; harmless.

## The work

- Decide: keep eager (simple, an override is a value) and document it, or make it lazy. If it stays, the KDoc of `with` says so.
- Tests first: the "CURRENT BEHAVIOUR" pin turns into the wanted expectation (RED before the fix); mutation check.
- CHANGELOG: a behaviour change without a compile signal.

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's word.
