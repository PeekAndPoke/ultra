# ULTRA-00338: Docs: ultra/kontainer after ULTRA-00319 (ServiceType, ServiceKind, dynamicPrototype)

> **Status (2026-11-14): NOT STARTED.** 
>
> **Part of:** ULTRA-00260 (Epic: Docs backlog)

## Why this exists

ULTRA-00319 replaced `InjectionType` and `ServiceProvider.Type` by `ServiceType`, `ServiceKind` and an `overridden`
flag, and added `dynamicPrototype()`. The Kontainer pages and the LLM mirror still describe the old five-value type.
Part of the docs sweep the maintainer wants after the foundation modules and Slumber are done (2026-11-14).

## The work

Pages `docs-site/src/pages/ultra/kontainer/*` and the mirror `docs-site/src/data/llms/kontainer.md`, which move
together; `ultra/kontainer/README.MD`.

From ULTRA-00319, "Docs lines that become false":

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

To add: the model table (two types, three kinds, only Static and Dynamic are declared, SemiDynamic is derived), when
to choose `dynamicPrototype()` over `prototype()`, that only dynamic singletons can be overridden at
`create { with { } }`, and the cause shown by `dumpKontainer()` and `getDebugInfo()`.

Known behaviours with tickets (ULTRA-00327 to ULTRA-00337) are documented only once each is decided.

## Acceptance

- Every statement on the Kontainer pages is true against the merged code; `pnpm astro build` is green.

## History

- 2026-11-14: created with the archive of ULTRA-00319 (public API changed, code settled).
- 2026-11-14: batch K1 is merged (`f575dd29`) and now in scope: the redefinition rule (ULTRA-00331), `extend`
  applying it (ULTRA-00330), `clone` checking like `create` (ULTRA-00332), lazy overrides (ULTRA-00333), the built-ins
  that can be neither registered nor overridden (ULTRA-00336). Still open and not to be documented yet: ULTRA-00327,
  ULTRA-00328, ULTRA-00329, ULTRA-00334, ULTRA-00335.
- 2026-11-14: batch K2 is merged (`8778c4a9`) and now in scope: the remembered validation result (ULTRA-00327), the
  cycle check reading every injection form, with the composite in its own list as the case to explain (ULTRA-00328),
  ambiguous injections and the decorator (ULTRA-00334), ambiguous nullable injections (ULTRA-00335). Still open:
  ULTRA-00329.
