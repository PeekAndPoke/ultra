# ULTRA-00351: Kontainer: faster kontainer creation and service provision, measured, no behaviour change

> **Status (2026-11-24): DONE.** Merged into master (31711410); four follow-up questions are with the maintainer (History).

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

## Measurements

Harness: `ultra/kontainer/src/jmh` (JMH 1.37, Gradle plugin `me.champeau.jmh` 0.7.4, both checked online
2026-11-24), run with `console/wt-gradle.sh :ultra:kontainer:jmh`, one benchmark set with `-PjmhIncludes=<regex>`.
Fixture `funktorShapedBlueprint()` (`fixture.kt`): 254 definitions plus the 3 built-ins: 149 static singletons
(class and factory producers, 5 implementations of `Handler`, `Clock`, a `Lookup<Handler>`, a `List<Handler>`, a
`Lazy` holder), 60 dynamic singletons (`UserProvider` and `RequestInfo` overridable), 40 semi dynamic singletons
through chains, 5 prototypes (no-arg class, 3-arg class, factory with args, factory without, 1-arg class).
`request` = create with the `UserProvider` override, then 10 gets (2 deep semi dynamics, a static, a dynamic, a
3-arg prototype, `getAll` of 5, `getOrNull` of an interface, the Lookup holder, a `Lazy` value).

