# ULTRA-00353: Kontainer: no stack trace per override, overrides copied at build, JMH harness in its own module

> **Status (2026-11-24): IN PROGRESS.** Track F: a, b and c done, tests first and mutation-checked, measured; waiting for the gate

## Why this exists

Open points left by ULTRA-00351 (kontainer creation and provision performance), decided by the maintainer on
2026-11-24.

## Decided (maintainer, 2026-11-24)

1. **No stack trace per override, option (c): "yes c".** Each override at `create`/`clone` builds a
   `ServiceDefinition` whose default `codeLocation = CodeLocation.create()` is a `Throwable()` (`ServiceDefinition.kt:21`,
   built in `ServiceProviderFactory`). It walks the whole call stack on every request (about 1 us, most of what an
   override still costs) and stores only `ServiceProviderFactory.<init>`, which tells nobody anything. Instead the
   override's location names the class of the override lambda (for example `ApiKontainerKt$createApi$1$1`), which
   points at the file and function where `with { }` was written, with no stack walk. Visible in the dump and debug info
   only (`KontainerTools.kt:44-45`).
2. **Overrides copied at build: "yes fix it please".** `DynamicOverrides.Builder.build()` wraps the builder's live
   map (`DynamicOverrides.kt:15`), and the kontainer keeps it for `clone`. A builder kept after `create` and used again
   would change what later clones get. Fix: `DynamicOverrides(overrides.toMap())`.
3. **The JMH harness moves to its own unpublished module: "move to own unpublished module".** Today it is
   `ultra/kontainer/src/jmh` with the `me.champeau.jmh` plugin in the published module's build. It uses public API only.
   Target: e.g. `ultra/kontainer-benchmarks` (unpublished, like `ultra/slumber-test-classes`); `ultra/kontainer`'s
   build goes back to having no JMH plugin. Run command updated in ULTRA-00351's notes and wherever documented.

## The work

- Tests first for 1 and 2 (the location of an override in debug info and dump; a builder used after `create` does not
  change a later clone), RED before the change, mutation-check.
- Measure 1 with the harness (createOneOverride, createTwoOverrides, cloneOneOverride, the request mix) before and after.
- 3 is build-only: the benchmark compiles and runs from the new module; no published module applies the JMH plugin.
- CHANGELOG: 1 and 2 under Changed (small, debug-visible) with the measured gain; 3 under Build.

## Implementation (Track F, wt/kontainer)

**1. Override location (554fc10c).** `ServiceDefinition.CodeLocation` gets an internal factory
`CodeLocation.of(origin: Class<*>)`; `create()` is unchanged for blueprint definitions. `ServiceProviderFactory` passes
`CodeLocation.of(provider.javaClass)`, the override lambda's class: no `Throwable`, no stack walk. Its `stack` is one
`StackTraceElement(<lambda class>, "invoke", null, -1)`, built on first use; `first()`, `stackPrinted` and the debug
info read it as before. The two `by lazy` caches stay as they were (gate round 1: the hand-written `@Volatile` caches
of 554fc10c saved 80 B per override, `createOneOverride` 976 B with the lazies, 896 B without, but were reverted;
`stackPrinted` had no test).

**Finding: Kotlin 2 compiles lambdas with invokedynamic.** The lambda's class is a hidden class, so the location
reads `io.peekandpoke.ultra.kontainer.e2e.ServiceDefinitionsSpec$4$$Lambda$770/0x000075b0a8375a50.invoke(Unknown
Source)`, not `ApiKontainerKt$createApi$1$1` as this ticket expected. It names the class or file facade where
`with { }` was written, not the function, and carries a per-JVM suffix. Nothing better is available without a stack
walk. To the maintainer: keep as is, or strip the `$$Lambda...` suffix to show only the enclosing class.

The dump shows no code location (`KontainerTools.dumpKontainer` has no such column); the debug info
(`ServiceDefinitionInfo.codeLocation`) and `CodeLocation.stack` / `first()` show it. Validation messages use only
blueprint definitions, never an override.

Tests (`ServiceDefinitionsSpec`), both RED before the change (the location was
`ServiceProviderFactory.<init>(ServiceProviderFactory.kt:31)`):
- an override's `codeLocation.stack` is exactly `[<lambda class>]`, `first()` and the debug info name it, the
  overwritten definition keeps its blueprint location;
- an override written in place: the debug info location starts with the class where `with { }` was written.

**2. Overrides copied at build (3cf31ddd).** `DynamicOverrides.Builder.build()` is `DynamicOverrides(overrides.toMap())`.
Test (`DynamicsAtCreateCharacterizationSpec`): a builder kept after `create` and given another override: the kontainer
and a later clone still get the first override; RED before (the clone got the second).

**3. JMH harness in its own module.** `ultra/kontainer-benchmarks` (in `settings.gradle`), with only `kotlin("jvm")` and
`me.champeau.jmh`, no maven publish plugin, like `ultra/slumber-test-classes` (no publish tasks:
`:ultra:kontainer-benchmarks:tasks --all` lists none). The sources moved unchanged (`src/jmh/kotlin`, package
`io.peekandpoke.ultra.kontainer.jmh`), `jmhImplementation(project(":ultra:kontainer"))`; the `jmh { }` settings
moved unchanged. `ultra/kontainer/build.gradle.kts` has no JMH plugin and no jmh tasks; `Deps.Jmh` stays. Run:
`console/wt-gradle.sh :ultra:kontainer-benchmarks:jmh [-PjmhIncludes=<regex>]`; results in
`.build-claude/ultra/kontainer-benchmarks/results/jmh/results.json`. Proven by a short run (`create`, `getAll5`).
The old command is still written in `CHANGELOG.md` (master only, for the coordinator); ULTRA-00351 has a note.

