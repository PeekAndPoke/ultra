# Vault module scan — findings backlog

**Status:** IN PROGRESS (triaged 2026-07-28)
**Plan:** none — output of a 12-agent scan of `ultra/vault`, 2026-07-28
**Security-critical:** no (but two findings touch data exposure — see S1, S2)

## Triage

**Scope note:** the `monko/` and `karango/` packages get their own scan later. Findings whose fix
lands in a driver are deferred here even when they were found from vault — marked ⏸ DRIVER.

### DONE — fixed and mutation-checked, 2026-07-28/29

| | Finding | Fix |
|---|---|---|
| A1 | `Ref.eager` never seeds `_cached` | private primary ctor takes `initialCache` |
| A2 | `Ref._key` discards its argument | falls back through `_cached?._key` |
| A3 | unsaved entities all share identity | empty-`_id` guard in `hasSameIdAs` (fail-closed) |
| A4 | graph builder misses `Ref` in collections | type-arg walk moved above the blacklist |
| A5 | graph builder NPEs on array fields | null-safe `package` |
| B3 | query timings lost when a query throws | `try`/`finally` in both `StopWatch.Impl` methods |
| D | unused import, wildcard import | removed / expanded |
| Q1 | `@Vault.Ignore` no-op on ctor properties | documented, with the round-trip reason |
| Q3 | `DefaultEntityCache` never cached negatives | new `NullableCache` in `ultra/cache`, used by both it and `SharedRepoClassLookup` |
| Q4 | `Ref.equals` collapsed unsaved refs | empty-`_id` guard, reflexivity pinned by test |
| Q5 | hook `CancellationException` escaped `Inline` | check `currentCoroutineContext().isActive` instead of trusting the type |
| Q6 | codecs skipped `wrapIfNonNull` | applied to `Ref` only — see below |

**Q6 landed narrower than proposed.** Wrapping `Stored` broke 3 Karango e2e tests, and for a
load-bearing reason: `EntityRepository.findById` issues `RETURN(DOCUMENT(repo, id)).wrapAsStored()`,
and AQL yields `null` for a missing document — the whole "findById returns null when absent"
contract depends on `Stored<T>` awaking to null. So `Ref` is wrapped, `Stored` deliberately is not,
and both halves now say why in the code. `Ref` gains the precise path (`root.owner` instead of
`root`) and stops silently returning null on a direct non-nullable awake.

### Still open — needs a decision

- **Q2** soft delete is opt-in and `findById` bypasses it (S1/S2). Decided NOT to filter `findById`:
  it would make restoring an accidentally-deleted row impossible, and it would make one method safe
  while every hand-written query stayed unfiltered. Real fix is enforcement at the repository layer —
  belongs to the driver scans. The unique-index half is split out into the two tasks below.
- **B5** `DefaultEntityCache` is unbounded. `FastCache` in `ultra/cache` already has
  maxEntries/maxMemory/TTL behaviours and is the natural fit.
- **C1–C3** cache locking: the sync and async paths use disjoint locks, and `clear()` is not ordered
  against an in-flight provider. Now inherited by `NullableCache`, documented there in one place
  rather than duplicated. Note `EntityCache.getOrPut` (the sync variant) has **no production
  caller** — dropping it would remove the second lock and the issue with it.
- **Q4 follow-on** `Stored` and `Ref` still have different equality semantics for the same row
  (`stored != stored.asRef`). Decided against unifying: id-only equality would silently make every
  `shouldBe` on a `Stored` vacuous. Sets/maps of Storables are not meaningful — use `hasSameIdAs`.

### Spun out into their own tasks

- `.claude/tasks/20260729-karango-softdelete-unique-indexes.md`
- `.claude/tasks/20260729-monko-softdelete-unique-indexes.md`

### Deferred to the driver scans ⏸

A6 (`MonkoCursor.timeMs` always 0.0), C4/C8/C9 (delete-path hooks and cache invalidation),
B4 (`totalNs` double-count — the fix is per driver), C16, and the `MonkoWorkerHistoryRepo`
batch-insert claim.

