# Audit: ultra:vault — Issues Report

**Date:** 2026-03-30
**Auditor:** Code audit agent (Wave 1)
**Module:** ultra/vault (~45 source files, 8 test files)

---

## Summary

| Category       | CRITICAL | HIGH  | MEDIUM | LOW   |
|----------------|----------|-------|--------|-------|
| Logic          | 1        | 1     | 1      | 0     |
| Implementation | 1        | 2     | 3      | 0     |
| Security       | 0        | 0     | 0      | 0     |
| Code Style     | 0        | 0     | 0      | 2     |
| API Design     | 0        | 1     | 2      | 1     |
| **Total**      | **2**    | **4** | **6**  | **3** |

---

## CRITICAL

### C1: `DefaultEntityCache.getOrPut` is not thread-safe — can return wrong values

- **File:** `ultra/vault/src/jvmMain/kotlin/caching.kt:80-91`
- **Category:** Logic
- **Impact:** Reads from `entries` without holding the lock, then acquires lock inside `put()`. Two threads can race:
  both see the key missing, both call the provider, both store results. Worse, the initial `entries[id]` read outside
  the lock can see partially-constructed state from a concurrent `put()` (HashMap is not thread-safe for concurrent
  read/write). Can cause `ClassCastException`, map corruption, or wrong entities returned.
- **Fix:** Wrap the entire `getOrPut` body in `synchronized(lock)`, or use `ConcurrentHashMap.computeIfAbsent`.

### C2: `VaultScope.launch` uses `runBlocking` (already tracked)

- **File:** `ultra/vault/src/jvmMain/kotlin/vault_module.kt:62-73`
- **Category:** Implementation
- **Impact:** After-save/delete hooks execute synchronously on caller's thread. Already tracked in funktor-issues.md.
- **Fix:** Replace with `CoroutineScope(SupervisorJob() + Dispatchers.IO).launch`.

---

## HIGH

### H1: `DefaultEntityCache.getOrPut` unconditionally calls `put` even for null provider results

- **File:** `ultra/vault/src/jvmMain/kotlin/caching.kt:88-90`
- **Category:** Logic
- **Impact:** Null results are stored but never served (null check fails on next access). If T is non-nullable, a cached
  null will produce a null value for a non-nullable type, causing downstream NPEs.
- **Fix:** Only call `put` when provider result is non-null.

### H2: `SharedRepoClassLookup` uses unsynchronized `mutableMapOf`

- **File:** `ultra/vault/src/jvmMain/kotlin/caching.kt:94-105`
- **Category:** Implementation
- **Impact:** Registered as singleton in Kontainer, shared across all requests. Both `typeLookup` and `nameLookup` are
  plain `HashMap` with `getOrPut` and no synchronization. Concurrent access can corrupt the map or throw
  `ConcurrentModificationException`.
- **Fix:** Use `ConcurrentHashMap`.

### H3: `DefaultQueryProfiler.entries` is a mutable `var List` with no synchronization

- **File:** `ultra/vault/src/jvmMain/kotlin/profiling/DefaultQueryProfiler.kt:6`
- **Category:** Implementation
- **Impact:** Re-assigned via `entries = entries.plus(entry)`. Two concurrent calls cause lost-update race — profiling
  entries silently dropped.
- **Fix:** Use `mutableListOf` with synchronized access, or an atomic reference.

### H4: `LazyRefCodec.awake` force-unwraps nullable cache result with `!!`

- **File:** `ultra/vault/src/jvmMain/kotlin/slumber/LazyRefCodec.kt:41`
- **Category:** API Design
- **Impact:** If entity was deleted, `cache.getOrPut` returns null, and `!!` throws NPE with no context. Dangling
  references are normal in eventually-consistent systems.
- **Fix:** Throw `VaultException("Referenced entity not found: $id")` or handle null gracefully.

---

## MEDIUM

### M1: `StoredSlumberer.slumber` performs unchecked cast to `Map<String, Any?>`

- **File:** `ultra/vault/src/jvmMain/kotlin/slumber/StoredSlumberer.kt:23-24`
- **Category:** Logic
- **Impact:** If `context.slumber(data.value)` returns non-Map, throws ClassCastException with no context.
- **Fix:** Add type check and throw descriptive `VaultException`.

### M2: `DatabaseGraphBuilder` sets `connection = ""` instead of `repo.connection`

- **File:** `ultra/vault/src/jvmMain/kotlin/tools/DatabaseGraphBuilder.kt:165`
- **Category:** Implementation
- **Impact:** Graph model's `Repo.connection` field is always blank.
- **Fix:** Replace `connection = ""` with `connection = repo.connection`.

### M3: `Repository.stores()` unsafe cast of `type` to `KClass<*>`

- **File:** `ultra/vault/src/jvmMain/kotlin/Repository.kt:181`
- **Category:** Implementation
- **Impact:** If `type` is `KTypeParameter`, cast throws ClassCastException.
- **Fix:** Use safe cast: `(type as? KClass<*>)?.supertypes...`.

### M4: `RefCodec` and `LazyRefCodec` use `runBlocking` inside deserialization

- **File:** `ultra/vault/src/jvmMain/kotlin/slumber/RefCodec.kt:35`, `LazyRefCodec.kt:38`
- **Category:** Implementation
- **Impact:** Can deadlock with limited thread pools. Exceptions may leak DB details.
- **Fix:** Wrap in try-catch mapping to `VaultException`. Long-term: make codec system async-aware.

### M5: `Storable.hasIdIn` creates intermediate list allocation

- **File:** `ultra/vault/src/jvmMain/kotlin/domain.kt:82`
- **Category:** API Design
- **Impact:** `others.map { it._id }` allocates O(n) list on every call.
- **Fix:** Use `others.any { it._id == this._id }`.

### M6: `VaultSlumberModule.getAwaker` NPE on star projection

- **File:** `ultra/vault/src/jvmMain/kotlin/slumber/` (getAwaker method)
- **Category:** Implementation
- **Impact:** `type.arguments[0].type!!` will NPE if type argument is `Stored<*>`.
- **Fix:** Handle star projection case explicitly.

---

## LOW

### L1: Wildcard import in `annotations.kt`

- **File:** `ultra/vault/src/commonMain/kotlin/annotations.kt:4`
- **Category:** Code Style
- **Fix:** Replace `import kotlin.annotation.AnnotationTarget.*` with explicit imports.

### L2: `Database` methods use `error()` instead of `VaultException`

- **File:** `ultra/vault/src/jvmMain/kotlin/Database.kt:66,102`
- **Category:** Code Style
- **Impact:** Inconsistent exception types. TODO comments acknowledge this.
- **Fix:** Replace `error(...)` with `throw VaultException(...)`.

### L3: `LazyRef` is a `data class` with lambda in equals/hashCode

- **File:** `ultra/vault/src/jvmMain/kotlin/domain.kt:173-175`
- **Category:** API Design
- **Impact:** Two LazyRefs with same `_id` but different providers are not equal. Counter-intuitive.
- **Fix:** Override equals/hashCode to use only `_id`, or make it a regular class.

---

## Test Coverage Gaps

No tests for: `RefCodec`/`LazyRefCodec`, `StoredAwaker`/`StoredSlumberer`, `DatabaseGraphBuilder`, `Repository.Hooks`,
`VaultScope`, `SharedRepoClassLookup`.
