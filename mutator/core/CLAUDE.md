# mutator/core

- **Not battle-tested**, despite its age and commit count (maintainer). Git history is not maturity.
- **`ListMutator.subList()` is a read-only snapshot, not a live view** (`src/commonMain/kotlin/ListMutator.kt:302`):
  adding, removing or setting through it throws. `slice(IntRange)`, `chunked()` and `windowed()` work. ULTRA-00234.
- **Specs go in `src/sharedTest`**, compiled for the JVM and JS; not `jvmTest`, not `commonTest` (it cannot see the
  generated mutators). A `@Mutable` test fixture lives in `commonTest`, package `io.peekandpoke.mutator.domain`.