Twelve sub-agents scanned all 31 main-source files of `ultra/vault`, one file at a time. Missing KDoc
was written directly for `lang/`, `hooks/`, `cli/`, `addons/`, `annotations.kt`, `TypedQuery.kt`,
`results.kt`, `repository_extensions.kt`, `VaultModels.kt`, `VaultConfig.kt`, `vault_module.kt`
(no executable code touched — verified by diff). What remains below is LOGIC findings only.

**Nothing here has been fixed.** Items marked ✅ were verified by the coordinator against the code
or by running a probe; the rest are agent claims that still need adversarial verification before
anyone acts on them.

---

## A. Verified by probe

### A1 ✅ `Ref.eager()` never seeds `_cached` — `asRef` is unusable without a suspend resolve
`domain.kt:229`. Probe output:
```
stored.asRef._rev   = ''      (stored._rev was 'rev1')
stored.asRef.asStored  THREW IllegalStateException: Ref(col/1) not yet resolved
```
`eager()` builds the inner `Stored` and immediately discards it into a lambda. Every non-suspend
accessor (`_rev`, `asStored`, `asRef`, `valueInternal`, `helpers.kt` `filterValueIsInstanceOf`)
either throws or silently returns `""`. Two KDocs actively promise otherwise: `domain.kt:47`
("wrapping the already-resolved value") and `domain.kt:228` ("Wraps an already-loaded value").
Fix is one line: `_cached = stored` in `eager()`.

### A2 ✅ `Ref._key` discards the `_key` it was given
`domain.kt:263` derives `_key` from `_id` instead. Probe: `Stored(_key="weird").asRef._key == "1"`.
Matters for `New(_key = k)` — `_id` is `""`, so the explicit key is lost entirely on `.asRef`.

### A3 ✅ Identity helpers treat all unsaved entities as the same entity
`domain.kt:91-101`. Probe: `New(a) hasSameIdAs New(b) == true`, and
`setOf(New(a).asRef, New(b).asRef).size == 1`. `_id` defaults to `""` and there is no empty guard.
`Ref.equals` is `_id`-only (`domain.kt:267`), so unsaved refs collapse in any Set/Map.
Agent checked `funktor/saas/.../OrgIsolationGuard.kt:75`, which uses `hasSameIdAs` as an
authorization decision — **not currently reachable** because `OrgId.parseOrNull` runs first.

### A4 ✅ `DatabaseGraphBuilder` never finds a `Ref` inside any collection
`tools/DatabaseGraphBuilder.kt:86` (package blacklist) runs BEFORE `:91` (type-argument walk).
`List<Ref<Product>>` short-circuits on `kotlin.collections` and its arguments are never visited.
So `data class Order(val items: List<Ref<Product>>)` produces `references = []` and the insights
graph draws no edge. Same for `Set`, `Map`, `Pair`. Fix: move the argument loop above the check.

### A5 ✅ `DatabaseGraphBuilder` NPEs on any entity with an array field
Same line. Probe: `ByteArray::class.java.isPrimitive == false` and `.package == null`, so
`cls.java.\`package\`.name` throws. `data class Attachment(val data: ByteArray, ...)` takes down the
whole Vault insights panel, not just that node. Also hits default-package classes.

### A6 ✅ `MonkoCursor.timeMs` is always `0.0`, profiling on or off
`MonkoDriver.kt:325` constructs `MonkoCursor(...)` without passing `_timeMs`, so it takes the
constructor default of `0.0` (`MonkoCursor.kt:14`). Karango computes it from the profiler
(`karango/.../cursor.kt:106`), Monko never does. `Cursor.kt:71` documents the property as "The time
the query took". So the same application code reports real timings on Arango and a constant zero on
Mongo. Strictly worse than the `NullQueryProfiler` case in D below, which at least depends on config.

---

## B. High — unverified, need checking before action

