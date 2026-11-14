# ULTRA-00336: Kontainer: the built-in Kontainer service can be overridden at create

> **Status (2026-11-14): BUILT** on `wt/kontainer`, batch K1, not merged: gate round 1 CLEAN, delta round 2 CLEAN.
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

## Why this exists

The built-in `Kontainer` service is declared dynamic (`KontainerBuilder.kt`), so `create { with<Kontainer> { other } }` is accepted: `get(Kontainer::class)` and every service that injects `Kontainer` then get the foreign kontainer, and with it another request's services. The same holds for `InjectionContext`. Found by reading (security reviewer, round 1 of ULTRA-00319); the same on master; not yet pinned by a test.

## Decided (maintainer, 2026-11-14)

"Yes refuse the override for the built-in services. All three i suppose."

`create` and `clone` refuse an override of `Kontainer`, `InjectionContext` and `KontainerBlueprint` with
`KontainerInconsistent`, naming the class and saying that it is managed by the kontainer itself. `KontainerBlueprint`
is a static singleton and is already refused today as "unexpected"; it gets the same clear message as the other two.

**And they cannot be registered at all** (maintainer, 2026-11-14: "This also means it must not be possible to
register any of these at all."): a blueprint builder, a module or an `extend` block that defines `Kontainer`,
`InjectionContext` or `KontainerBlueprint` (`singleton`, `prototype`, `dynamic`, `dynamicPrototype`, `instance`, by
class or with a factory) is refused with `KontainerInconsistent`, naming the class and saying that it is managed by
the kontainer itself. Test first what happens today (the built-ins are added after the builder block ran,
`KontainerBuilder.kt:527-546`, so a user definition is probably replaced silently): pin it, then refuse it. Decide in
the plan whether the refusal comes when the definition is made or when the blueprint is built; either way before any
kontainer exists.

## Plan as built (track D, batch K1, 2026-11-14)

- **Registration: refused at the defining call**, so the stack trace points at the line.
  - Covered: the `singleton`, `prototype`, `dynamic` and `dynamicPrototype` service builders (every overload goes
    through them) and both `instance()` forms, inside `kontainer { }`, a module and `extend { }`.
  - `KontainerInconsistent("<class> is managed by the kontainer itself and cannot be registered")`.
  - The kontainer's own registration of the three bypasses the check.
  - Today's behaviour is pinned first, in every form, as CURRENT BEHAVIOUR.
- **Override: refused at `create` and `clone`**, through the shared entry point of ULTRA-00332, before the dynamics
  check.
  - `KontainerInconsistent("<class> is managed by the kontainer itself and cannot be overridden")`.
  - `Kontainer` and `InjectionContext` leave the set of overridable dynamics; `Kontainer` and `InjectionContext` stay dynamic sources of promotion.
- Matching is by class equality. All three classes are final, so no subclass can be registered under them.
- Callers: no code registers or overrides a built-in (searched).

## Built (batch K1, 2026-11-14)

As planned. Registration is refused at the defining call: a statement after the registration in the same block does
not run, which a test pins.

Today's behaviour, pinned first and confirmed by a run (commit `a4977d16`, 95 tests): every registration form of the
three was accepted silently and the built-in still provided its value. The form could change what it reported:
- a `prototype` registration made the built-in (Prototype, Static)
- a `dynamicPrototype` registration made it (Prototype, Dynamic)
- a `dynamic` registration made `KontainerBlueprint` (Singleton, Dynamic)

Overrides of `Kontainer` and `InjectionContext` were accepted at `create` and `clone`; `KontainerBlueprint` was
refused as "Unexpected dynamics were provided" at `create` and threw `ServiceNotFound` at `clone`.

Pins turned, before -> after:
- `BuiltInServicesCharacterizationSpec`:
  - 90 registration pins ("accepted, the built-in wins") -> refused with
    `KontainerInconsistent("<class> is managed by the kontainer itself and cannot be registered")`
  - 5 override pins -> refused with `KontainerInconsistent("<class> is managed by the kontainer itself and cannot be
    overridden")`
  - plus one test with a built-in and an unexpected class together: the built-in is named first.
- `DynamicsAtCreateCharacterizationSpec`, "CURRENT BEHAVIOUR: an override for the Kontainer itself is accepted ..." ->
  refused, same message.

Mutation (failed / 524):

| Mutant | Result |
|---|---|
| KB1 to KB4: refusal removed from `singleton` / `prototype` / `dynamic` / `dynamicPrototype` | RED, 18 each |
| KB5 and KB6: refusal removed from `instance(value)` / `instance(class, value)` | RED, 9 each |
| KB7: override refusal removed | RED, 7 |
| KB8, KB9, KB10: `Kontainer` / `InjectionContext` / `KontainerBlueprint` taken out of the built-in set | RED, 32 / 33 / 32 |

The built-ins are also excluded from the overridable set (removed while building as redundant, restored in round 1):
not even the internal `instantiate` accepts an override of them. `Kontainer` and `InjectionContext` stay dynamic
sources of promotion.

Changelog (behaviour change):
- `ultra/kontainer`: `Kontainer`, `InjectionContext` and `KontainerBlueprint` cannot be registered. Every form of
  `singleton`, `prototype`, `dynamic`, `dynamicPrototype` and `instance` is refused, in `kontainer { }`, a module and
  `extend`: `KontainerInconsistent("<class> is managed by the kontainer itself and cannot be registered")`, thrown at
  the defining call. Before, the registration was accepted silently and the built-in still provided its value, though
  the declared form could change its reported type and kind.
- `ultra/kontainer`: they cannot be overridden at `create` or `clone`:
  `KontainerInconsistent("<class> is managed by the kontainer itself and cannot be overridden")`. Before, overrides of
  `Kontainer` and `InjectionContext` were accepted, and services injecting `Kontainer` got the foreign one.
  `KontainerBlueprint` was refused as "Unexpected dynamics were provided" at `create` and with `ServiceNotFound` at
  `clone`.
- What was really fixed by refusing registrations: on master `prototype(Kontainer::class)` made `Kontainer`
  (Prototype, Static), so a singleton injecting only `Kontainer` was a global singleton and held the first request's
  kontainer (ULTRA-00336).

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

- First pin today's behaviour in `ultra/kontainer/src/test/kotlin/e2e/characterization/` as "CURRENT BEHAVIOUR" (track D
  does this with the round-1 fixes of ULTRA-00319), so the scenario is confirmed by a run.
- Refuse an override of the built-in services at `create` and `clone` with `KontainerInconsistent`.
- Tests first, mutation check, CHANGELOG (behaviour change without a compile signal).

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's standing word for Kontainer oddities ("fix each with its own ticket").
