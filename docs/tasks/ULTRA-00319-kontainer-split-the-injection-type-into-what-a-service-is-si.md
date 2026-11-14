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

- [ ] **Tests first, on today's code, no production change:** characterization specs that pin, for every
      combination of provider and dependency, what is instantiated when and how often: singleton / prototype /
      dynamic, each depending on each, direct and transitive, through `Lazy`, `List`, `Lookup` and
      `InjectionContext`; promotion to SemiDynamic and how far it spreads; overrides at `create { with { } }`;
      validation errors; the blueprint's debug info. They must be green before any change and stay green after it.
- [ ] Mutation-check the characterization specs against today's code (the promotion rule, the prototype
      exemption), so they are known to bite.
- [ ] Then the split, behind the unchanged builder API; the new combination gets its builder function; `Log` in
      `ultra/log` uses it and its TODO goes.
- [ ] Every consumer suite green (funktor-all, demo); CHANGELOG; the Kontainer docs page and the skill.

Kontainer is battle-tested (CLAUDE.md, project facts): a strict gate is in order although the ticket is not
security-critical; the maintainer decides.

## History

- 2026-11-13: created on the maintainer's word, from the log scoping discussion (ULTRA-00149 L11, ULTRA-00158).
- 2026-11-14: the model decided with the maintainer (two types, three kinds, override as a flag, `dynamicPrototype()`).
