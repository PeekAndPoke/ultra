# Funktor & Monko — Known Issues

**Date:** 2026-03-26 (updated 2026-03-31: VaultScope CRITICAL + HIGH fixed in Wave 1)
**Source:** Senior engineer, QA engineer, coroutine expert code reviews
**Related:** [funktor-v1-roadmap.md](funktor-v1-roadmap.md), [monko-query-dsl.md](monko-query-dsl.md)

---

## Summary

| Category                | CRITICAL | HIGH  | MEDIUM | LOW   | Fixed  |
|-------------------------|----------|-------|--------|-------|--------|
| Monko / MonkoRepository | 0        | 0     | 2      | 3     | 6      |
| Coroutine / Concurrency | 0        | 3     | 4      | 2     | 2      |
| Security / Auth Logic   | 0        | 0     | 0      | 0     | 4      |
| Funktor Logic           | 0        | 2     | 4      | 3     | 0      |
| **Total**               | **0**    | **5** | **10** | **8** | **12** |

**Top priorities (remaining):**

1. ~~VaultScope `runBlocking`~~ **FIXED (Wave 1, 2026-03-31)**
2. WorkerTracker cancellation broken — running workers can't be stopped (HIGH coroutine)
3. ~~`setPassword` does not verify caller authorization~~ **FIXED (2026-03-31)**
4. ~~Sign-up race condition — duplicate users possible~~ **FIXED (2026-03-31)**

---

## Fixed Issues (2026-03-26)

| # | Severity | Issue                                                   | Fix Applied                                                               |
|---|----------|---------------------------------------------------------|---------------------------------------------------------------------------|
| 1 | CRITICAL | `MonkoCursor.fullCount` always null — pagination broken | `fullCount` passed via constructor, driver runs `countDocuments()`        |
| 2 | HIGH     | N+1 delete in `removeAllButLastSuccessful`              | Refactored to use `deleteMany` with ID exclusion                          |
| 3 | HIGH     | N+1 delete in `removeAllEndedAfter`                     | Refactored to use `deleteMany` with time filter                           |
| 4 | MEDIUM   | `recreateIndexes()` crashes on `_id_` index             | Skips `_id_` in the drop loop                                             |
| 5 | MEDIUM   | TTL index double-counts retention in log repo           | Changed to `expireAfter(0, SECONDS)`                                      |
| 6 | MEDIUM   | `remove(Stored)` doesn't fire after-delete hooks        | Added `hooks.applyOnAfterDeleteHooks()`                                   |
| 7 | CRITICAL | Password recovery token not invalidated after use       | Token deleted immediately after successful reset via `removeAuthRecord()` |
| 8 | LOW      | Wrong log message in password recovery email failure    | Changed "Password Changed" to "Password Recovery"                         |

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

## Open Issues — Coroutine / Concurrency (from deep review)

### ~~CRITICAL: VaultScope.launch uses `runBlocking`~~ FIXED (Wave 1, 2026-03-31)

- **FIXED:** Now uses `CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { block() }`.

### ~~HIGH: VaultScope has no lifecycle management~~ FIXED (Wave 1, 2026-03-31)

- **FIXED:** `VaultScope.shutdown()` cancels the `SupervisorJob`.

### HIGH: `runBlocking` inside Ktor lifecycle event handlers — nested blocking risk

- **File:** `funktor/core/src/jvmMain/kotlin/lifecycle/AppLifeCycleBuilder.kt` (lines 37-122)
- **Impact:** All lifecycle hook dispatchers use `runBlocking` to call suspend functions. If hooks
  themselves trigger `VaultScope.launch` (which also uses `runBlocking`), nested `runBlocking` on the
  same thread pool can deadlock. Also, `onAppStarting` (line 122) runs `runBlocking` eagerly during
  builder construction, not during an event.
- **Fix:** Ensure inner `runBlocking` uses distinct dispatchers (`Dispatchers.IO`). Consider migrating
  to suspending lifecycle events if Ktor supports them.

### HIGH: WorkerTracker.lastRuns not synchronized

- **File:** `funktor/cluster/src/jvmMain/kotlin/workers/services/WorkerTracker.kt` (lines 119-129)
- **Impact:** `putLastRunInstant` and `getLastRunInstant` read/write `lastRuns` (a `HashMap`) without
  synchronization, while other methods (`clear`, `clearFutureRuns`, `lockWorker`) do synchronize.
  Workers run concurrently on `Dispatchers.IO`, causing data races.
