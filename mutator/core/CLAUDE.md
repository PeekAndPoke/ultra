# mutator/core

- **Not battle-tested**, despite its age and commit count (maintainer). Git history is not maturity.
- **Known defect:** `ListMutator.subList()` throws (`src/commonMain/kotlin/ListMutator.kt:260`), and so does the
  stdlib's `slice(IntRange)`, which calls it; `chunked()` and `windowed()` work — ULTRA-00234.
- **Specs go in `src/sharedTest`**, compiled for the JVM and JS; not `jvmTest`, not `commonTest` (it cannot see the
  generated mutators). A `@Mutable` test fixture lives in `commonTest`, package `io.peekandpoke.mutator.domain`.
