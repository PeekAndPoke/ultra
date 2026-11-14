# ULTRA-00319: Kontainer: split the injection type into what a service is (singleton, prototype) and how it behaves (static, dynamic)

> **Status (2026-11-13): DESIGNED.** 
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

## Why this exists

Kontainer has three injection types: `Singleton`, `Prototype`, `Dynamic` (`ultra/kontainer/src/main/kotlin/
InjectionType.kt`). They mix two questions: what a service is (one shared instance, or a new one per injection)
and how it behaves over the life of a blueprint (the same for every kontainer, or bound to one kontainer, i.e. one
request). A prototype that depends on a dynamic service has no name: `Log` in `ultra/log` is one (a TODO there asks
for a "dynamicPrototype"), and a singleton that injects it silently becomes one instance per kontainer
("SemiDynamic").

The maintainer (2026-11-13), on two options (a fourth type `DynamicPrototype`, or splitting the type): "I somehow
lean to option 2. but this will be the most work. But since this is a foundational piece of software we should do
it. But in doing it we have to be VERY CAREFUL to not alter any behaviour. So a test-first approach seems advisable
here."

What this does NOT do, by itself (review of ULTRA-00158, 2026-11-13): a singleton that injects a dynamic prototype
still becomes one instance per kontainer. The split names and exposes that; it does not stop it. Whether `Log`
should stop being dynamic is a separate decision in ULTRA-00158.

## Decided with the maintainer (2026-11-14)

**The model: two questions per service.**

| | Static | Dynamic | SemiDynamic |
|---|---|---|---|
| **Singleton** | one instance for all kontainers (`Singleton` today) | one instance per kontainer, declared (`Dynamic` today) | one instance per kontainer, because it injects something dynamic (`SemiDynamic` today) |
| **Prototype** | new instance per injection, nothing dynamic behind it (`Prototype` today) | new instance per injection, declared as bound to the kontainer (no name today; `Log`) | new instance per injection, injects something dynamic (no name today) |

- **Type:** Singleton, Prototype. **Kind:** Static, Dynamic, SemiDynamic (the maintainer's words for the two axes;
  the final identifiers are chosen when building, and must not clash with `InjectionType` and `ServiceProvider.Type`).
- **Only Static and Dynamic can be declared.** SemiDynamic is always derived by the blueprint: a service declared
  Static that injects, through any chain (prototypes in between included), something Dynamic or SemiDynamic.
- For a prototype the three values change nothing about when it is created; they say what it is bound to, which is
  what makes a singleton above it SemiDynamic. For a singleton, Dynamic and SemiDynamic behave the same at runtime.
- **"Override" is its own flag**, not a value of the kind: a dynamic service replaced at `create { with { } }`
  (`ServiceProvider.Type.DynamicOverride` today).
- **Builder API:** `singleton()`, `prototype()`, `dynamic()` stay as they are; **`dynamicPrototype()` is new**, for
  the one combination that cannot be declared today.
- **Tools and debug output** (maintainer: "whatever makes sense"): show both answers and the override flag, and for a
  SemiDynamic service name the dynamic service(s) that made it so. Keep the old names readable where a tool's output
  is parsed or pinned by a test; decide per tool when building.
- **No behaviour change:** the same objects are created at the same times as today. Read from the code, not yet
  run: the path singleton -> prototype -> dynamic ends in SemiDynamic (`KontainerBlueprint.semiDynamicDefinitions`,
  `DependencyLookup.getAllDependents` walks through prototypes; prototypes are filtered out of the set). One of the
  first cases the characterization specs pin.

**Decided for step 2 (maintainer, 2026-11-14):**

- **Names:** `ServiceType` (Singleton, Prototype) and `ServiceKind` (Static, Dynamic, SemiDynamic).
- **Cleanest variant, backward compatibility is not an issue:** `InjectionType` and `ServiceProvider.Type` with their
  mixed names go; a service carries `ServiceType`, `ServiceKind` and the override flag. Tools and debug output show
  those. The characterization specs that pin the old enum entries and the listing order change on purpose, each such
  change listed in the ledger.