### B1 Vault codecs are the only ones that silently return null for a non-nullable type
`slumber/VaultSlumberModule.kt:32` — `getAwaker`/`getSlumberer` never call `SlumberModule.wrapIfNonNull`,
unlike every other module (`BuiltInModule.kt:129`, `JavaTimeModule.kt:24`, `KotlinxTimeModule.kt:19`).
Claimed effect: a document with `org: null` for a non-null `Ref<Organisation>` yields a null element
inside a `List<Stored<OrgMember>>`, NPE-ing far from the cause with no diagnostic, because the
path-tracking second pass in `Codec.awakeInternal` never runs.

### B2 An after-save hook can fail a save that already committed
`VaultHookScope.kt:109` rethrows every `CancellationException`. Under `Inline` — the default for
directly constructed drivers and all e2e tests — a hook using `withTimeout` propagates out of
`repo.save()` after the write landed, so the caller retries an already-persisted write. The class
KDoc at `VaultHookScope.kt:19-22` claims failures are "never propagated"; the repo's own test
`VaultHookScopeSpec.kt:52-61` asserts the opposite.

### B3 Query timings are discarded whenever the query throws
`profiling/QueryProfiler.kt:32` — `StopWatch.Impl.invoke`/`async` record elapsed time only on the
success path, no try/finally. A query that dies on a 30s timeout is registered with `totalNs = 0`,
so the slowest operation in the request renders as free.

### B4 `Entry.totalNs` double-counts nested and concurrent stopwatches
`profiling/QueryProfiler.kt:123` sums all five stopwatches with no nesting model. Monko nests
`measureDeserializer` inside `measureQuery`, so a 500 ms query reports 900 ms. Karango runs query and
EXPLAIN concurrently, so enabling `explain` appears to double query time. Flows into `Cursor.timeMs`.

### B5 `DefaultEntityCache` is unbounded
`caching.kt:52` — no size cap, no TTL, only manual `clear()`. Karango's cursor puts every deserialized
entity into it (`karango/.../cursor.kt:63-65`), so a streaming export over a large collection is not
memory-bounded even though the cursor is. Contrast `funktor/rest/.../index_jvm.kt:50-54`, which caps
the slumber cache at `maxMemory/10`.

---

## C. Medium — concurrency and correctness

- **C1** `caching.kt:67` — sync `getOrPut` locks `syncLock`, async `getOrPutAsync` locks `asyncMutex`.
  Two disjoint locks over one map, so the "no double provider call" guarantee in the class KDoc
  (`caching.kt:44`) does not hold across paths. *Note: the sync variant has no production caller
  today, which caps the real impact.*
- **C2** `caching.kt:81` — one cache-wide mutex is held across the provider, which is a DB round
  trip. 50 parallel ref resolutions for 50 distinct ids serialize behind it.
- **C3** `caching.kt:54` — `clear()` takes neither lock, so a provider in flight writes its result
  after the clear. Defeats the explicit invalidation at `BackgroundJobs.kt:484-486`.
- **C4** Entity cache is **never invalidated on delete** — `EntityRepository.remove` never touches
  `entityCache`. Within one request, a `Ref` to a just-deleted row resolves to the deleted entity
  instead of throwing.
- **C5** `VaultHookScope.kt:89` — `bound.isActive` is a TOCTOU check; a hook launched just before
  shutdown never runs and logs nothing at all.
- **C6** Deferred hooks have **no ordering guarantee** (`VaultHookScope.kt:97`, independent `launch`
  onto `Dispatchers.IO`), while `Inline` is strictly ordered. The whole test suite runs the ordered
  mode; production runs the unordered one.
- **C7** Deferred hooks capture the request-scoped driver/codec/`EntityCache`, so they read through a
  cache frozen at request end and pin the request's object graph while they run.
- **C8** After-delete hooks fire unconditionally, ignoring `RemoveResult` — `MonkoRepository.kt:183`,
  `EntityRepository.kt:301`. A retried delete that removes nothing still fires cascades. And
  `EntityRepository.kt:332` maps every `KarangoQueryException` to `count = 0`, so a write conflict is
  indistinguishable from "already gone".
- **C9** `remove(entities: Iterable<...>)` fires **no** after-delete hooks while `remove(entity)`
  does (`EntityRepository.kt:309`) — switching to the bulk overload silently disables them.
