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

Still to re-confirm before the split is built: the final identifiers of the two axes.

## The work

- [x] **Tests first, on today's code, no production change:** characterization specs that pin, for every
      combination of provider and dependency, what is instantiated when and how often: singleton / prototype /
      dynamic, each depending on each, direct and transitive, through `Lazy`, `List`, `Lookup` and
      `InjectionContext`; promotion to SemiDynamic and how far it spreads; overrides at `create { with { } }`;
      validation errors; the blueprint's debug info. They must be green before any change and stay green after it.
      (2026-11-14, see "Characterization (step 1)".)
- [x] Mutation-check the characterization specs against today's code (the promotion rule, the prototype
      exemption), so they are known to bite. (2026-11-14, 21 mutants, all red.)
- [ ] Then the split, behind the unchanged builder API; the new combination gets its builder function; `Log` in
      `ultra/log` uses it and its TODO goes.
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

## History

- 2026-11-13: created on the maintainer's word, from the log scoping discussion (ULTRA-00149 L11, ULTRA-00158).
- 2026-11-14: the model decided with the maintainer (two types, three kinds, override as a flag, `dynamicPrototype()`).
