# Fix: karango collection creation race (async-driver fire-and-forget)

**Status:** DONE (2026-07-19)
**Plan:** — (bug fix)
**Security-critical:** no



## Commits & files changed

<!-- Generated 2026-07-28 from `git log --follow --name-status` over this task file.
     Commits that merely renamed the doc into the archive (R100) are excluded, since
     their code belongs to whatever task shipped alongside the move. -->

| Commit | Date | Files | Subject |
|---|---|---:|---|
| `a8195362` | 2026-07-19 | 2 | fix(karango): await collection creation to end first-run 404 race |

Archived by `d71db9f8` (rename only — that commit's code belongs to another task).

### Files changed (2)

**karango/core**
- `karango/core/src/main/kotlin/vault/KarangoDriver.kt`
- `karango/core/src/test/kotlin/e2e/E2E-EnsureEntityCollection-Spec.kt`

## Symptom

On the FIRST run of any brand-new collection, app startup failed:

```
ERROR - OnAppStarting hook ...EnsureRepositoriesOnAppStarting failed!
com.arangodb.ArangoDBException: Response: 404, Error: 1203 - collection or view not found
WARN  - KarangoDriver [WARNING] Will retry to create index 'operator_users::persistent-email' ... 404
```

Surfaced by the new `operator_users` repo (first genuinely new collection in a while). Worked on the
second run because the collection existed by then.

## Root cause

`KarangoDriver.ensureEntityCollection` (`karango/core/src/main/kotlin/vault/KarangoDriver.kt`) was a
leftover from the sync→async Arango driver migration: `.exists().await()` got its `.await()`, but the
creation call did not —

```kotlin
if (!arangoColl.exists().await()) {
    arangoDb.createCollection(name, options)   // future NOT awaited → fire-and-forget
}
```

So `ensureEntityCollection` returned before the collection existed; `Repository.ensure()` then ran
`ensureIndexes()` against a missing collection → 404. Second run: `exists()` is true (the orphaned
future had completed), so no create, indexes succeed.

## Fix

Await the creation future: `arangoDb.createCollection(name, options).await()`. One line.

MongoDB/monko is unaffected — it creates collections implicitly (no explicit `createCollection`).

## Test evidence

- `karango/core/src/test/kotlin/e2e/E2E-EnsureEntityCollection-Spec.kt` — drops the collection to
  force the first-run condition, calls `ensureEntityCollection`, asserts the collection exists the
  moment it returns.
- Verified the test **fails without the fix** (`failures=1`) and **passes with it** (`failures=0`)
  — it genuinely locks the regression.
