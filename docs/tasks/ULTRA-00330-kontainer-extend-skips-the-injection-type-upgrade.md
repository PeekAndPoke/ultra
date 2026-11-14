# ULTRA-00330: Kontainer: extend skips the injection type upgrade

> **Status (2026-11-14): NOT STARTED.** 
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
redefinition rule against the base blueprint. A service that is dynamic in the base stays dynamic when the extension
redefines it as a singleton, and its users stay one per kontainer (they became global singletons). A prototype stays a
prototype (it became a singleton). Silent (ULTRA-00330).

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

## The work

- `extend` applies the same redefinition rule as the builder, or refuses a redefinition that lowers the type.
- Tests first: the "CURRENT BEHAVIOUR" pin turns into the wanted expectation (RED before the fix); mutation check.
- CHANGELOG: a behaviour change without a compile signal.

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's word.