- **Fix:** Wrap in `sync {}` blocks, or replace `lastRuns` with `ConcurrentHashMap`.

### HIGH: WorkerTracker.lockWorker — job reference set after completion, cancellation broken

- **File:** `funktor/cluster/src/jvmMain/kotlin/workers/services/WorkerTracker.kt` (lines 98-108)
- **Impact:** `coroutineScope { async(context) { block() } }` waits for completion, so
  `runningWorkers[workerId]?.job = job` only executes AFTER the block finishes. During execution,
  `job` is always `null`. `clear()` calls `job?.cancel()` which always hits `null` — running workers
  **cannot be cancelled during shutdown**.
- **Fix:** Use `CoroutineScope(context).async { ... }` and set the job reference before awaiting.

### MEDIUM: WorkersRunner.state — non-volatile shared variable

- **File:** `funktor/cluster/src/jvmMain/kotlin/workers/WorkersRunner.kt` (line 25)
- **Impact:** `var state = State.Running` is read by IO dispatcher coroutine and written by
  `onAppStopping` handler. No `@Volatile`, no atomic. JVM memory model doesn't guarantee visibility.
- **Fix:** Add `@Volatile` or use `AtomicReference<State>`.

### MEDIUM: TimingInterceptor.children — unsynchronized mutable list

- **File:** `funktor/core/src/jvmMain/kotlin/coroutines/timing.kt` (line 50)
- **Impact:** `mutableListOf<TimingInterceptor>()` mutated in `copyForChild()` and read in
  `getCpuProfile()` from different threads. `ConcurrentModificationException` possible.
- **Fix:** Use `ConcurrentLinkedQueue` or `Collections.synchronizedList()`.

### MEDIUM: WorkerHistory.Vault — lastFlush/lastCleanup not synchronized

- **File:** `funktor/cluster/src/jvmMain/kotlin/workers/services/WorkerHistory.kt` (lines 64-68)
- **Impact:** `lastFlush` is written inside `synchronized(buffer)` but read outside it. `lastCleanup`
  similarly inconsistent. Multiple worker coroutines race on these fields.
- **Fix:** Access under same lock, or use `AtomicReference<MpInstant>`.

### MEDIUM: WorkerHistory.Vault.putRun — `coroutineScope { launch }` blocks the caller

- **File:** `funktor/cluster/src/jvmMain/kotlin/workers/services/WorkerHistory.kt` (lines 96-106)
- **Impact:** `coroutineScope` waits for all children, so this is not async — the caller is suspended
  until flush/cleanup completes. If intent was fire-and-forget, this is wrong. If intent was to switch
  to IO, `withContext(Dispatchers.IO)` would be clearer.
- **Fix:** Clarify intent. Use `withContext` if synchronous, separate scope if async.

### LOW: PrimitiveGlobalLocksProvider.Storage — static singleton, never cleaned up

- **File:** `funktor/cluster/src/jvmMain/kotlin/locks/PrimitiveGlobalLocksProvider.kt` (lines 16-18)
- **Impact:** `lockMap` keys never removed when lists become empty. Memory leak over time. Shared
  across instances and tests.
- **Fix:** Remove empty entries. Consider instance-scoped storage.

### LOW: LogbackKarangoLogAppender.append — fire-and-forget without error handling

- **File:** `funktor/logging/src/jvmMain/kotlin/karango/LogbackKarangoLogAppender.kt` (line 103)
- **Impact:** `application.launch(Dispatchers.IO) { repository.tryInsert(entry) }` — if tryInsert
  throws, exception propagates to Application's handler. If this triggers another log event that fails,
  recursive loop possible.
- **Fix:** Wrap in `try/catch` to swallow errors silently.

---

## Open Issues — Security / Auth Logic

### ~~CRITICAL: Password recovery token not invalidated after use~~ FIXED

- **FIXED:** Token is now deleted via `removeAuthRecord()` immediately after successful password reset.

### ~~HIGH: `setPassword` does not verify caller authorization or current password~~ FIXED (2026-03-31)

