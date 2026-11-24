# ULTRA-00351: Kontainer: faster kontainer creation and service provision, measured, no behaviour change

> **Status (2026-11-24): DESIGNED.** Starts when the other kontainer tracks are done (maintainer). Benchmark first.

## Why this exists

funktor creates a kontainer on every HTTP call (`funktor/core/src/jvmMain/kotlin/ktor_call.kt:27-36`; the demo's API
calls hand in one override, `UserProvider`; `AppKontainers.system()` two; workers `clone` once per tick). The
maintainer: building a blueprint need not be efficient; **creating a kontainer and providing services during its
life must be as efficient as possible**. A read-only audit (2026-11-24, on the code after ULTRA-00329) found the costs
below and no real data race.

The maintainer's conditions (2026-11-24):
- **No behaviour change, internal changes only.** Public API, results, exceptions and messages stay as they are.
- **Everything a change touches is covered by tests before the change is made**; missing coverage is added first.
- **Measured**: a benchmark before and after each change.

## Findings (audit, line numbers on master after cc71f47d)

| # | Finding | Where | Expected gain |
|---|---|---|---|
| 1 | Every `create` copies the whole provider map (`Map.plus` builds `LinkedHashMap(this)`), also with no overrides | `ServiceProviderFactory.kt:16-35` | about N x 40 B plus a table per request (N about 250 in a funktor app); create becomes O(overrides). Keep the blueprint map shared, overrides in a small map looked up first; `getAllProviders` keeps its order |
| 2 | Every override captures a full stack trace: `ServiceDefinition`'s default `codeLocation = CodeLocation.create()` is `Throwable()`, inside the Ktor call stack; the stored location is only `ServiceProviderFactory.<init>` | `ServiceDefinition.kt:21`, `ServiceProviderFactory.kt:25` | one native stack walk per override per request, and the backtrace kept with the kontainer |
| 3 | Already-resolved keys are resolved again: `getOrNull`, `getAll` and the `LazyServiceLookup` map call `get(type)`, which runs `superTypeLookup.getDistinctFor` once more | `Kontainer.kt:119-141`, `TypeLookup.kt:96-98` | 3 lookups become 2 per injection; one less per `getAll` element |
| 4 | `getProvider` allocates a capturing lambda for `computeIfAbsent` on every call | `ServiceProviderFactory.kt:49` | one allocation less per get |
| 5 | A blueprint-level resolution cache (requested class to its distinct key, or null), so a hot get needs no `first()` iterator | `TypeLookup.kt:21-36, 82-87` | fewer allocations per get |
| 6 | No-arg classes are built with `createInstance()` on every instance (constructor scan, `callBy`) | `ServiceProducer.kt:61-63` | cheaper creation of dynamic singletons per request and of prototypes |
| 7 | Creator arguments built through `params.map{}.toTypedArray()`, then copied again by the spread in `ctor.call(*params)` | `ServiceProvider.kt` (`create` of ForSingleton and ForPrototype), `ServiceProducer.kt:65-68` | one ArrayList and one array less per creation |
| 8 | (measure first) call the Java constructor directly instead of kotlin-reflect `call` | `ServiceProducer.kt:65-68` | unknown; value-class parameters need kotlin-reflect's unboxing |
| 9 | The override checks of `createChecked` (built-ins, dynamics check, `overriddenServiceOf`) look each override up 3 to 4 times, and `overriddenServiceOf` again in the factory | `KontainerBlueprint.kt:225-259`, `ServiceProviderFactory.kt:19` | 1 lookup per override; messages unchanged |
| 10 | `Kontainer.tools` built eagerly; the providers map not presized | `Kontainer.kt:19`, `ServiceProviderFactory.kt:37` | micro |

**Thread safety (no fix required):** blueprint fields are immutable; `validationError`, `globalKontainerLazy` and
`semiDynamicSources` are synchronized lazies; the TypeLookup, DynamicsChecker and `InjectionContext.nextCache` caches
are ConcurrentHashMaps whose only race is duplicate deterministic work; the provider factory uses `computeIfAbsent`
with a non-reentrant mapping; `ForSingleton` is correct double-checked locking; the prototype counter is a CAS on an
immutable value. Hardening only: `DynamicOverrides.Builder.build()` wraps the builder's live map (a leaked builder
could change a kontainer's overrides later): copy it. Pre-existing edge, to document, not to change: a singleton whose
constructor forces a `Lazy` back into a singleton still being created can deadlock across two threads, or on one thread
re-enter its monitor.

