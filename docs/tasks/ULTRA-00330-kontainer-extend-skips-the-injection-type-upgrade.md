# ULTRA-00330: Kontainer: extend skips the injection type upgrade

> **Status (2026-11-14): BUILT** on `wt/kontainer`, batch K1, not merged: gate round 1 CLEAN, delta round 2 CLEAN.
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

**Found by:** the characterization specs of ULTRA-00319 (Kontainer injection type split), step 1, 2026-11-14.

## Why this exists

In `base.extend { singleton(X) }`, a prototype or dynamic `X` becomes a plain `Singleton`: `extend` does not apply the type upgrade that a redefinition inside one builder applies. The base blueprint's singletons that inject `X` lose their promotion to SemiDynamic, so a service that was per request silently becomes shared across requests.

Pinned as it is today in `ultra/kontainer/src/test/kotlin/e2e/characterization/RedefinitionCharacterizationSpec.kt`, in a test
whose name starts with "CURRENT BEHAVIOUR". The split of ULTRA-00319 keeps this behaviour unchanged; the fix is this
ticket (maintainer, 2026-11-14: "fix each with its own ticket").

## Decided (maintainer, 2026-11-14)

"here we should also prevent the downgrade from dynamic to singleton. The service needs to stay dynamic. We do not
throw or log anything. This is just how the kontainer works."

So `extend` applies the same redefinition rule as a redefinition inside one builder: a service that was dynamic stays
dynamic when an extension redefines it as a singleton, and a prototype stays a prototype. Silently: no exception, no
log line.

## Plan as built (track D, batch K1, 2026-11-14)

- `KontainerBlueprint.extend` applies `RedefinitionUpgrade` (ULTRA-00331) to every extension definition against the
  base definition of the same class, after the extension's own block ran.
  - The rule is associative, so this equals one builder holding base and extension.
  - Silent.
- `overwrites` stays as today (null for an extension's definitions). Recording the base definition there would change
  the debug output; not built.
- Tests first: the two CURRENT BEHAVIOUR pins in `RedefinitionCharacterizationSpec` and the one in
  `DynamicPrototypeSpec` turn into the decided expectation.
- Callers: no production code calls `extend` (searched); only kontainer tests do.

## Built (batch K1, 2026-11-14)

As planned: `KontainerBlueprint.extend` applies `RedefinitionUpgrade` against the base definitions; `overwrites`
unchanged.

Pins turned, before -> after:
- `RedefinitionCharacterizationSpec`, "CURRENT BEHAVIOUR: extend ..., a prototype redefined as singleton becomes
  Singleton": (Singleton, Static) -> stays (Prototype, Static).
- `RedefinitionCharacterizationSpec`, "CURRENT BEHAVIOUR: extend ..., a dynamic redefined as singleton ends the
  promotion": Target (Singleton, Static) -> (Singleton, Dynamic); User (Singleton, Static), shared -> SemiDynamic,
  one per kontainer.
- `DynamicPrototypeSpec`, "Redefinition across extend skips the rule": Target (Singleton, Static) -> (Prototype,
  Dynamic); User global -> SemiDynamic.
- New, for the rule across extend: prototype in the base and dynamic in the extension gives (Prototype, Dynamic); a
  static singleton in the base is replaced as is.

Mutation: KE, `extend` without the rule: RED, 4 / 524.

Changelog (behaviour change, no compile signal): `ultra/kontainer`: `KontainerBlueprint.extend` applies the
redefinition rule against the base blueprint. Silent. Eight of the 16 pairs change, base then extension (all 16 are
pinned in `ExtendRedefinitionCharacterizationSpec`):
- `dynamic` then `singleton`: stays a dynamic singleton, and its users stay one per kontainer (it became a global
  singleton and its users global).
- `dynamic` then `prototype` or `dynamicPrototype`: one instance per kontainer (was one per injection), and an
  override at create is now accepted where it was refused. For `dynamic` then `prototype`, a singleton injecting it
  goes from global to one per kontainer.
- `prototype` then `singleton`: stays a prototype, one instance per injection (it became a global singleton).
- `prototype` then `dynamic`: a dynamic prototype (was a dynamic singleton). One instance per injection, and
  `extended.create { with { X() } }` throws `KontainerInconsistent` where it was accepted.
- `dynamicPrototype` then `singleton`: stays a dynamic prototype, its users stay one per kontainer (it became a global
  singleton).
- `dynamicPrototype` then `prototype`: stays a dynamic prototype, its users stay one per kontainer (it became a static
  prototype and its users global).
- `dynamicPrototype` then `dynamic`: a dynamic prototype (was a dynamic singleton). One instance per injection, and an
  override at create throws `KontainerInconsistent` where it was accepted (ULTRA-00330).

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

- `extend` applies the same redefinition rule as the builder, or refuses a redefinition that lowers the type.
- Tests first: the "CURRENT BEHAVIOUR" pin turns into the wanted expectation (RED before the fix); mutation check.
- CHANGELOG: a behaviour change without a compile signal.

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's word.