- **Overrides at `create { with { } }`: only for dynamic singletons, as today.** No override for a prototype of any
  kind, `dynamicPrototype` included.
- **A prototype that injects something dynamic is reported as Prototype with kind SemiDynamic.**
- **The eight CURRENT BEHAVIOUR findings stay unchanged through the split;** each is fixed in its own ticket:
  ULTRA-00327 (validation once), ULTRA-00328 (cycle through List), ULTRA-00329 (Lookup bound to the first
  kontainer), ULTRA-00330 (extend skips the upgrade), ULTRA-00331 (prototype redefined as dynamic), ULTRA-00332
  (clone skips the dynamics check), ULTRA-00333 (eager overrides), ULTRA-00334 (decorator self-cycle).
- The guard that keeps prototypes out of the SemiDynamic singletons is made explicit (today only the order of the
  provider map does it; mutant M1).

## The work

- [x] **Tests first, on today's code, no production change:** characterization specs that pin, for every
      combination of provider and dependency, what is instantiated when and how often: singleton / prototype /
      dynamic, each depending on each, direct and transitive, through `Lazy`, `List`, `Lookup` and
      `InjectionContext`; promotion to SemiDynamic and how far it spreads; overrides at `create { with { } }`;
      validation errors; the blueprint's debug info. They must be green before any change and stay green after it.
      (2026-11-14, see "Characterization (step 1)".)
- [x] Mutation-check the characterization specs against today's code (the promotion rule, the prototype
      exemption), so they are known to bite. (2026-11-14, 21 mutants, all red.)
- [x] Then the split, behind the unchanged builder API; the new combination gets its builder function; `Log` in
      `ultra/log` uses it and its TODO goes. (2026-11-14, see "Step 2: built".)
- [ ] Every consumer suite green (funktor-all, demo); CHANGELOG; the Kontainer docs page and the skill.

Kontainer is battle-tested (CLAUDE.md, project facts): a strict gate is in order although the ticket is not
security-critical; the maintainer decides.

## Characterization (step 1)

Done 2026-11-14 on branch `wt/kontainer`, no production change. Specs in
`ultra/kontainer/src/test/kotlin/e2e/characterization/`; constructor runs are counted by a test-only `Births`
counter (`helpers.kt`), independent of what the kontainer tracks.

| Spec | Tests | Pins |
|---|---|---|
| `ProviderMatrixCharacterizationSpec` | 47 | singleton / prototype / dynamic, each on each: direct (9), two levels (27), three levels (10 chosen), plus the singleton -> prototype -> dynamic path on its own. Per row: the reported type in two kontainers, the constructor runs, which instances are shared, what each service sees of its dependency |
| `PromotionCharacterizationSpec` | 13 | how far SemiDynamic spreads: up every chain and through two parents, through prototypes, through interfaces and super types; not down, not sideways; `Kontainer` promotes, `KontainerBlueprint` does not |
| `InjectionFormsCharacterizationSpec` | 18 | plain, nullable, `Lazy<T>`, `Lazy<T?>`, `List<T>`, `Lazy<List<T>>`, `Lookup<T>`, `InjectionContext`, `Kontainer`, factories with several parameters: promotion, when the target is built, how often |
| `DynamicsAtCreateCharacterizationSpec` | 14 | overrides at `create { with { } }`, dynamics without one, refused overrides (exception and message), two kontainers with different overrides, `clone` |
| `PrototypesCharacterizationSpec` | 11 | new per get and per injection point, inside singleton / dynamic / prototype, what a prototype sees of a dynamic, providers per kontainer |
| `ValidationCharacterizationSpec` | 19 | missing, lazy missing, ambiguous, unknown injection, abstract service, cycles (which are and are not detected), when validation runs |
| `RedefinitionCharacterizationSpec` | 13 | a later definition with another injection type, its producer, the effect on promotion; across modules and across `extend` |
| `ToolsCharacterizationSpec` | 8 | `getDebugInfo()` and `dumpKontainer()` for one blueprint holding every `ServiceProvider.Type`; the enum entries |