## Decide before implementing

Each of these would change something observable; under "no behaviour change" they are out unless the maintainer
says otherwise:
- **Finding 2** changes the code location that debug info and the dump show for an overridden service (today the
  useless `ServiceProviderFactory.<init>`). Options: keep the stack capture (no gain), reuse the overwritten
  definition's location, or a constant. Ask.
- **Finding 3, suspected latent bug:** inside `getAll(Base)`, the re-resolution `get(it)` of a candidate that is itself
  a supertype of another registered service would throw `ServiceAmbiguous`. Skipping the re-resolution would stop that
  throw, which is a behaviour change. Write the test first: if it throws today, finding 3 is applied only where it
  changes nothing, and the bug goes to the maintainer as its own question.
- **Finding 5** must not change `isProviderCreated` or the per-kontainer providers map (ServiceProviderFactorySpec
  pins them): no shortcut that skips the per-kontainer map for static singletons.
- **Finding 6:** `createInstance` throws for a class with several all-optional constructors; the replacement must
  throw the same way, or keep `createInstance` for those classes.
- **Finding 8** only if the benchmark shows a gain worth it, and only where no value-class parameter is involved.

## The work

1. **Benchmark first.** JMH in `ultra/kontainer` (a `jmh` source set, plugin version looked up online with webq),
   run through `console/wt-gradle.sh :ultra:kontainer:jmh` with fixed `-wi 5 -i 10 -f 2` and `-prof gc`; the key
   numbers are time per op and `gc.alloc.rate.norm` (bytes per op).
   - Fixture: a synthetic blueprint shaped like a funktor app (about 250 definitions: 150 static singletons, 60
     dynamic, about 40 semi-dynamic through chains, 5 prototypes no-arg and with args, class and factory producers,
     one interface with 5 implementations, one overridable `UserProvider`-like dynamic). If the real funktor-demo
     blueprint builds without infrastructure, a second fixture with it.
   - Benchmarks: `create()`, `create` with 1 and 2 overrides, `clone` with 1 override, a recorded request (create plus
     the services a real API call touches), hot `get` of a static and of a dynamic singleton, `getOrNull` of an
     interface, `getAll` of 5, prototype creation (no-arg, 3 args, factory), `LazyServiceLookup.get`; create, request
     and static get also at 8 threads.
   - Baseline recorded in this ticket before any change.
2. **Coverage first, per finding:** list the tests that pin the behaviour the change touches (create with and without
   overrides, `clone`, `getAllProviders` order and content, `isProviderCreated`, debug info and dump, every injection
   form, `getAll` order, error messages of refused overrides, `createInstance` edge cases). Add what is missing and
   mutation-check the new tests against the CURRENT code before changing it.
3. **One finding per commit**, in the order 1, 4, 7, 3, 9, 6, 5, 10, then 8 if measured worth it; the hardening copy in
   `DynamicOverrides`; the KDoc note on the `ForSingleton` lazy edge. Each with its benchmark delta in this ticket.
4. Suites: kontainer, and the consumer suites ULTRA-00329 used (log, vault, insights, cluster, all, core, rest, auth,
   messaging, logging, codegen, saas, demo server) plus the compile sweep.
5. `/feature-review` gate (standard; correctness-critical, a concurrency charter question for every reviewer).
6. CHANGELOG: an internal entry under Unreleased with the measured gains; no breaking change.

## Acceptance

- Baseline and after numbers for every benchmark recorded here; no benchmark regresses beyond noise.
- No public API change; all existing kontainer and consumer suites green unchanged (tests added, none weakened).
- Every changed path was covered by a test that went RED on a mutant of the current code before the change.

## History

- 2026-11-24: created on the maintainer's word from the read-only performance and thread-safety audit, with the
  conditions above ("This should not lead to any behaviour changes, only internal changes"). Start after the other
  kontainer tracks.