**Deviation from the work order:** iterations are 1 s, not JMH's default 10 s: `-wi 5 -w 1s -i 10 -r 1s -f 2
-prof gc` (set in `build.gradle.kts`). With 10 s iterations one run takes about 85 minutes; with 1 s it takes 8.5
minutes and the errors stay within a few percent. The same settings before and after.

**No funktor-demo fixture:** its blueprint needs a `FunktorDemoConfig` and its services reach Arango and Mongo, and
a `jmh` dependency of `ultra/kontainer` on `funktor-demo:server` would invert the module layering.

### Baseline (master 912f035c, 2026-11-24, JDK 17.0.17, 16 cores, load average 5 to 10 from other builds)

| Benchmark | ns/op | B/op |
|---|---:|---:|
| create | 3,357.8 +- 25.7 | 12,704 |
| createOneOverride | 4,572.3 +- 133.6 | 13,936 |
| createTwoOverrides | 5,395.4 +- 49.4 | 15,032 |
| cloneOneOverride | 4,796.0 +- 290.8 | 14,136 |
| request | 8,432.6 +- 118.5 | 20,264 |
| getStaticHot | 28.8 +- 1.2 | 40 |
| getDynamicHot | 28.2 +- 0.2 | 40 |
| getOrNullInterface | 44.9 +- 0.4 | 40 |
| getAll5 | 164.3 +- 6.8 | 264 |
| prototypeNoArg | 81.6 +- 0.7 | 144 |
| prototype3Args | 227.4 +- 1.7 | 384 |
| prototypeFactory | 165.2 +- 1.4 | 264 |
| lazyLookupGet | 40.4 +- 0.2 | 40 |
| createT8 (8 threads) | 4,558.4 +- 121.8 | 12,704 |
| requestT8 (8 threads) | 11,128.4 +- 827.1 | 19,984 |
| getStaticHotT8 (8 threads) | 31.8 +- 0.4 | 40 |

Reading: `create` is 12.7 KB, nearly all of it the copied provider map (finding 1). Each override adds about
1.2 KB and 0.8 to 1.2 us; the stack capture of finding 2 is part of that (not touched, waits for the maintainer).

### Final full run (wt/kontainer after 96a4f863, 2026-11-24, same settings, load average 3 to 4)

Applied: findings 1, 4, 7, 3 (getOrNull only), 6, 5, 10 (presize only). Not applied: 2, the getAll / List / Lookup
part of 3 and the `DynamicOverrides.Builder` copy (maintainer questions), 8 (not asked for), 9 (measured as a
regression). No benchmark regresses.

| Benchmark | ns/op before | ns/op after | delta | B/op before | B/op after |
|---|---:|---:|---:|---:|---:|
| create | 3,357.8 +- 25.7 | 15.3 +- 0.1 | -100% | 12,704 | 264 |
| createOneOverride | 4,572.3 +- 133.6 | 950.5 +- 8.4 | -79% | 13,936 | 1,664 |
| createTwoOverrides | 5,395.4 +- 49.4 | 1,953.2 +- 18.3 | -64% | 15,032 | 2,752 |
| cloneOneOverride | 4,796.0 +- 290.8 | 1,028.2 +- 14.9 | -79% | 14,136 | 1,864 |
| request | 8,432.6 +- 118.5 | 3,592.6 +- 32.4 | -57% | 20,264 | 5,520 |
| getStaticHot | 28.8 +- 1.2 | 13.6 +- 0.1 | -53% | 40 | 0 |
| getDynamicHot | 28.2 +- 0.2 | 13.8 +- 0.6 | -51% | 40 | 0 |
| getOrNullInterface | 44.9 +- 0.4 | 14.9 +- 0.3 | -67% | 40 | 0 |
| getAll5 | 164.3 +- 6.8 | 94.7 +- 1.3 | -42% | 264 | 64 |
| prototypeNoArg | 81.6 +- 0.7 | 61.3 +- 1.2 | -25% | 144 | 104 |
| prototype3Args | 227.4 +- 1.7 | 109.5 +- 4.0 | -52% | 384 | 176 |
| prototypeFactory | 165.2 +- 1.4 | 82.1 +- 3.4 | -50% | 264 | 104 |
| lazyLookupGet | 40.4 +- 0.2 | 26.8 +- 0.2 | -34% | 40 | 0 |
| createT8 | 4,558.4 +- 121.8 | 74.4 +- 0.5 | -98% | 12,704 | 264 |
| requestT8 | 11,128.4 +- 827.1 | 4,330.3 +- 58.1 | -61% | 19,984 | 5,328 |
| getStaticHotT8 | 31.8 +- 0.4 | 16.2 +- 0.2 | -49% | 40 | 0 |

### After the gate batch (c893a64d..761dbbdf: finding 3 reverted, finding 6 dropped)

Kontainer suite 618 / 0 / 0 (XML). Production changes only in `Kontainer.kt`, `ServiceProducer.kt` (both equal
master again) and a KDoc line in `TypeLookup.kt`, so no consumer suites. Affected benchmarks, against the final full
run above (the reverted findings' gains go back, as expected):

| Benchmark | ns/op before | ns/op after | delta | B/op before | B/op after |
|---|---:|---:|---:|---:|---:|
| request | 3,592.6 +- 32.4 | 3,852.1 +- 29.4 | +7% | 5,520 | 5,496 |
| getStaticHot | 13.6 +- 0.1 | 14.4 +- 0.1 | +6% | 0 | 0 |
| getOrNullInterface | 14.9 +- 0.3 | 21.5 +- 0.3 | +45% | 0 | 0 |
| getAll5 | 94.7 +- 1.3 | 95.7 +- 1.7 | +1% | 64 | 64 |
| prototypeNoArg | 61.3 +- 1.2 | 68.3 +- 1.5 | +11% | 104 | 104 |
| prototype3Args | 109.5 +- 4.0 | 136.0 +- 2.0 | +24% | 176 | 176 |
| prototypeFactory | 82.1 +- 3.4 | 99.9 +- 2.3 | +22% | 104 | 104 |
| lazyLookupGet | 26.8 +- 0.2 | 24.0 +- 3.4 | -10% | 0 | 0 |
| requestT8 | 4,330.3 +- 58.1 | 4,473.2 +- 40.1 | +3% | 5,328 | 5,496 |

The same run against the baseline (what ships):

| Benchmark | ns/op before | ns/op after | delta | B/op before | B/op after |
|---|---:|---:|---:|---:|---:|
| request | 8,432.6 +- 118.5 | 3,852.1 +- 29.4 | -54% | 20,264 | 5,496 |
| getStaticHot | 28.8 +- 1.2 | 14.4 +- 0.1 | -50% | 40 | 0 |
| getOrNullInterface | 44.9 +- 0.4 | 21.5 +- 0.3 | -52% | 40 | 0 |
| getAll5 | 164.3 +- 6.8 | 95.7 +- 1.7 | -42% | 264 | 64 |
| prototypeNoArg | 81.6 +- 0.7 | 68.3 +- 1.5 | -16% | 144 | 104 |
| prototype3Args | 227.4 +- 1.7 | 136.0 +- 2.0 | -40% | 384 | 176 |
| prototypeFactory | 165.2 +- 1.4 | 99.9 +- 2.3 | -40% | 264 | 104 |
| lazyLookupGet | 40.4 +- 0.2 | 24.0 +- 3.4 | -40% | 40 | 0 |
| requestT8 | 11,128.4 +- 827.1 | 4,473.2 +- 40.1 | -60% | 19,984 | 5,496 |

### Suites and compile sweep (after 96a4f863, counts from the XML, all written by this run)

| Suite | Tests | Failures | Errors |
|---|---:|---:|---:|
| `:ultra:kontainer:test` | 616 | 0 | 0 |
| `:ultra:log:jvmTest` | 74 | 0 | 0 |
| `:ultra:vault:jvmTest` | 287 | 0 | 0 |
| `:funktor:insights:jvmTest` | 119 | 0 | 0 |
| `:funktor:cluster:jvmTest` | 162 | 0 | 0 |
| `:funktor:all:jvmTest` | 289 | 0 | 0 |
| `:funktor:core:jvmTest` | 775 (2 skipped) | 0 | 0 |
| `:funktor:rest:jvmTest` | 124 | 0 | 0 |
| `:funktor:auth:jvmTest` | 322 | 0 | 0 |
| `:funktor:messaging:jvmTest` | 67 | 0 | 0 |
| `:funktor:logging:jvmTest` | 14 | 0 | 0 |
| `:funktor:codegen:test` | 104 | 0 | 0 |
| `:funktor:saas:jvmTest` | 50 | 0 | 0 |
| `:funktor-demo:server:test` | 52 | 0 | 0 |

Compile sweep (`compileKotlinJvm compileTestKotlinJvm compileKotlinJs compileTestKotlinJs compileKotlin
compileTestKotlin compileJmhKotlin --continue`): BUILD SUCCESSFUL, 0 lines `e:`. No public signature changed, so no
stale-test-class risk from an ABI change.

Trap seen on the way: `generateDocs` runs before the kontainer tests and writes into the tracked
`ultra/kontainer/common/docs/`; when a mutant broke an example it deleted `ultra::docs/index.md`. The next green run
wrote it back unchanged. Mutation runs since then use `-x generateDocs`.

### Per finding (subset runs, same settings; each says what its "before" is, the baseline unless stated)

**Finding 1** (blueprint map shared, overrides in their own map looked up first; getAllProviders keeps the order).
Tests: the 616 of the kontainer suite green. Mutants of the new code: getProvider ignoring the override map, 40 RED;
getAllProviders ignoring it, 5 RED.

| Benchmark | ns/op before | ns/op after | delta | B/op before | B/op after |
|---|---:|---:|---:|---:|---:|
| create | 3,357.8 +- 25.7 | 13.7 +- 0.1 | -100% | 12,704 | 232 |
| createOneOverride | 4,572.3 +- 133.6 | 968.0 +- 7.7 | -79% | 13,936 | 1,632 |
| createTwoOverrides | 5,395.4 +- 49.4 | 1,941.7 +- 14.1 | -64% | 15,032 | 2,720 |
| cloneOneOverride | 4,796.0 +- 290.8 | 1,030.3 +- 7.2 | -79% | 14,136 | 1,832 |
| request | 8,432.6 +- 118.5 | 5,316.1 +- 77.6 | -37% | 20,264 | 7,852 |
| createT8 | 4,558.4 +- 121.8 | 65.9 +- 0.5 | -99% | 12,704 | 232 |
| requestT8 | 11,128.4 +- 827.1 | 6,335.6 +- 54.1 | -43% | 19,984 | 7,680 |

**Finding 4** (`providers[cls]` first, `computeIfAbsent` with one stored function). Tests: 616 green. "before" for
the gets is the baseline (finding 1 does not touch them); `request` after finding 1 was 5,316 ns and 7,852 B.

| Benchmark | ns/op before | ns/op after | delta | B/op before | B/op after |
|---|---:|---:|---:|---:|---:|
| request | 8,432.6 +- 118.5 | 5,178.7 +- 45.9 | -39% | 20,264 | 6,456 |
| getStaticHot | 28.8 +- 1.2 | 25.5 +- 0.7 | -11% | 40 | 0 |
| getDynamicHot | 28.2 +- 0.2 | 23.8 +- 0.2 | -16% | 40 | 0 |
| getOrNullInterface | 44.9 +- 0.4 | 42.2 +- 1.2 | -6% | 40 | 0 |
| getAll5 | 164.3 +- 6.8 | 137.0 +- 1.7 | -17% | 264 | 64 |
| prototypeNoArg | 81.6 +- 0.7 | 75.4 +- 0.6 | -8% | 144 | 104 |
| prototype3Args | 227.4 +- 1.7 | 208.1 +- 1.4 | -8% | 384 | 224 |
| prototypeFactory | 165.2 +- 1.4 | 150.4 +- 1.3 | -9% | 264 | 144 |
| lazyLookupGet | 40.4 +- 0.2 | 33.2 +- 0.3 | -18% | 40 | 0 |
| getStaticHotT8 | 31.8 +- 0.4 | 26.6 +- 0.4 | -16% | 40 | 0 |

Finding 4's price, found later: the stored function is one object per kontainer, `create` 232 B to 264 B and
13.7 ns to 14.8 ns. Measured alternative (not applied): a plain read and a capturing `computeIfAbsent` lambda only on
a miss keeps `create` at 232 B, but costs one lambda per provider created, `request` 5,840 B to 7,048 B and
`requestT8` +10% in time. One object per kontainer is the better trade.

**Finding 7** (the argument array built directly, no intermediate list). The copy made by the spread in
`ctor.call(*params)` stays: Kotlin emits `Arrays.copyOf` there and only a Java helper could avoid it. Tests: 616
green; mutant of the new code (prototype arguments reversed): 23 RED. "before" is after finding 4.

| Benchmark | ns/op before | ns/op after | delta | B/op before | B/op after |
|---|---:|---:|---:|---:|---:|
| request | 5,178.7 +- 45.9 | 4,919.9 +- 43.2 | -5% | 6,456 | 5,832 |
| prototypeNoArg | 75.4 +- 0.6 | 75.2 +- 0.4 | 0% | 104 | 104 |
| prototype3Args | 208.1 +- 1.4 | 202.0 +- 3.3 | -3% | 224 | 176 |
| prototypeFactory | 150.4 +- 1.3 | 142.3 +- 1.7 | -5% | 144 | 104 |

**Finding 3: applied, then reverted at the gate (round 1, see the ledger): not behaviour-neutral for aliased keys.**
As first applied (`getOrNull` only: the provider of the one candidate directly; getAll, List and Lookup unchanged, their
pins stay). Behaviour-neutral by construction: when `service` has exactly one candidate `T`, every candidate of `T` is
a candidate of `service`, so `T`'s candidates are exactly `{T}` and the second resolution can only return `T`.
That argument is wrong for aliased keys: `Int::class` (Java `int`) and `java.lang.Integer` are equal `KClass` keys
of the candidate cache with different Java assignability, and the second resolution read and filled that shared
entry. Pinned by two tests in `ProvisionPathsCharacterizationSpec` (RED on the shortcut, green on 912f035c and
after the revert).
Tests: 616 green; mutant of the new code (provider of the requested class): 4 RED, and generateDocs fails on the
examples. "before" is after finding 7.

| Benchmark | ns/op before | ns/op after | delta | B/op before | B/op after |
|---|---:|---:|---:|---:|---:|
| request | 4,919.9 +- 43.2 | 4,515.5 +- 37.9 | -8% | 5,832 | 5,728 |
| prototype3Args | 202.0 +- 3.3 | 154.2 +- 1.4 | -24% | 176 | 176 |
| getOrNullInterface (before: after finding 4) | 42.2 +- 1.2 | 26.8 +- 0.4 | -36% | 0 | 0 |

**Finding 9: measured, not worth it, not applied.** Implemented as "createChecked resolves each override once into
a map and hands it to the factory" (616 tests green). The extra map costs more than the hash lookups it saves (an
exact override costs one `HashSet` lookup in `overriddenServiceOf`). Correction: the first version of this table
compared against the numbers after finding 1, which miss the 32 B per kontainer finding 4 added; "before" below is
a later run of the code without finding 9 (after finding 5, which does not touch creation):

| Benchmark | ns/op before | ns/op with finding 9 | delta | B/op before | B/op with finding 9 |
|---|---:|---:|---:|---:|---:|
| create | 14.8 +- 0.1 | 16.3 +- 0.1 | +10% | 264 | 264 |
| createOneOverride | 964.2 +- 11.4 | 1,018.3 +- 10.1 | +6% | 1,664 | 1,792 |
| createTwoOverrides | 1,938.8 +- 16.7 | 2,004.5 +- 15.9 | +3% | 2,752 | 2,936 |
| cloneOneOverride | 1,029.4 +- 7.2 | 1,053.1 +- 8.6 | +2% | 1,864 | 1,984 |
| createT8 | 75.3 +- 0.5 | 74.4 +- 0.5 | -1% | 264 | 264 |

Reverted; the code stays as after finding 3.

**Finding 6: applied, then dropped at the gate (round 1): about 5 ns per no-arg instance for a copy of kotlin-reflect's
`createInstance` internals that a later kotlin-reflect could diverge from. Its tests stay.** As first applied (the constructor `createInstance` would pick, looked up once and lazily, then `callBy(emptyMap())`
as `createInstance` does; checked against the kotlin-reflect 2.4.21 bytecode of `createInstance`. Without exactly
one such constructor `createInstance` still runs on every instance and throws as before). Tests: 616 green; mutants
of the new code: `firstOrNull` for `singleOrNull`, 1 RED (two all-optional constructors); a Java constructor made
accessible, 1 RED (private constructor). A small gain: the time goes to kotlin-reflect's call itself. "before" is
after finding 3 (request) and after finding 7 (prototypeNoArg); the request's B/op moves within its run-to-run
spread of about 250 B.

| Benchmark | ns/op before | ns/op after | delta | B/op before | B/op after |
|---|---:|---:|---:|---:|---:|
| request | 4,515.5 +- 37.9 | 4,495.1 +- 39.1 | 0% | 5,728 | 5,840 |
| prototypeNoArg | 75.2 +- 0.4 | 70.0 +- 1.9 | -7% | 104 | 104 |

**Finding 5** (a blueprint-level cache from a requested class to its distinct candidate, and one for
`getDistinctForOrNull` with a marker for "none"; failures are not cached and throw every time; the per-kontainer
provider map is untouched, so `isProviderCreated` reads as before). Tests: 616 green; mutants of the new code: a
failure cached after its first throw, 5 RED (among them both "on every call" tests); the OrNull cache storing "none"
for everything, 167 RED. "before" is the latest number of each benchmark from the steps above.

| Benchmark | ns/op before | ns/op after | delta | B/op before | B/op after |
|---|---:|---:|---:|---:|---:|
| request | 4,495.1 +- 39.1 | 3,978.0 +- 40.6 | -12% | 5,840 | 5,840 |
| getStaticHot | 25.5 +- 0.7 | 15.4 +- 0.6 | -40% | 0 | 0 |
| getDynamicHot | 23.8 +- 0.2 | 14.3 +- 0.1 | -40% | 0 | 0 |
| getOrNullInterface | 26.8 +- 0.4 | 14.7 +- 0.1 | -45% | 0 | 0 |
| getAll5 | 137.0 +- 1.7 | 95.8 +- 1.7 | -30% | 64 | 64 |
| prototypeNoArg | 70.0 +- 1.9 | 58.9 +- 1.8 | -16% | 104 | 104 |
| prototype3Args | 154.2 +- 1.4 | 110.5 +- 4.6 | -28% | 176 | 176 |
| prototypeFactory | 142.3 +- 1.7 | 87.5 +- 0.9 | -39% | 104 | 104 |
| lazyLookupGet | 33.2 +- 0.3 | 29.5 +- 1.2 | -11% | 0 | 0 |
| getStaticHotT8 | 26.6 +- 0.4 | 16.4 +- 1.6 | -38% | 0 | 0 |

**Finding 10** (the providers map presized for 32 entries, so a request's map does not grow twice; the table is
still allocated on the first provider, so `create` allocates the same). `Kontainer.tools` stays eager: a lazy keeps
its identity but allocates a 24 B `Lazy` to save a 16 B object. Tests: 616 green. "before" is a run of the code
after finding 5 without this change. The cost: a kontainer that creates only a few providers allocates a 64-slot
table instead of a 16-slot one (about 190 B more).

| Benchmark | ns/op before | ns/op after | delta | B/op before | B/op after |
|---|---:|---:|---:|---:|---:|
| create | 14.8 +- 0.1 | 15.5 +- 0.1 | +4% | 264 | 264 |
| createOneOverride | 964.2 +- 11.4 | 963.0 +- 9.5 | 0% | 1,664 | 1,664 |
| request | 4,095.3 +- 36.9 | 3,659.2 +- 26.5 | -11% | 5,840 | 5,520 |
| requestT8 | 4,782.9 +- 74.8 | 4,361.5 +- 69.0 | -9% | 5,832 | 5,416 |

## Coverage map (before any change)

New spec `ProvisionPathsCharacterizationSpec` (17 tests), green on today's code. Every mutant below was applied to
TODAY's code, the kontainer suite run, the file restored with `cp`.

| Finding | Behaviour it touches | Pinning tests (existing / new) | Mutant of today's code: RED tests |
|---|---|---|---|
| 1 | provider for an unknown class | new: getProvider of an unknown class, with and without overrides | `getValue` to `[cls]!!`: the new test |
| 1 | listing order, override in place | KontainerToolsKindsSpec, ToolsCharacterizationSpec listing order; new: getAllProviders order | overrides listed first: those 3 |
| 1 | the blueprint map is not changed by an override | DynamicsAtCreate "Two kontainers", ExactOverrideMatchSpec x2, ValidationCharacterization newKontainer; new: override seen only by its own kontainer | override written into the shared map: those 5 |
| 1 | getAllProviders registers what it creates | new: getAllProviders creates the missing providers | not registering: the new test |
| 1, 4 | provider once per kontainer, shared for global singletons, concurrent first requests | ServiceProviderFactorySpec (12), ServiceProviderCreatedSpec | (existing, not re-run as mutants) |
| 3 | getAll and Lookup resolve each candidate again | new: getAll ambiguity, List / Lazy List / Lookup ambiguity | skipping re-resolution in getAll: both new tests; in the Lookup: the Lookup test |
| 3, 5 | messages of not found and ambiguous, on every call | new: get unknown, get ambiguous; ExactOverrideMatch, DynamicsAtCreate | message changed: 5 RED (ambiguous), 1 RED (not found) |
| 3, 5 | getOrNull: one candidate, none, two | OptionalsSpec, InjectionForms; new: getOrNull on every call | two candidates give the first: the new test |
| 5 | getProvider of a super type | BaseTypeInjection, all factory specs; new: getProvider of a super type | resolving by the requested class: 37 RED |
| 7 | argument order and values | the three factory specs, GeneralInjection, GlobalKontainer | arguments reversed: 55 RED (singletons), 23 RED (prototypes) |
| 9 | refusal order and messages | BuiltInServicesCharacterization, DynamicPrototypeSpec, DynamicsAtCreate | ambiguity check dropped: 2 RED; dynamics check before the built-in check: 12 RED |
| 6 | createInstance edge cases | new: two all-optional constructors, throwing no-arg constructor, private no-arg constructor | `ctor.call()`: two-constructor test; unwrapping InvocationTargetException: throwing test; Java constructor made accessible: two-constructor and private tests |
| 8 | constructor call with arguments | new: throwing constructor with arguments, private constructor with arguments, value class parameter | unwrapping: throwing test; Java constructor made accessible: private and value class tests |
| 10 | `tools` identity | `Kontainer.tools` is a `val`: a lazy keeps the identity but allocates a `Lazy` (24 B) to save a 16 B object, so no gain; not planned |

Also pinned as it is, not touched by any finding: an `object` registered as a class service is refused at the
definition (no primary constructor).

**Finding 3, confirmed latent bug (a question for the maintainer, not fixed):** with `PathsMiddle : PathsBase` and
`PathsLeaf : PathsMiddle` both registered, `getAll(PathsBase)`, a `List<PathsBase>` or `Lazy<List<PathsBase>>`
injection, and `Lookup<PathsBase>.get(PathsMiddle::class)` throw `ServiceAmbiguous` for `PathsMiddle`. The new
tests pin today's behaviour, so finding 3 is applied only to `getOrNull` (every plain, nullable and Lazy
injection): there the one candidate's own candidates are always exactly itself, so the second resolution cannot
change the result.

## Review ledger

Round 1, standard gate, three charters (implementation and code style, domain, security), Opus high (rung 1).
Zero CRITICAL, zero MAJOR: PASS. Security: no findings.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR (domain) | `Kontainer.kt:131` | Finding 3's getOrNull shortcut is not behaviour-neutral for aliased keys (`Int::class` and `java.lang.Integer`): which form asks first decides the shared cache entry, and the second resolution read and filled it | fix | Reverted to `get(type, context)`; two pins, RED on the shortcut, green on 912f035c and after the revert (c893a64d) |
| 1 | MINOR (impl) | `KontainerBenchmark.kt:37` | The setup comment says every global singleton is created; only those a request touches are | fix | Comment corrected (761dbbdf) |
| 1 | MINOR (impl) | `TypeLookup.kt` | A public, subclassable base now caches `getDistinctFor` successes | fix | KDoc line on `getAllCandidatesFor`: its answer must be stable per type (761dbbdf) |
| 1 | surplus (impl) | `ServiceProducer.kt` | Finding 6 copies kotlin-reflect internals for about 5 ns per instance | fix, dropped | Production change reverted, tests kept (3f541b04) |
| 1 | surplus (impl, domain) | `ServiceProviderFactory.kt` | Finding 10 makes `create` slower | reject | Measured: request -11%, requestT8 -9% against create +4% (0.7 ns, same B/op), see Finding 10 |
| 1 | surplus (impl) | `ProvisionPathsCharacterizationSpec.kt` | The "object as class service" test pins something no finding touches | reject | A harmless pin of today's behaviour, cheap and green |
| 1 | surplus (security) | `ultra/kontainer/build.gradle.kts` | The JMH plugin is applied on a published module | maintainer-decision | Whether the harness moves to its own module is the maintainer's call |

CLEAN in round 1: nothing writes the shared provider map; override isolation at create and clone; the new caches hold
only types and constructors, successes only; `getProvider` read-then-`computeIfAbsent` creates each provider once;
the init order of `createProvider`; failures never cached; cache keys bounded by loaded classes; identity of
providers and singletons unchanged; ordering of `getAllProviders` unchanged.

## Acceptance

- Baseline and after numbers for every benchmark recorded here; no benchmark regresses beyond noise.
- No public API change; all existing kontainer and consumer suites green unchanged (tests added, none weakened).
- Every changed path was covered by a test that went RED on a mutant of the current code before the change.

## History

- 2026-11-24: created on the maintainer's word from the read-only performance and thread-safety audit, with the
  conditions above ("This should not lead to any behaviour changes, only internal changes"). Start after the other
  kontainer tracks.
- 2026-11-24: IN PROGRESS. baseline measured, coverage mapped; waiting for the go
- 2026-11-24: Track E: JMH harness and baseline, coverage map, 17 characterization tests mutation-checked on
  today's code; finding 3's getAll throw confirmed and pinned. Stopped for the go.
- 2026-11-24: Track E, after the go: findings 1, 4, 7, 3 (getOrNull), 6, 5, 10 committed one by one with their
  deltas; 9 implemented, measured as a regression, reverted; a finding 4 variant measured and dropped. Final full
  run, 14 suites and the compile sweep green. Stopped for the gate.
- 2026-11-24: gate round 1 PASS. MINOR batch: finding 3 reverted (aliased `Int` / `Integer` keys, two pins), finding 6
  dropped, KDoc on `getAllCandidatesFor`, benchmark comment fixed; ledger recorded. Affected benchmarks re-run.
- 2026-11-24: merged into master (31711410). Gate PASS in round 1; the MINOR batch reverted findings 3 and 6 and was checked by hand (both files back to master, one KDoc line). Open with the maintainer: finding 2 (override stack trace), the getAll/List/Lookup ServiceAmbiguous throw, the DynamicOverrides.Builder copy, where the JMH harness lives.
- 2026-11-24: DONE.