- **C10** Containment is per repository operation, not per hook (`Repository.kt:89` plain `forEach`):
  the first hook that throws cancels every later hook, and the log names the repo, not the hook.
- **C11** `caching.kt:65` — `entries[id] as? T` can never fail (T is unreified, bound `Any?`), so the
  "wrong type falls through to the provider" reading is wrong; the CCE is deferred to the caller.
- **C12** `Repository.saveIfModified` returns `stored.asStored` on the no-op branch — for a `New`
  input that fabricates a `Stored` that was never persisted, with `_id = ""`. All four new tests use
  a `Stored` input, so this branch is untested.
- **C13** `Expression.downcast()` declares its own `T`, shadowing the interface's, so the `T : D`
  bound constrains nothing — it is exactly as unsafe as `forceCastTo` while its name implies a
  checked cast. Reachable from the AQL DSL.
- **C14** `hooks/Timestamped.kt:35` — `createdAt` can be silently overwritten: the insert-vs-update
  decision reads the value's own sentinel, and `OnBeforeSave` is not suspend so it cannot read the
  stored row back. Rebuilding an entity from a DTO on update resets its creation date.
- **C15** `TimestampedHook.kt:31` — `as X` is an unchecked erased cast; `withCreatedAt`/`withUpdatedAt`
  are declared to return the widest type `Timestamped`, so a subtype-changing implementation compiles
  and persists the wrong type.
- **C16** `DatabaseTools` is registered `singleton` but injects the dynamic `DatabaseGraphBuilder`, so
  it is silently semi-dynamic and `DatabaseGraphBuilder.model by lazy` memoizes for exactly one
  request — the full reflection walk re-runs on every insights render.

---

## S. Data-exposure adjacent

- **S1** Soft delete is **opt-in filtering with no framework enforcement**, and `findById` bypasses it
  on both backends. REST entity params bind via `findById` (`funktor/core/.../broker/vault/vault.kt:31`),
  so a soft-deleted row still binds and still passes `OrgIsolationGuard`. `B2bMembersApi` is safe only
  because every mutation re-reads through the filtering `findByOrgAndUser`. Any handler using
  `params.member.value()` directly returns removed-member data with a 200.
- **S2** Soft-deleted rows still occupy unique indexes (no partial filter on either backend), so
  `OrgMembersStorage.add()` for a previously-removed member throws a raw driver error (Arango 1210 /
  Mongo E11000) instead of a domain outcome. Pinned by `OrgMembersStorageBaseSpec.kt:127`.

---

## D. Low / cleanup

- `domain.kt:6` — unused `VaultDslMarker` import left by an uncommitted edit that removed all three usages.
- `annotations.kt:4` — wildcard import `kotlin.annotation.AnnotationTarget.*`, forbidden by `CLAUDE.md`.
- `@Vault.Ignore` is a **no-op on primary-constructor properties** (`KarangoKspProcessor.kt:137`,
  `MonkoKspProcessor.kt:130` filter it only on the non-ctor branch). `@Vault.Ignore val passwordHash`
  compiles, generates a query path anyway, and warns about nothing.
- `@Vault.Field`/`@Vault.Ignore` declare `AnnotationTarget.FUNCTION`, but no consumer reads functions.
- `profiling/QueryProfiler.kt:100` — `queryExplained` KDoc is a copy-paste of `totalCount`'s.
- `measureIterator` / `measureSerializer` are never written by any driver; the insights panel always
  renders "0.00 ms (0x)". Karango's real serialization cost (`KarangoDriver.kt:107`) is unmeasured.
- `NullQueryProfiler` makes `Cursor.timeMs` a constant `0.0`, and it is the **default**
  (`VaultConfig.profile = false`). `Cursor.kt:71` documents the property as "the time the query took".
- `DefaultQueryProfiler.kt:8` — non-`@Volatile` var written under lock, read unlocked.
- `slumber/VaultSlumberModule.kt:44` — `New` has a Slumberer but no Awaker, so `New<X>` is write-only;
  the class KDoc claims all three types round-trip.
- `slumber/StoredSlumberer.kt:34` — `_rev` is written by nobody and read by `StoredAwaker`, so no
  in-process round trip preserves it (harmless today: no optimistic locking anywhere in the stack).
