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

## Decide before implementing (re-confirm with the maintainer)

- The two axes and their names (e.g. `kind`: Singleton / Prototype; `scope`: Static / Dynamic), and what the four
  combinations mean. Is "SemiDynamic" (a static singleton promoted by a dynamic dependency) a fifth, derived state?
- The builder API: do `singleton()`, `prototype()`, `dynamic()` stay as they are (source compatible), with a new
  form for the fourth combination?
- What tools and debug output show (`kontainer` insights, the dependency graph): the split or the old names.

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
