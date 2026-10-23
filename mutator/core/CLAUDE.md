# mutator/core

- **Not battle-tested**, despite its age and commit count (maintainer). Git history is not maturity.
- **Known defect:** `ListMutator.subList()` throws (`src/commonMain/kotlin/ListMutator.kt:260`), and stdlib functions
  such as `chunked()` call it internally — ULTRA-00234.