- `RefCodec.kt:29` — `data.split("/").first()` accepts any shape; a bare key becomes its own
  collection name and fails later as a `VaultException`, which extends `Throwable` not `Exception`.
- `DatabaseGraphBuilder.kt:51` — `visitedTypes` dedupes by `KType`, so two fields of the same type
  collapse to one edge, but a nullable variant counts separately. Edge weights are meaningless.
- `DatabaseGraphBuilder.kt:83` — comment says all refs are lazy; the model tags every reference
  `Type.Direct`, making `Reference.Type.Lazy` and the dashed-edge branch in `VaultCollector` dead code.
- `lang/index.kt` — four of the five `@DslMarker` annotations are only ever applied to functions,
  where `@DslMarker` has no effect. Inert decoration that looks load-bearing.
- `MonkoWorkerHistoryRepo.kt:95` — `batchInsert` loops single inserts, contradicting the interface
  KDoc's "single database round-trip" promise.
- `RepoFixtureLoader.kt:190` — zips batch-insert results to inputs by index with no size check.
- `Database.recreateIndexes()` has no per-repository error isolation: a failure mid-run leaves one
  repo with indexes dropped and not recreated, and every later repo untouched, with no summary.
- `TimestampedMillisHook` has zero production users and zero tests.

---

## Corrections to earlier statements in this file

Agents pushed back on four things during the documentation pass. All four checked out:

- **The `totalNs` concurrency double-count (B4) is Arango-only.** Karango runs EXPLAIN concurrently
  with the query (`async` + `awaitAll`); Monko runs it sequentially afterwards. The *nesting*
  double-count is Monko-only (`measureDeserializer` inside `measureQuery`). Neither backend has both.
- **`StoredSlumberer` on an unresolved `Ref` throws `IllegalStateException`, not `VaultException`**
  (`domain.kt:245-247`) — and a `Ref` never reaches `StoredSlumberer` through the codec anyway, since
  both declared-type and runtime-class dispatch land on `RefCodec`. Direct calls only.
- **`New<X>` does not fail to find an Awaker** — it falls through to `BuiltInModule`'s
  `DataClassAwaker`, which wants a nested `_value` key while `StoredSlumberer` writes a flat map. The
  round trip is broken, but by shape mismatch rather than by a missing codec.
- **"Returns null silently fails the enclosing object" is only true for a required non-nullable
  field.** A nullable field becomes `null`; an optional one falls back to its default.

## Verified CLEAN (do not re-tread)

- `Ref.resolve()` double-checked locking is correct; the non-reentrancy comment is accurate and no
  reachable self-deadlock exists. 100-concurrent-resolve test passes.
- `Repository.save(storable)` is not infinite recursion — the `is Stored` branch smart-casts to the
  more specific overload.
- All six `@Suppress("UNCHECKED_CAST")` in `domain.kt` are guarded by a preceding `is X`.
- `SharedRepoClassLookup` sentinel/`putIfAbsent` logic is correct, and its singleton-vs-dynamic
  scoping is sound — it caches only `KClass` values.
- `DeferredVaultHookScope` is a genuine singleton (zero constructor deps) — the kontainer
  semi-dynamic trap does not apply. Pinned by `VaultHookScopeWiringSpec.kt:39`.
- `TypedQuery.of` is type-sound; no path found where the declared return type diverges from what a
  backend yields.
- `RemoveResult.count` genuinely means *actually removed* on both backends (`RETURN OLD` in Karango,
  `deletedCount` in Monko) — an earlier suspicion that Karango always returned 0 did not survive.
- Batch-insert ordering IS preserved by both real implementations; only the KDoc was ambiguous.
- `VaultModels` serialization exposure is gated — the only endpoint is `VaultApi`, behind
  `authFloor = { isSuperUser() }`.
- `OrgMembersStorage`'s three filtering reads DO apply `notDeleted` on both backends.
- Graph-builder recursion terminates on cyclic and mutually-recursive type graphs.
- Clock injection for the timestamp hooks is sound; no stale clock captured at boot.
