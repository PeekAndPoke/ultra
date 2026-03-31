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

### C1: `DefaultEntityCache.getOrPut` is not thread-safe — ✅ FIXED

- **File:** `ultra/vault/src/jvmMain/kotlin/caching.kt:80-91`
- **Category:** Logic
- **Status:** FIXED — Entire `getOrPut` body now wrapped in `synchronized(lock)`.

### C2: `VaultScope.launch` uses `runBlocking` — ✅ FIXED

- **File:** `ultra/vault/src/jvmMain/kotlin/vault_module.kt:62-73`
- **Category:** Implementation
- **Status:** FIXED — Now uses `scope.launch { block() }` instead of `runBlocking`.

---

## HIGH

### H1: `DefaultEntityCache.getOrPut` unconditionally calls `put` even for null — ✅ FIXED

- **File:** `ultra/vault/src/jvmMain/kotlin/caching.kt:88-90`
- **Category:** Logic
- **Status:** FIXED — Now checks `if (value != null)` before storing.

### H2: `SharedRepoClassLookup` uses unsynchronized `mutableMapOf` — ✅ FIXED

- **File:** `ultra/vault/src/jvmMain/kotlin/caching.kt:94-105`
- **Category:** Implementation
- **Status:** FIXED — Both maps now use `ConcurrentHashMap`.

### H3: `DefaultQueryProfiler.entries` is a mutable `var List` with no synchronization — ✅ FIXED

- **File:** `ultra/vault/src/jvmMain/kotlin/profiling/DefaultQueryProfiler.kt:6`
- **Category:** Implementation
- **Status:** FIXED — Added `synchronized(lock)` around entries mutation. Made setter `private`.

### H4: `LazyRefCodec.awake` force-unwraps nullable cache result with `!!` — ✅ FIXED

- **File:** `ultra/vault/src/jvmMain/kotlin/slumber/LazyRefCodec.kt:41`
- **Category:** API Design
- **Status:** FIXED — Replaced `!!` with `?: throw VaultException("Referenced entity not found: $id")`.

---

## MEDIUM

### M1: `StoredSlumberer.slumber` performs unchecked cast to `Map<String, Any?>` — ✅ FIXED

- **File:** `ultra/vault/src/jvmMain/kotlin/slumber/StoredSlumberer.kt:23-24`
- **Category:** Logic
- **Status:** FIXED — Added explicit `is Map<*, *>` check with descriptive `VaultException`.

### M2: `DatabaseGraphBuilder` sets `connection = ""` instead of `repo.connection`

- **File:** `ultra/vault/src/jvmMain/kotlin/tools/DatabaseGraphBuilder.kt:165`
- **Category:** Implementation
- **Impact:** Graph model's `Repo.connection` field is always blank.
- **Fix:** Replace `connection = ""` with `connection = repo.connection`.

### M3: `Repository.stores()` unsafe cast of `type` to `KClass<*>` — ✅ FIXED

- **File:** `ultra/vault/src/jvmMain/kotlin/Repository.kt:181`
- **Category:** Implementation
- **Status:** FIXED — Uses safe cast `(type as? KClass<*>)?.supertypes?.any {...}`.

### M4: `RefCodec` and `LazyRefCodec` use `runBlocking` inside deserialization

- **File:** `ultra/vault/src/jvmMain/kotlin/slumber/RefCodec.kt:35`, `LazyRefCodec.kt:38`
- **Category:** Implementation
- **Impact:** Can deadlock with limited thread pools. Exceptions may leak DB details.
- **Fix:** Wrap in try-catch mapping to `VaultException`. Long-term: make codec system async-aware.

### M5: `Storable.hasIdIn` creates intermediate list allocation — ✅ FIXED

- **File:** `ultra/vault/src/jvmMain/kotlin/domain.kt:82`
- **Category:** API Design
- **Status:** FIXED — Replaced with `others.any { it._id == _id }`.

### M6: `VaultSlumberModule.getAwaker` NPE on star projection — ✅ FIXED

- **File:** `ultra/vault/src/jvmMain/kotlin/slumber/VaultSlumberModule.kt`
- **Category:** Implementation
- **Status:** FIXED — Uses safe navigation `type.arguments.firstOrNull()?.type?.let {...}` instead of `!!`.

---

## LOW

### L1: Wildcard import in `annotations.kt` — ✅ FIXED

- **File:** `ultra/vault/src/commonMain/kotlin/annotations.kt:4`
- **Category:** Code Style
- **Status:** FIXED — Uses explicit imports.

### L2: `Database` methods use `error()` instead of `VaultException` — ✅ FIXED

- **File:** `ultra/vault/src/jvmMain/kotlin/Database.kt:66,102`
- **Category:** Code Style
- **Status:** FIXED — Replaced with `throw VaultException(...)`. TODOs removed.

### L3: `LazyRef` is a `data class` with lambda in equals/hashCode

- **File:** `ultra/vault/src/jvmMain/kotlin/domain.kt:173-175`
- **Category:** API Design
- **Impact:** Two LazyRefs with same `_id` but different providers are not equal. Counter-intuitive.
- **Fix:** Override equals/hashCode to use only `_id`, or make it a regular class.

---

## Test Coverage Gaps

No tests for: `RefCodec`/`LazyRefCodec`, `StoredAwaker`/`StoredSlumberer`, `DatabaseGraphBuilder`, `Repository.Hooks`,
`VaultScope`, `SharedRepoClassLookup`.