Module total: 186 tests before, 329 after (XML of 2026-11-14 12:57, 0 failures).

The singleton -> prototype -> dynamic path ends in SemiDynamic for the singleton; the prototype stays Prototype and
sees the dynamic of the kontainer it is built in (verified, the matrix row and its own test).

### Mutation table

One mutant at a time in `ultra/kontainer/src/main/kotlin/`, backup with `cp`, run, restore, `cmp` identical. Every
run executed all 329 tests (XML counts).

| # | Mutant | Result | Killed by (characterization) |
|---|---|---|---|
| M1 | `KontainerBlueprint`: prototype exemption removed (`.filter { !prototypeDefinitions.contains(it) }`) | RED, 1 | Tools: listing order only (see note) |
| M1b | prototypes added to the provider map before the semi dynamics, filter kept | RED, 1 | Tools: listing order only |
| M1c | exemption removed and prototypes in the semi set get no prototype provider | RED, 28 | 22 characterization tests, plus `InjectionTypeDowngradePreventionSpec` |
| M2 | `DependencyLookup.getAllDependents` one step only | RED, 14 | matrix two / three levels, promotion chains, through interfaces |
| M3 | SemiDynamic provided by `forGlobalSingleton` | RED, 37 | matrix, promotion, forms, overrides, clone |
| M4 | Dynamic provided by `forGlobalSingleton` | RED, 52 | matrix, forms, dynamics at create, promotion |
| M5a | prototype provided by `forGlobalSingleton` | RED, 85 | matrix, prototypes, forms, promotion |
| M5b | prototype provided by `forDynamicSingleton` (one per kontainer) | RED, 75 | matrix, prototypes, forms |
| M6a | `Lazy<T>` not counted as a dependency | RED, 4 | forms: Lazy, Lazy nullable, factory with Lazy |
| M6b | `List<T>` not counted | RED, 2 | forms: List |
| M6c | `Lookup<T>` not counted | RED, 2 | forms: Lookup |
| M6d | `Lazy<List<T>>` not counted | RED, 2 | forms: Lazy List |
| M6e | `InjectionContext` not counted | RED, 2 | forms: InjectionContext in a singleton |
| M6f | nullable parameter not counted | RED, 3 | forms: nullable; validation: nullable cycle, decorator |
| M7 | dynamics check at `create` removed | RED, 8 | the five refused overrides, check on every create, prototype redefined as dynamic |
| M8 | validation on every `create` | RED, 1 | validation: runs only once |
| M9 | no upgrade when redefining a prototype | RED, 5 | redefinition: prototype redefined, order of redefinitions |
| M10 | `Kontainer` registered as singleton | RED, 7 | promotion: Kontainer promotes; forms; tools |
| M11 | cycle check follows `List<T>` | RED, 1 | validation: List cycle |
| M12 | `getAllDependents` keeps the initial set (no `.minus(of)`) | RED, 1 | Tools: listing order only |
| M13 | override reported as `Dynamic` | RED, 21 | overrides, tools, redefinition |

Note on M1, M1b, M12: the prototype exemption is redundant at runtime today. `serviceProviderProviders` adds the
prototype providers last, so a prototype that lands in the semi set is overwritten by its `ForPrototype` provider
under the same key; dropping the filter only moves the service's position in the debug listing.
`InjectionTypeDowngradePreventionSpec` does not catch M1. M1c shows the specs bite as soon as a prototype in the semi
set reaches runtime. The split must keep one of the two guards; the order of `getDebugInfo()` and
`dumpKontainer()` is pinned by one test and may be changed on purpose.

### CURRENT BEHAVIOUR findings

Pinned as they are, test names start with "CURRENT BEHAVIOUR"; whether any changes is a separate decision.

1. The override lambda of `create { with { } }` runs at create, before anything is requested; `clone()` runs it
   again (DynamicsAtCreate).
