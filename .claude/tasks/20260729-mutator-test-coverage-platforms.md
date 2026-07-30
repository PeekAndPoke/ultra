# Mutator: specs are JVM-only, so JS and native are unverified

**Status:** TODO — pick up when the `mutator` package is reviewed
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

- [ ] Move the platform-agnostic specs from `mutator/core/src/jvmTest/` to `src/commonTest/`. Most
      should move unchanged — check for JVM-only dependencies (`java.*`, kotest JVM-only matchers,
      reflection) before moving each one.
- [ ] Leave genuinely JVM-specific specs where they are (the two under `jvmTest/kotlin/e2e/` are
      likely candidates — they exercise the KSP-generated code).
- [ ] Confirm afterwards that `jsBrowserTest` reports a non-zero count, again from
      `build/test-results/**/TEST-*.xml`. A green `allTests` proves nothing here.
- [ ] While in there: check whether native targets are configured for `mutator/core` at all, and
      whether they should be.

## Note on the Observable change of 2026-07-29

`Mutator.kt:121` was simplified to `observe(block)` when the weak-reference machinery was removed
from `ultra/common`'s `Observable`. That change is verified by:

- `mutator:core:jvmTest` — 154 tests, JVM only
- `:mutator:core:compileKotlinJs` — compiles clean
- `kraft:core-tests:jsTest` — 394 browser tests green, which exercise mutators through Kraft form
  state, i.e. indirect JS cover

No direct JS assertion on mutator behaviour exists. Worth re-checking once the specs move.
