# Funktor & Monko — Known Issues

**Date:** 2026-03-26
**Source:** Senior engineer + QA engineer code reviews after Monko repo creation
**Related:** [funktor-v1-roadmap.md](funktor-v1-roadmap.md), [monko-query-dsl.md](monko-query-dsl.md)

---

## Fixed Issues (2026-03-26)

| # | Severity | Issue                                                   | Fix Applied                                                        |
|---|----------|---------------------------------------------------------|--------------------------------------------------------------------|
| 1 | CRITICAL | `MonkoCursor.fullCount` always null — pagination broken | `fullCount` passed via constructor, driver runs `countDocuments()` |
| 2 | HIGH     | N+1 delete in `removeAllButLastSuccessful`              | Refactored to use `deleteMany` with ID exclusion                   |
| 3 | HIGH     | N+1 delete in `removeAllEndedAfter`                     | Refactored to use `deleteMany` with time filter                    |
| 4 | MEDIUM   | `recreateIndexes()` crashes on `_id_` index             | Skips `_id_` in the drop loop                                      |
| 5 | MEDIUM   | TTL index double-counts retention in log repo           | Changed to `expireAfter(0, SECONDS)`                               |
| 6 | MEDIUM   | `remove(Stored)` doesn't fire after-delete hooks        | Added `hooks.applyOnAfterDeleteHooks()`                            |

---

## Open Issues — Monko / MonkoRepository

### MEDIUM: `runBlocking` in MonkoDriver.version lazy property

- **File:** `monko/core/src/main/kotlin/MonkoDriver.kt` (lines 106-113)
- **Impact:** Blocks dispatcher thread on first access. Called on every query via `getConnectionName()`.
  Under high concurrency, risks thread starvation.
- **Fix:** Make `version` a suspend function with caching, or initialize eagerly during driver setup.
- **Workaround:** Only blocks once (lazy), then cached. Low impact in practice unless first query
  happens under heavy load.

### MEDIUM: Enum comparison uses `.name` string — fragile

- **Files:** `backgroundjobs/monko/MonkoBackgroundJobsQueueRepo.kt`,
  `workers/monko/MonkoWorkerHistoryRepo.kt`
- **Impact:** `Filters.eq(fieldPath, EnumValue.name)` assumes Slumber serializes enums as their `.name`.
  If serialization convention ever changes, filters silently return no results.
- **Fix:** Add a helper function or comment documenting this dependency. Consider creating a DSL
  extension like `infix fun <T : Enum<T>> MongoPropertyPath<*, T>.eq(value: T)` that handles
  the `.name` conversion automatically.

### LOW: `batchInsert` is sequential in MonkoWorkerHistoryRepo

- **File:** `funktor/cluster/.../workers/monko/MonkoWorkerHistoryRepo.kt`
- **Impact:** Inserts one document at a time. Should use MongoDB's `insertMany()`.
- **Priority:** Low — batch sizes are typically small (< 10).

### LOW: MonkoSentMessagesRepo mixes raw Filters with DSL

- **File:** `funktor/messaging/.../monko/MonkoSentMessagesRepo.kt` (line 60)
- **Impact:** Uses `Filters.in(field {...}, refs)` instead of DSL operator. Works correctly but
  inconsistent style. The `isIn` operator doesn't work here because `r.lookup.refs` is `Set<String>`
  and we need "any element of the field array matches any element of the input set" semantics.
- **Fix:** Consider adding an `anyIn` DSL operator for array-to-array membership queries.

### LOW: LogsStorageBaseSpec references KarangoLogEntry directly

- **File:** `funktor/logging/.../LogsStorageBaseSpec.kt` (line ~43)
- **Impact:** Base test spec casts repository to `Repository<KarangoLogEntry>` for inserting test data.
  May break when Monko spec runs if the cast fails.
- **Fix:** Use an abstract `insertTestEntry()` method that each DB-specific spec implements, or
  insert via the `LogsStorage` interface instead of directly via repository.