2. `clone { with { X() } }` for a non-dynamic `X` skips the dynamics check and throws `ServiceNotFound`, where
   `create` throws `KontainerInconsistent` (DynamicsAtCreate).
3. A global singleton's `Lookup<T>` stays bound to the first kontainer: prototypes fetched through it are created by,
   and recorded in, that kontainer's provider (`instances` keeps every one), also when used from later kontainers
   (InjectionForms).
4. A cycle through `List<T>` passes validation and overflows the stack at `get` (Validation).
5. A decorator with a nullable parameter of its own super type is reported as a cycle with itself (Validation).
6. After a failed validation the next `create` of the same blueprint succeeds (the flag is set before
   `validate()`); `get` then throws `InvocationTargetException` (Validation).
7. A prototype redefined as dynamic stays Prototype: its users are not promoted and it cannot be overridden at
   create (Redefinition).
8. `extend` does not apply the type upgrade: a prototype or dynamic redefined as singleton in an extension becomes
   Singleton, which also ends the promotion of the base blueprint's singletons (Redefinition).

## Step 2: the split, plan as built

Written before the build (2026-11-14); updated where the build differed.

### The types (`ultra/kontainer/src/main/kotlin/`)

- `ServiceType.kt`: `enum class ServiceType { Singleton, Prototype }`, what a service is.
- `ServiceKind.kt`: `enum class ServiceKind { Static, Dynamic, SemiDynamic }`, what it is bound to.
- `InjectionType.kt` is deleted; `ServiceProvider.Type` is deleted.
- `ServiceDefinition`: `injectionType` becomes `type: ServiceType` and `kind: ServiceKind`, the DECLARED kind,
  Static or Dynamic only (`require` in the constructor). `withType(...)` becomes `withScope(type, kind)`.
- `ServiceProvider`: `type: Type` becomes `type: ServiceType`, `kind: ServiceKind` (the derived kind) and
  `overridden: Boolean` (today's `DynamicOverride`). The provider factories become `forGlobalSingleton(definition)`,
  `forDynamicSingleton(kind, definition)`, `forOverride(definition, instance)` and `ForPrototype.Provider(kind,
  definition)`.

### The builder

- `singleton`, `prototype`, `dynamic` unchanged. New `dynamicPrototype`, a `ServiceBuilder` like the other three, so
  it has every overload they have (class, class with impl, factories with 0 to 10 parameters). `instance()` stays
  singleton only, as today.
- Built-ins unchanged: `Kontainer` and `InjectionContext` are (Singleton, Dynamic), `KontainerBlueprint` is
  (Singleton, Static).
- The redefinition rule (`KontainerBuilder.InjectionTypeUpgrade`, renamed `RedefinitionUpgrade`): a later
  definition keeps the type AND kind of the one it replaces, unless that one is a static singleton, in which case
  the later definition is taken as it is. This is today's rule restated: Prototype forced Prototype, Dynamic forced
  Dynamic, Singleton let the new one through. For `dynamicPrototype`: over a static singleton it is taken as is; over
  a prototype it stays a static prototype; over a dynamic singleton it stays a dynamic singleton; and a
  `dynamicPrototype` replaced by anything stays a `dynamicPrototype`. These choices are pinned and handed to
  ULTRA-00331.

### How the blueprint derives the kind

1. Sources: every definition declared Dynamic, singletons and prototypes alike.
2. `DependencyLookup.getAllDependents(sources)` as today; every service in it that is not itself a source is
   SemiDynamic, whatever its type. So a prototype above a dynamic is (Prototype, SemiDynamic), a declared
   `dynamicPrototype` is (Prototype, Dynamic), and both promote a singleton above them as a prototype-in-between
   does today.
3. Everything else is Static.
4. Overridable at `create { with { } }`: only (Singleton, Dynamic). The dynamics checker and the base type lookup
   for overrides take this set; a `dynamicPrototype` override is refused like any other non-dynamic
   (`KontainerInconsistent`, "Unexpected dynamics were provided").

### The provider map, and the explicit prototype guard

One `when` per definition, on the type first: a Prototype gets `ForPrototype` whatever its kind; a static Singleton
the shared global provider; a Dynamic or SemiDynamic Singleton one provider per kontainer. The guard (M1) no longer
depends on the order of the map. The map keeps today's listing order on purpose (it numbers validation errors and
orders the debug listing): static singletons, semi dynamic singletons, dynamic singletons, prototypes. As built, the
semi dynamic singletons keep their discovery order, all other groups definition order: exactly today's order, so not
even the numbering of validation errors changes.