### Mutants (each applied to the new code, kontainer suite with `-x generateDocs`, restored with `cp` and `cmp`)

| Mutant | RED |
|---|---|
| override definition without `codeLocation` (back to `CodeLocation.create()`) | both location tests |
| `CodeLocation.of` captures a `Throwable` | both location tests |
| `CodeLocation.of(instance.javaClass)` instead of the lambda's class | both location tests |
| stack cache hit returns an empty array (on the `@Volatile` caches, since reverted) | the exact-location test (its second read) |
| `build()` without `toMap()` | the kept-builder test (clone gets the second override) |

### Measurements (JMH, same settings as ULTRA-00351, 2026-11-24, load average 3 to 5; before: d957e729)

| Benchmark | ns/op before | ns/op after 1 | ns/op after 1 and 2 | B/op before | B/op after 1 | B/op after 1 and 2 |
|---|---:|---:|---:|---:|---:|---:|
| createOneOverride | 1,319.7 +- 89.8 | 120.0 +- 0.9 | 112.8 +- 1.1 | 1,664 | 816 | 896 |
| createTwoOverrides | 2,637.2 +- 332.3 | 221.4 +- 1.9 | 256.1 +- 5.5 | 2,752 | 1,168 | 1,384 |
| cloneOneOverride | 1,438.5 +- 154.6 | 160.1 +- 1.9 | 170.4 +- 1.7 | 1,864 | 1,032 | 1,112 |
| request | 4,280.7 +- 68.5 | 3,053.2 +- 62.9 | 3,075.0 +- 54.9 | 5,512 | 4,600 | 4,680 |

Reading: the stack capture was about 1.2 us and 850 B per override (-91% for one override). The copy of decision 2 costs
80 B per create with one override (a singleton map) and 216 B with two (a LinkedHashMap), about 35 ns for two.
After the move, from the new module: `create` 17.4 +- 0.2 ns, 248 B; `getAll5` 59.2 +- 2.0 ns, 64 B.
After the gate's revert to `by lazy` (one run, load average 3.5 to 4): `createOneOverride` 124.0 +- 1.1 ns, 976 B
(the 80 B of the two lazies back; still -91% in time and -41% in bytes against 1,319.7 ns and 1,664 B).

### Suites and compile sweep (after 8e65db73, counts from the XML, all written by this run)

| Suite | Tests | Failures | Errors |
|---|---:|---:|---:|
| `:ultra:kontainer:test` | 624 | 0 | 0 |
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
compileTestKotlin compileJmhKotlin --continue`): BUILD SUCCESSFUL, 0 lines `e:`, `compileJmhKotlin` now in
`:ultra:kontainer-benchmarks`. These runs cover ULTRA-00352 as well.

## Review ledger

Round 1, standard gate, three charters (implementation and code style, domain, security), Opus high (rung 1), for
ULTRA-00352 and ULTRA-00353 together. Zero CRITICAL, zero MAJOR: PASS. This ticket's rows (ULTRA-00352 has the
aliasing row):

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR (security, impl) | `funktor/all/src/jvmTest/kotlin/InsightsGraphsApiSpec.kt:191` | `codeLocation shouldNotContain "ServiceProviderFactory"` no longer tells a request's graph from the blueprint's | fix | Asserts the UserProvider definition's `overwrites` is null (an override always overwrites the blueprint definition; field checked on `InsightsKontainerGraph.Definition`), soft with the type check; mutant (graph from a kontainer with the UserProvider override): RED on the new assertion, restored |
| 1 | MINOR (security) | `ServiceProviderFactory.kt` | The hidden-class suffix carries a native address (`/0x...`) into the debug info | maintainer-decision | Whether to strip the `$$Lambda...` suffix is asked; not stripped |
| 1 | MINOR (impl) | `ServiceDefinition.kt` | `stackPrinted` has no test after the cache rewrite | fix | Reverted to `by lazy` as before |
| 1 | surplus (impl) | `ServiceDefinition.kt` | Hand-written `@Volatile` caches for 80 B | fix | Reverted; `createOneOverride` re-measured (976 B) |
| 1 | surplus (impl) | `ServiceDefinitionsSpec.kt` | `println` in a new test | fix | Removed |
| 1 | surplus (impl) | `ServiceDefinition.kt` | Dead `origin?.name ?: "n/a"` fallback | fix | Gone with the revert: `checkNotNull(origin)` states the invariant |
| 1 | surplus (impl) | `ultra/kontainer-benchmarks/build.gradle.kts` | group and version on an unpublished module | reject | Mirrors `ultra/slumber-test-classes`, harmless |
| 1 | surplus (impl) | `DynamicOverrides.kt` | The copy is redundant on the clone path (merge copies again) | reject | One `build()` for create and clone is simpler; measured (about 10 ns, 80 B) |

CLEAN in round 1: the CodeLocation API is compatible and blueprint definitions keep their stack; the benchmark module
is unpublished and `ultra/kontainer` has no JMH plugin.

## Acceptance

- No stack walk on the override path; the override's location names the lambda's class; both changes pinned by tests.
- `:ultra:kontainer` builds without the JMH plugin; the harness runs from its own module; all suites green.

## History

- 2026-11-24: created on the maintainer's word.
- 2026-11-24: IN PROGRESS. Track F: a, b and c done, tests first and mutation-checked, measured; waiting for the gate
- 2026-11-25: gate round 1 PASS (0 CRITICAL, 0 MAJOR); MINOR batch applied: CodeLocation back to `by lazy`, println removed, InsightsGraphsApiSpec asserts `overwrites`; the `$$Lambda` suffix is with the maintainer. Kontainer 626 / 0 / 0, funktor/all 289 / 0 / 0 (XML).
