# ULTRA-00331: Kontainer: a prototype redefined as dynamic stays a prototype

> **Status (2026-11-14): NOT STARTED.** 
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

**Found by:** the characterization specs of ULTRA-00319 (Kontainer injection type split), step 1, 2026-11-14.

## Why this exists

A prototype redefined as dynamic stays `Prototype`: singletons that inject it are not promoted, and `create { with { } }` refuses an override for it.

Pinned as it is today in `ultra/kontainer/src/test/kotlin/e2e/characterization/RedefinitionCharacterizationSpec.kt`, in a test
whose name starts with "CURRENT BEHAVIOUR". The split of ULTRA-00319 keeps this behaviour unchanged; the fix is this
ticket (maintainer, 2026-11-14: "fix each with its own ticket").

## Decided (maintainer, 2026-11-14)

The maintainer weighed throwing ("The initial author wants the service to be a prototype so making it a singleton
must fail") against allowing the upgrade to a dynamic singleton, and agreed to the rule below: "ok yes we do it like
this."

**The redefinition rule, two sentences:**

1. The first definition fixes what the service is, singleton or prototype. The one exception stays: a plain static
   singleton can be replaced by anything.
2. Request binding is never lost and can always be added: if either definition is dynamic, the service is dynamic.

| Redefinition | Today | With the rule |
|---|---|---|
| `prototype` then `dynamic` | (Prototype, Static) | (Prototype, Dynamic) |
| `prototype` then `dynamicPrototype` | (Prototype, Static) | (Prototype, Dynamic) |
| every other pair | | unchanged |

Silent: no exception, no log line, as for `extend` (ULTRA-00330). Why no upgrade to a dynamic singleton: a prototype
promises every injector its own instance; services written against the first definition may keep state in it, and one
shared instance per request would mix that state without any failure. Why no exception: consistency with ULTRA-00330,
redefinition is the normal way to swap an implementation, and a safe resolution exists.

Consequence, accepted: `create { with { ... } }` for such a service is still refused (only dynamic singletons can be
overridden; the message says so). `extend` follows the same rule (ULTRA-00330).

## Plan as built (track D, batch K1, 2026-11-14)

- `KontainerBuilder.RedefinitionUpgrade.adjust(newDef, existingDef)`:
  - no earlier definition, or the earlier one is a static singleton: the later definition as it is
  - otherwise: type = the earlier type; kind = Dynamic if either is Dynamic, else Static
  - the producer is always the later one, as today
- The rule is associative, so applying it per redefinition (builder) or to a whole extension (`extend`, ULTRA-00330)
  gives the same result for the same sequence of definitions.
- Only `prototype` then `dynamic` and `prototype` then `dynamicPrototype` change, both to (Prototype, Dynamic); the
  other pairs are pinned unchanged.
- Silent; an override at create is refused for such a service, with the existing message and its dynamic prototype
  hint.
- Tests first:
  - the CURRENT BEHAVIOUR pins (`RedefinitionCharacterizationSpec`, `DynamicPrototypeSpec`) turn into the decided
    expectation
  - `InjectionTypeUpgradeSpec` "A Prototype service must not be downgraded to a dynamic" changes with the decision
- Callers: no production code redefines a prototype as dynamic (searched).

## Built (batch K1, 2026-11-14)

As planned; the rule is in `KontainerBuilder.RedefinitionUpgrade`.

Pins turned, before -> after:
- `RedefinitionCharacterizationSpec`, "CURRENT BEHAVIOUR: prototype redefined as dynamic stays Prototype ..." ->
  "Prototype redefined as dynamic: Prototype.Dynamic, its users are promoted, an override at create is refused":
  - Target: (Prototype, Static) -> (Prototype, Dynamic)
  - User: (Singleton, Static), shared -> (Singleton, SemiDynamic), one per kontainer
  - Refusal message: `Unexpected dynamics were provided: Target` -> the same plus `(only dynamic singletons can be
    overridden, not the dynamic prototype Target)`
- `RedefinitionCharacterizationSpec`, "singleton, prototype, dynamic ends as Prototype": (Prototype, Static) ->
  (Prototype, Dynamic).
- `DynamicPrototypeSpec`, "CURRENT BEHAVIOUR: prototype then dynamicPrototype stays Prototype.Static ...": Target
  (Prototype, Static) -> (Prototype, Dynamic); User global -> SemiDynamic, one per kontainer.
- `InjectionTypeUpgradeSpec`, "A Prototype service must not be downgraded to a dynamic ..." -> "... stays a Prototype
  and becomes Dynamic": (Prototype, Static) -> (Prototype, Dynamic).

Mutation (failed / 524):

| Mutant | Result |
|---|---|
| KR1: the static-singleton arm off | RED, 6 |
| KR2: the type from the later definition | RED, 14 |
| KR3: the later kind ignored (the old rule) | RED, 5 |
| KR4: the earlier kind ignored | RED, 10 |
| KR5: always dynamic | RED, 3 |

Changelog (behaviour change, no compile signal): `ultra/kontainer`: the redefinition rule. The first definition
fixes whether a service is a singleton or a prototype; a plain static singleton can still be replaced by anything. If
either definition is dynamic, the service is dynamic. `prototype(X)` then `dynamic(X)` or `dynamicPrototype(X)` now
gives a dynamic prototype (was a static prototype), so singletons injecting `X` become one per kontainer. An override
of `X` at create is still refused; the message now names it as a dynamic prototype (ULTRA-00331).

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
   - Fixed: excluded again; they stay dynamic sources of promotion (pinned).
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

## The work

- Decide the redefinition table for all pairs of (ServiceType, ServiceKind), `dynamicPrototype` included, and make builder and `extend` follow it (see ULTRA-00330).
- Tests first: the "CURRENT BEHAVIOUR" pin turns into the wanted expectation (RED before the fix); mutation check.
- CHANGELOG: a behaviour change without a compile signal.

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's word.