### Tools and debug output

- `DebugInfo.ServiceDebugInfo`: `type: ServiceType`, `kind: ServiceKind`, `overridden: Boolean` and
  `semiDynamicBecauseOf: List<ClassInfo>`, the declared dynamic services a SemiDynamic service reaches (empty
  otherwise).
- `DebugInfo.ServiceDefinitionInfo`: `type: ServiceType` and `kind: ServiceKind` (declared) replace `injectionType`.
- `dumpKontainer()`: columns `Service ID | Type | Kind | Instances`; an overridden service shows its kind as
  `Dynamic (overridden)`.

### Callers outside the module

| Where | What changes |
|---|---|
| `funktor/insights/.../collectors/KontainerCollector.kt` | `Data.Service.type` from `ServiceProvider.Type` to `String`, mapped to the five old names (wire kept) |
| `funktor/insights/.../InsightsGraphs.kt` | `Service.type` and `Definition.injectionType` mapped to the old names (wire kept) |
| `ultra/log/src/jvmMain/kotlin/index_jvm.kt` | `Log` as `dynamicPrototype`, only if nothing changes at runtime (counting test first) |
| `ultra/log/.../LoggingKontainerModuleSpec.kt`, `ultra/vault/.../VaultHookScopeWiringSpec.kt` | assertions on the old enum, translated |
| GUIs (`funktor/codegen/.../ts/insights/*`, `funktor/inspect/.../insights/*`) | none, they read the unchanged wire |

### Wire shapes that carry these names (kept unchanged end to end)

1. **Insights records on disk**, slice `kontainer.services[].type`: `Singleton`, `SemiDynamic`, `Dynamic`,
   `Prototype`, `DynamicOverride`. Written by `KontainerCollector`, read raw by both GUIs (`isProcessWide` is
   `type == "Singleton"`, the colour table, the override join on `DynamicOverride`).
2. **Insights graph API** (`InsightsKontainerGraph`): `services[].type` (same names minus `DynamicOverride`) and
   `definition.injectionType` (`Singleton`, `Dynamic`, `Prototype`); the graph fingerprint hashes them.
3. Mapping, one function in `funktor/insights`: overridden -> `DynamicOverride`; any Prototype -> `Prototype`
   (also SemiDynamic and Dynamic prototypes, so the GUIs see no unknown value); Singleton Static -> `Singleton`,
   SemiDynamic -> `SemiDynamic`, Dynamic -> `Dynamic`. Definitions: Singleton Static -> `Singleton`, Singleton
   Dynamic -> `Dynamic`, any Prototype -> `Prototype`. Records written before the split and after it are the same.
   Showing the kind of a prototype in the GUIs would need new wire fields: an open choice, not built.

## Step 2: built

Done 2026-11-14 on branch `wt/kontainer`: plan `e9a9dbde`, split `18b1d1aa`.

### Result

- The split is as planned above. Hard rule held: no existing test changed a constructor count, an instance identity, an
  exception type or a message. Specs changed only where they named the old enums, the debug listing or its columns.
- `dynamicPrototype()` is new, a `ServiceBuilder` like the other three.
- `Log` in `ultra/log` is declared `dynamicPrototype`; its TODO is gone. The counting test is green before and after,
  so this changes nothing at runtime.
- The insights wire is unchanged: records on disk and the graph API carry the same five names.