---

## Open Issues — Funktor (pre-existing, not caused by Monko work)

### HIGH: `queueIfNotPresent` has TOCTOU race across JVMs

- **File:** `funktor/cluster/src/jvmMain/kotlin/backgroundjobs/BackgroundJobs.kt` (lines 186-193)
- **Impact:** `hasWaitingByTypeAndDataHash()` + `create()` are two separate operations. In a
  multi-server cluster, two servers can both pass the check and both insert, creating duplicate jobs.
  The in-process `Mutex` only protects within a single JVM.
- **Fix:** Add a unique compound index on `(type, dataHash, state)` in both Karango and Monko repos,
  and catch the duplicate key exception. Or use an atomic upsert.

### HIGH: Global lock expiration not checked during acquisition

- **File:** `funktor/cluster/src/jvmMain/kotlin/locks/VaultGlobalLocksProvider.kt`
- **Impact:** The retry loop in `doAcquireInternal` never checks if the existing lock has expired.
  If a server crashes while holding a lock, all other servers are blocked until the cleanup worker
  runs (which only runs periodically). In the worst case, a crashed server blocks the entire cluster
  for the cleanup interval.
- **Fix:** During acquire, check if the existing lock's `expires` field is in the past. If so, treat
  the lock as free (delete it and re-acquire). This makes lock recovery near-instant instead of
  depending on the cleanup schedule.

### MEDIUM: ServerBeacon `update()` TOCTOU race on first creation

- **Files:** `locks/karango/KarangoServerBeaconRepo.kt`, `locks/monko/MonkoServerBeaconRepo.kt`
- **Impact:** `modifyById(serverId) { ... } ?: insert(key=serverId, ...)` has a race window where
  two concurrent callers both see `null` from `findById`, then both call `insert`. The second will
  fail with a duplicate key error.
- **Severity:** Low in practice — self-healing (next heartbeat succeeds). Same pattern in both
  Karango and Monko.
- **Fix:** Use atomic upsert instead of find-then-insert.

### MEDIUM: Static mutable state in `WorkerHistory.Adapter.Vault`

- **File:** `funktor/cluster/src/jvmMain/kotlin/workers/services/WorkerHistory.kt` (lines 64-68)
- **Impact:** `buffer`, `lastFlush`, and `lastCleanup` are static companion object fields. This means:
    1. State leaks between tests (test isolation violation)
    2. Multiple `Vault` adapter instances share the same buffer (if DI creates more than one)
- **Fix:** Move these fields to instance properties. For test isolation, either reset them in test
  setup or use a fresh adapter per test.

### LOW: `page=0` vs `page=1` inconsistency in LogsFilter

- **File:** `funktor/logging/src/commonMain/kotlin/LogsFilter.kt`
- **Impact:** `LogsFilter` defaults to `page = 1`, but some tests use `page = 0`. If pagination is
  1-indexed, `page=0` would return unexpected results. If 0-indexed, the default `page=1` skips
  the first page.
- **Fix:** Decide on 0-indexed or 1-indexed convention and enforce it everywhere. Document it in
  LogsFilter.

---

## Test Coverage Gaps

| Area                          | Gap                                                          | Priority |
|-------------------------------|--------------------------------------------------------------|----------|
| LogsStorage update/bulkAction | Zero tests for `update()` and `execBulkAction()`             | P1       |
| Pagination                    | No multi-page tests for logs, jobs, or storage               | P1       |
| Concurrent claimNextDue       | No test for two callers racing to claim the same job         | P1       |
| removeAllButLastSuccessful    | No test for the keep-N-delete-rest logic                     | P1       |
| TTL cleanup                   | No test for automatic document expiry                        | P2       |
| Index creation                | No test verifying indexes exist after `ensureIndexes()`      | P2       |
| Attachments                   | SentMessages tests always pass `emptyList()` for attachments | P2       |
| Auth findByToken unknown      | No test for `findByToken` with a token that doesn't exist    | P2       |