- **FIXED:** Added `currentPassword` field to `AuthSetPasswordRequest`. `setPassword()` now validates
  the current password before allowing a change. Frontend `ChangePasswordWidget` updated to collect it.
  Route handler already had identity check (`userId` match). Tests added for wrong-password case.

### ~~HIGH: Sign-up race condition — duplicate users possible~~ FIXED (2026-03-31)

- **FIXED:** `signUp()` now wraps `createUserForSignup()` in try-catch to handle duplicate key
  exceptions from concurrent sign-ups. Returns `AuthError("User already exists")` on race.
  Comment documents that a unique index on email in the user repository is required.

### ~~LOW: Wrong log message in password recovery email failure~~ FIXED

- **FIXED:** Changed "Password Changed" to "Password Recovery" in the log message.

---

## Open Issues — Funktor Logic (pre-existing, not caused by Monko work)

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

### MEDIUM: Attack detection delay creates thread-holding DoS vector

- **File:** `funktor/rest/src/jvmMain/kotlin/ApiStatusPages.kt` (lines 98-105)
- **Impact:** `.php`/`/wp-admin` requests get a `delay(500ms)` before responding. While `delay` doesn't
  block a thread, it holds coroutine resources. At 2000 req/s, ~1000 coroutines suspended simultaneously
  — could exhaust memory.
- **Fix:** Immediately return 404 for blacklisted patterns. Do rate limiting at reverse proxy level.

### MEDIUM: Lock cleanup can release locks held by alive servers

- **File:** `funktor/cluster/src/jvmMain/kotlin/locks/workers/GlobalLocksCleanupWorker.kt` (lines 28-48)
- **Impact:** Releases locks where `serverId !in aliveServerIds`. If `getAll()` returns empty list
  (transient DB error), ALL locks are released. Also, a newly started server that hasn't sent its first
  beacon yet could have its locks released.
- **Fix:** Skip dead-server cleanup if `aliveServerIds` is empty. Require at least one historical beacon
  before cleanup.

### MEDIUM: `archiveJob` deletes before archiving — data loss on archive failure

- **File:** `funktor/cluster/src/jvmMain/kotlin/backgroundjobs/BackgroundJobs.kt` (lines 572-582)
- **Impact:** Removes job from queue first, then saves to archive. If archive save fails (DB timeout),
  job is permanently lost — removed from queue but never archived.
- **Fix:** Archive first, then remove from queue. Or wrap in a transaction.

### MEDIUM: `dataHash` collision risk in `queueIfNotPresent`

- **File:** `funktor/cluster/src/jvmMain/kotlin/backgroundjobs/domain/BackgroundJobQueued.kt` (lines 21-29)
- **Impact:** 32-bit `Int.hashCode()` has birthday-problem collisions at ~65K distinct values per type.
  Collision prevents legitimate distinct job from being enqueued — silent job loss.
- **Fix:** Store full serialized data or SHA-256 hash for secondary verification alongside the int hash.

### LOW: `page=0` vs `page=1` inconsistency in LogsFilter

- **File:** `funktor/logging/src/commonMain/kotlin/LogsFilter.kt`
- **Impact:** `LogsFilter` defaults to `page = 1`, but some tests use `page = 0`. If pagination is
  1-indexed, `page=0` would return unexpected results. If 0-indexed, the default `page=1` skips
  the first page.
- **Fix:** Decide on 0-indexed or 1-indexed convention and enforce it everywhere. Document it in
  LogsFilter.

### LOW: `WorkerHistory.Adapter.InMemory` is not thread-safe

- **File:** `funktor/cluster/src/jvmMain/kotlin/workers/services/WorkerHistory.kt` (lines 36-59)
- **Impact:** Singleton `object` with unsynchronized `mutableMapOf` and `mutableListOf`. Workers run
  on `Dispatchers.IO` concurrently. `ConcurrentModificationException` possible.
- **Fix:** Add synchronization or use concurrent collections. Likely only used in dev/test.

### LOW: `BackgroundJobRetryPolicy` only has `LinearDelay` — no exponential backoff

- **File:** `funktor/cluster/src/jvmMain/kotlin/backgroundjobs/domain/BackgroundJobRetryPolicy.kt`
- **Impact:** Under failure conditions, linear retry can overwhelm external services.
- **Fix:** Add `ExponentialBackoff` policy variant.

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