### Test counts (XML, `--rerun`)

| Suite | Before | After | Failures |
|---|---|---|---|
| `:ultra:kontainer:test` | 329 | 405 | 0 |
| `:ultra:log:jvmTest` | 67 | 69 | 0 |
| `:ultra:vault:jvmTest` | 287 | 287 | 0 |
| `:funktor:insights:jvmTest` | 114 | 117 | 0 |
| `:funktor:all:jvmTest` | not run before (no tests added) | 289 | 0 |

Repo-wide compile sweep (`compileKotlinJvm compileTestKotlinJvm compileKotlinJs compileTestKotlinJs compileKotlin
compileTestKotlin --continue`): green, 0 errors. "Before" for log and insights is the after count minus the tests added.

New in kontainer (76 tests):
- `ProviderMatrixCharacterizationSpec`: 44 `dynamicPrototype` rows. Direct: 7. Two levels: all 37 with at least
  one `dynamicPrototype`.
- `DynamicPrototypeSpec`: 19 tests.
- `PrototypeKindSpec`: 7 tests.
- `KontainerToolsKindsSpec`: 6 tests.

Also new:
- ultra/log: 2 tests, counting Log instances per kontainer and pinning the declaration.
- funktor/insights: `KontainerWireNamesSpec`, 3 tests.

### Ledger: changed lines in pre-existing specs

- 21 files, 299 lines out and 410 in, by hunk:
  - 144 enum, listing or column translations, by the table.
  - 8 hunks where `Prototype` became (Prototype, SemiDynamic), each one injecting something dynamic:
    `InjectionTypeDowngradePreventionSpec` 28; `PrototypesCharacterizationSpec` 140; `PromotionCharacterizationSpec`
    162, 163 and 256; `InjectionFormsCharacterizationSpec` 361 and 393; `ToolsCharacterizationSpec` 92; plus the
    singleton -> prototype -> dynamic test in the matrix.
  - 4 matrix row blocks, the 46 old rows in the new vocabulary. 14 of them have a `Prototype.SemiDynamic` entry; the
    test names change with them. The constructor counts are unchanged, checked row by row against the old rows.
  - 9 ticket comments on the CURRENT BEHAVIOUR pins.
  - 24 import hunks.
  - 7 hunks of scaffolding for new cases: the `DP` token, `Declared.DynamicPrototype`, the Log test classes, a KDoc
    that named the deleted enum, the Log comment that became false.
- The full before and after of every line is in the track report.

### Mutation table (step 2)

One mutant at a time, backup with `cp`, run, restore, `cmp` identical. N are new, M are step-1 mutants on their moved
lines. "Tests" is the suite run.

| # | Mutant | Tests | Result |
|---|---|---|---|
| N1 | SemiDynamic derived for singletons only, not prototypes | 405 | RED, 42 |
| N2 | `dynamicPrototype` provided as one instance per kontainer | 405 | RED, 59 |
| N3 | `dynamicPrototype` provided as a global singleton | 405 | RED, 60 |
| N4 | Override refusal removed (dynamic prototypes overridable) | 405 | RED, 3 |
| N5 | Explicit prototype guard removed (a SemiDynamic prototype gets a per-kontainer singleton provider) | 405 | RED, 46 |
| N6a | Redefinition keeps the new kind instead of the replaced one | 405 | RED, 12 |
| N6b | Redefinition passes any singleton through, dynamic ones included | 405 | RED, 7 |
| N6c | Redefinition keeps the new type instead of the replaced one | 405 | RED, 11 |
| N7 | `dynamicPrototype()` declares Static | 405 | RED, 62 |
| N8 | Only dynamic singletons are sources of promotion | 405 | RED, 24 |
| N9 | `semiDynamicBecauseOf` always empty | 405 | RED, 1 |
| N10 | Prototypes listed first | 405 | RED, 2 |
| N11 | Override flag false | 405 | RED, 23 |
| N12 | `Log` back to `prototype` (ultra/log) | 69 | RED, 1: only the declaration pin; the counting test stays green |
| N13 | Wire: prototypes named by their kind (insights) | 117 | RED, 3 |
| N14 | Wire: `DynamicOverride` dropped (insights) | 117 | RED, 3 |
| M2 | `getAllDependents` one step | 405 | RED, 24 |
| M3 | SemiDynamic singleton as global singleton | 405 | RED, 81 |
| M4 | Dynamic singleton as global singleton | 405 | RED, 119 |
| M5a | Prototype as global singleton | 405 | RED, 162 |
| M5b | Prototype as per-kontainer singleton | 405 | RED, 156 |
| M6a to M6f | `Lazy`, `List`, `Lookup`, `Lazy<List>`, `InjectionContext`, nullable not counted | 405 | RED, 6 / 4 / 4 / 2 / 4 / 3 |
| M7 | Dynamics check removed | 405 | RED, 10 |
| M8 | Validation on every create | 405 | RED, 1 |
| M9 | No redefinition rule at all | 405 | RED, 16 |
| M10 | `Kontainer` registered as singleton | 405 | RED, 10 |
| M11 | Cycle check follows `List` | 405 | RED, 1 |
| M12 | `getAllDependents` keeps the initial set | 405 | SURVIVED: equivalent now |

