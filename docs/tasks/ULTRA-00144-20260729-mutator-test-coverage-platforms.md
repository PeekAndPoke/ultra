# ULTRA-00144: Mutator: specs are JVM-only, so JS and native are unverified

> **Status (2026-11-13): IN REVIEW.** implemented on `wt/mutator` (track C of ULTRA-00316); the review gate is next
>
> **Part of:** ULTRA-00255 (Ultra libraries epic)

**Plan:** noticed while simplifying `Observable` in `ultra/common` (2026-07-29)
**Security-critical:** no

## What was found

`mutator/core` is a multiplatform module consumed by Kraft on JS, but **every one of its 13 spec
files lives in `src/jvmTest/`**. `src/commonTest/` holds only fixtures (`domain/`,
`index_commonTest.kt`) — no specs. Measured from the test-result XML, not the gradle summary:

```
mutator/core   jvmTest 154   jsBrowserTest 0
```

`:mutator:core:allTests` therefore reports success while running nothing on JS.

Contrast `ultra/common`, where the specs sit in `commonTest` and consequently execute everywhere:

```
ultra/common   jvmTest 323   jsBrowserTest 152   linuxX64Test 121
```

## Why it matters

A `commonMain` defect that only manifests on JS or native cannot be caught by mutator's suite. That
is not hypothetical in this repo — the `ultra/common` scan turned up several genuine
platform-divergent defects (JS `WeakSet.size` returning `NaN`, JS `WeakSet.toSet()` throwing, native
`RunSync` being a non-re-entrant global spin lock). Anything of that shape inside mutator would ship
green.

Relevant project fact from `CLAUDE.md`: **Mutator is NOT battle-tested**, despite its age and commit
count. It is exactly the module where thin coverage is most likely to hide something.

## Spec

- [x] Move the platform-agnostic specs from `mutator/core/src/jvmTest/` to `src/commonTest/`. Most
      should move unchanged — check for JVM-only dependencies (`java.*`, kotest JVM-only matchers,
      reflection) before moving each one. *All 14 moved unchanged, to `src/sharedTest/` rather than
      `commonTest` — see Implementation notes.*
- [x] Leave genuinely JVM-specific specs where they are (the two under `jvmTest/kotlin/e2e/` are
      likely candidates — they exercise the KSP-generated code). *None is JVM-specific; every spec uses
      KSP-generated code, the e2e ones included.*
- [x] Confirm afterwards that `jsBrowserTest` reports a non-zero count, again from
      `build/test-results/**/TEST-*.xml`. A green `allTests` proves nothing here.
- [x] While in there: check whether native targets are configured for `mutator/core` at all, and
      whether they should be. *None configured; dropped (consumers are Kraft on JS and the JVM).*

## Implementation notes

- **Not `commonTest`.** All 14 specs call the per-class mutators that KSP generates for the `commonTest`
  domain fixtures. KSP generates per platform test compilation (`kspTestKotlinJvm`, `kspTestKotlinJs`),
  KSP has no `commonTest` target, and under K2 `commonTest` cannot see a platform source set's
  generated code: the first attempt, specs in `commonTest`, failed to compile on both platforms with
  `Unresolved reference 'mutator'` and the like. The specs therefore live in `mutator/core/src/sharedTest/kotlin`,
  added as a source directory of both `jvmTest` and `jsTest`. `src/examples` already used this pattern
  (a `jvmTest` srcDir). `src/jvmTest` keeps only its package file.
- **`id("io.kotest")` added.** Without the Kotest Gradle plugin, Kotest 6 discovers no specs on JS (Gradle:
  "the test task did not discover any tests").
- **The mutator processor no longer runs in `kspJsTest`.** With it there, Kotest's JS discovery
  processor crashes: `KotestSymbolProcessor.finish()` calls `getContainingFile()` on class declarations it
  kept from round 1, and the mutator processor's generated files force a second round, which KSP2
  invalidates ("KaInvalidLifetimeOwnerAccessException: PSI has changed since creation"). It reproduces with
  the KSP caches deleted, so it is a Kotest 6.2.6 + KSP 2.3.12 defect, not stale state. `jsTest` instead
  compiles the domain mutators from `kspTestKotlinJvm`
  (`generated/ksp/jvm/jvmTest/kotlin/io/peekandpoke/mutator/domain`, wired with `builtBy`). The JS run's
  output had been byte-identical to the JVM run's (`diff -r` on all 16 files). Trade-off: this module no
  longer runs the mutator processor on a JS compilation. A Kotest release whose processor survives
  multi-round KSP would let `add("kspJsTest", ...)` come back.
- `failOnNoDiscoveredTests = false` removed: JS now discovers tests.
- No spec failed on JS.
- Coverage gap found by the mutation check (not fixed, out of scope): no spec asserts that
  `ListMutator.add(index, element)` notifies (`ListMutator.kt:151-154`). Removing its `notifyObservers()`
  survives on both platforms.

## Test evidence

Counts from `.build-claude/mutator/core/test-results/*/TEST-*.xml`, via
`console/wt-gradle.sh :mutator:core:jvmTest :mutator:core:jsBrowserTest -x kotlinStoreYarnLock`:

| | jvmTest | jsBrowserTest |
|---|---|---|
| before | 154 tests, 14 specs, 0 failures | 0 (no XML) |
| after the move | 154, 14 specs, 0 failures | 155 = the same 14 specs with identical per-spec counts + 1 Kotest bootstrap entry, 0 failures |
| with the ULTRA-00234 probe spec | 158, 0 failures | 159, 0 failures |

- Mutation check on JS: removing `notifyObservers()` from `ListMutator.add(element)` made
  `ListMutatorNotificationsSpec` "add() should notify" fail on `jsBrowserTest` (1 of 155). Restored.

## Changelog text

None: test and build changes only; the published artifacts are unchanged.

## Note on the Observable change of 2026-07-29

`Mutator.kt:121` was simplified to `observe(block)` when the weak-reference machinery was removed
from `ultra/common`'s `Observable`. That change is verified by:

- `mutator:core:jvmTest` — 154 tests, JVM only
- `:mutator:core:compileKotlinJs` — compiles clean
- `kraft:core-tests:jsTest` — 394 browser tests green, which exercise mutators through Kraft form
  state, i.e. indirect JS cover

No direct JS assertion on mutator behaviour exists. Worth re-checking once the specs move.
*Re-checked 2026-11-13: every spec now runs on JS, green.*

## History

- 2026-10-21: numbered ULTRA-00144 (was `docs/tasks/20260729-mutator-test-coverage-platforms.md`).
- 2026-11-13: implemented on `wt/mutator`; specs in `src/sharedTest`, running on JVM and JS.