- **M12 is an equivalent mutant now.** The blueprint checks the declared kind first and keeps only declared-static
  services as SemiDynamic, so the sources in the set change nothing (in step 1 it moved one listing position).
- **M1 of step 1 no longer exists.** The guard is the `when` on the type; N5 removes it and turns 46 tests red,
  constructor counts included.

### Choices made, for the maintainer

1. **Redefinitions with `dynamicPrototype`** (pinned in `DynamicPrototypeSpec`, handed to ULTRA-00331):
   - over a singleton: (Prototype, Dynamic)
   - over a prototype: stays (Prototype, Static)
   - over a dynamic: stays (Singleton, Dynamic)
   - replaced by anything: stays (Prototype, Dynamic), so it cannot be made overridable
   - across `extend`: no rule (ULTRA-00330)
2. **Wire names:** every prototype is `Prototype` on the wire, whatever its kind; a declared `dynamicPrototype`
   definition is `Prototype` as `injectionType`. Showing the kinds in the GUIs needs new wire fields; not built.
3. **`semiDynamicBecauseOf`** lists every declared dynamic service a SemiDynamic service reaches, also ones reached
   through another dynamic.
4. **`dumpKontainer()`** shows an overridden service's kind as `Dynamic (overridden)`; there is no separate column.
5. **`ServiceDefinition` refuses `SemiDynamic`** with `IllegalArgumentException` (`require`); nobody passes it.

### Changelog text (for the coordinator; `CHANGELOG.md` not edited)

Under "Breaking changes":

- `ultra/kontainer`: the injection type is split into what a service is and what it is bound to (ULTRA-00319).
  `InjectionType` and `ServiceProvider.Type` are gone. A service now has a `ServiceType` (`Singleton`, `Prototype`),
  a `ServiceKind` (`Static`, `Dynamic`, `SemiDynamic`) and an `overridden` flag. Only `Static` and `Dynamic` are
  declared; `SemiDynamic` is derived. A prototype that reaches something dynamic is now reported as
  (`Prototype`, `SemiDynamic`), where it was `Prototype`. What is created when, how often, and which instance is
  shared does not change. To migrate:
  - `provider.type == ServiceProvider.Type.Singleton` -> `type == ServiceType.Singleton && kind == ServiceKind.Static`
  - `Type.SemiDynamic` -> `ServiceType.Singleton` with `ServiceKind.SemiDynamic`
  - `Type.Dynamic` -> `ServiceType.Singleton` with `ServiceKind.Dynamic`
  - `Type.DynamicOverride` -> `ServiceType.Singleton`, `ServiceKind.Dynamic`, `overridden == true`
  - `Type.Prototype` -> `type == ServiceType.Prototype`; the kind is `Static`, or `SemiDynamic` when it reaches
    something dynamic
  - `ServiceDefinition.injectionType` -> `type` and `kind`, as declared: `InjectionType.Singleton` ->
    (`Singleton`, `Static`), `InjectionType.Dynamic` -> (`Singleton`, `Dynamic`), `InjectionType.Prototype` ->
    (`Prototype`, `Static`)
  - `ServiceDefinition.withType(t)` -> `withScope(type, kind)`
  - `KontainerBuilder.InjectionTypeUpgrade` -> `RedefinitionUpgrade`, same rule
  - `ServiceProvider.Provider.forInstance(type, definition, instance)` -> `forOverride(definition, instance)`;
    `forGlobalSingleton(definition)`; `forDynamicSingleton(kind, definition)`;
    `ServiceProvider.ForSingleton(kind, definition)`, `ForPrototype(kind, definition)`,
    `ForInstance(kind, overridden, definition, instance)`
  - `DebugInfo.ServiceDebugInfo.type` is a `ServiceType`, with new `kind`, `overridden` and `semiDynamicBecauseOf`;
    `DebugInfo.ServiceDefinitionInfo.injectionType` -> `type` and `kind`
  - `KontainerTools.dumpKontainer()` has a new `Kind` column

Under "New":

- `ultra/kontainer`: `dynamicPrototype()`, next to `singleton()`, `prototype()` and `dynamic()`, with the same
  overloads. A new instance on every injection like a prototype, bound to the kontainer like a dynamic: a singleton
  injecting one becomes `SemiDynamic`. It cannot be overridden at `create { with { } }`; such an override is refused
  with `KontainerInconsistent` like any other non-dynamic (ULTRA-00319).
- `ultra/log`: `Log` is declared as `dynamicPrototype`. Nothing changes at runtime: a service injecting a `Log` was
  and is `SemiDynamic` (ULTRA-00319; whether Log stays dynamic is ULTRA-00158).
- `funktor/insights`: no change on the wire. Records and the graph API keep the names `Singleton`, `SemiDynamic`,
  `Dynamic`, `Prototype`, `DynamicOverride` (ULTRA-00319).

### Docs lines that become false (list only, not edited)

- `docs-site/src/pages/ultra/kontainer/introspection.astro`:
  - 60 to 89, the table of five provider "Type" values: `type` is now `Singleton` or `Prototype`; the other three are
    a kind or the override flag.
  - 41 and 102 print `service.type` and `provider.type`: they now print only `Singleton` or `Prototype`, which is not
    what the table says.
  - 19, "their types, and instances": the dump also shows the kind.
- `docs-site/src/pages/ultra/kontainer/lifecycle.astro`:
  - 232, "the provider type will show `SemiDynamic`": it is the kind now.
  - 237 to 272, the lifecycle comparison table: no `dynamicPrototype`.
- `docs-site/src/data/llms/kontainer.md`:
  - 57, "Three lifecycles": there are four declarations.
  - 213, "Kontainer supports three service lifecycles": false, same reason.
  - 543 to 548, the comparison table: no `dynamicPrototype`.
  - 1034 to 1041, the debug example: `service.type` prints only `Singleton` or `Prototype`.
- `ultra/kontainer/README.MD` 31, "Three service types": there are two types and four declarations.
- `ultra/kontainer/CLAUDE.md` 3 stays true. It names `SemiDynamic` as what a singleton becomes; that is now a
  `ServiceKind`, and a `dynamicPrototype` promotes as a `dynamic` does.
- The kontainer skill: none exists in `.claude/skills/`.

## History

- 2026-11-13: created on the maintainer's word, from the log scoping discussion (ULTRA-00149 L11, ULTRA-00158).
- 2026-11-14: the model decided with the maintainer (two types, three kinds, override as a flag, `dynamicPrototype()`).
- 2026-11-14: step 2 decisions (names, cleanest variant, overrides, reporting); ULTRA-00327 to ULTRA-00334 created for the pinned behaviours.
