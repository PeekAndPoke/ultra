# Audit: Karango — Issues Report

**Date:** 2026-03-30
**Auditor:** Code audit agent (Wave 1)
**Module:** karango/core (~183 source files, 145 test files) + karango/ksp

---

## Summary

| Category       | CRITICAL | HIGH  | MEDIUM | LOW   |
|----------------|----------|-------|--------|-------|
| Logic          | 1        | 1     | 1      | 1     |
| Implementation | 0        | 2     | 2      | 1     |
| Security       | 1        | 0     | 1      | 0     |
| Code Style     | 0        | 0     | 2      | 2     |
| API Design     | 0        | 0     | 0      | 2     |
| **Total**      | **2**    | **3** | **6**  | **6** |

---

## CRITICAL

### C1: AQL injection via `_key` in UPSERT statement — ✅ FIXED

- **File:** `karango/core/src/main/kotlin/aql/upsert.kt:66`
- **Category:** Security
- **Status:** FIXED — Now uses `.value("upsert_key", entity._key)` bind variable.

### C2: `ensureIndexes()` aborts after first successful index — ✅ FIXED

- **File:** `karango/core/src/main/kotlin/vault/EntityRepository.kt:161-214`
- **Category:** Logic
- **Status:** FIXED — `return` statements removed from forEach lambda; all indexes are now processed.

---

## HIGH

### H1: `_key` string interpolation in `DOCUMENT()` function — ✅ FIXED

- **File:** `karango/core/src/main/kotlin/aql/functions_document.kt:35`
- **Category:** Security → actually LOW risk
- **Status:** FIXED — Uses `.aql("id")` bind variable. Safe in practice.

### H2: Cursor uses `runBlocking` inside iterator for batch pagination

- **File:** `karango/core/src/main/kotlin/cursor.kt:59`
- **Category:** Implementation
- **Impact:** `next()` calls `runBlocking { inner = inner.nextBatch().await() }` from synchronous `Iterator.next()`.
  Blocks calling coroutine's thread during batch fetch. Thread starvation risk under load. TODO acknowledges this.
- **Fix:** Redesign cursor to use `Flow<T>` or `ReceiveChannel<T>`.

### H3: Cursor iterator is not thread-safe despite claiming to handle concurrent access

- **File:** `karango/core/src/main/kotlin/cursor.kt:35-87`
- **Category:** Implementation
- **Impact:** Comment on line 45 mentions thread safety, but `idx` is not volatile/atomic, `data` can be reassigned
  concurrently, `inner` is mutated without synchronization. Two threads calling `next()` simultaneously can
  skip/double-read elements.
- **Fix:** Document as not thread-safe (remove misleading comment), or use proper synchronization.

---

## MEDIUM

### M1: `KarangoCursor.close()` silently swallows all exceptions — ✅ FIXED

- **File:** `karango/core/src/main/kotlin/cursor.kt:101-106`
- **Category:** Implementation
- **Status:** FIXED — Empty `finally` replaced with `catch` block that logs to stderr.

### M2: `LIMIT` directly interpolates Int values into AQL

- **File:** `karango/core/src/main/kotlin/aql/limit.kt:6`
- **Category:** Security
- **Impact:** `"LIMIT $offset, $limit"` — safe for Int types but when `limit` is null (from `SKIP`), literal `"null"` is
  interpolated. Depends on undocumented ArangoDB behavior.
- **Fix:** Use bind variables, or validate `limit != null`.

### M3: Global mutable `dbCache` with stale connection risk

- **File:** `karango/core/src/main/kotlin/index.kt:57-65`
- **Category:** Implementation
- **Impact:** Global `mutableMapOf` never cleans up closed connections. Can return dead connections. TODO acknowledges
  this.
- **Fix:** Add eviction mechanism and connection health checks.

### M4: `AqlQueryOptionProvider` discards `count(true)` when provider is null — ✅ FIXED

- **File:** `karango/core/src/main/kotlin/vault/KarangoDriver.kt:105-109`
- **Category:** Logic
- **Status:** FIXED — Changed to `optionsProvider?.invoke(it) ?: it` to preserve default options.

### M5: Query and bind variables exposed in error messages — ✅ FIXED

- **File:** `karango/core/src/main/kotlin/vault/KarangoDriver.kt:123-126`
- **Category:** Implementation
- **Status:** FIXED — Error message now only includes bind variable keys (not values).

### M6: KSP generated code uses wildcard imports and fully-qualified names

- **File:** `karango/ksp/src/main/kotlin/KarangoKspProcessor.kt:107-110, 198-206`
- **Category:** Code Style
- **Impact:** Generated code uses `import io.peekandpoke.karango.*` and fully-qualified class names like
  `io.peekandpoke.karango.testdomain.TestPersonDetails`. Violates project conventions.
- **Fix:** Generate explicit imports, use short names.

---

## LOW

### L1: Typo in log message: "crate" instead of "create" — ✅ FIXED

- **File:** `karango/core/src/main/kotlin/vault/EntityRepository.kt:194`
- **Category:** Code Style
- **Status:** FIXED — Already corrected in previous fix.

### L2: `version` property uses `runBlocking` in lazy initializer

- **File:** `karango/core/src/main/kotlin/vault/KarangoDriver.kt:62-64`
- **Category:** Implementation
- **Impact:** First access blocks coroutine's thread. Same pattern as Monko driver.
- **Fix:** Make `getDatabaseVersion()` a suspend function, or document blocking behavior.

### L3: Wildcard import in hand-written code — ✅ FIXED

- **File:** `karango/core/src/main/kotlin/index.kt:19`
- **Category:** Code Style
- **Status:** FIXED — Replaced with `import java.util.Base64`.

### L4: Incomplete `AqlFunc` enum — appears to be dead code

- **File:** `karango/core/src/main/kotlin/aql/base_func.kt:8-43`
- **Category:** API Design
- **Impact:** Many entries with TODOs, several declared but not implemented. Actual implementations use `aqlFunc<>()`
  directly.
- **Fix:** Complete or remove as dead code.

### L5: `PAGE()` does not validate negative `epp` values — ✅ FIXED

- **File:** `karango/core/src/main/kotlin/aql/for.kt:93-95`
- **Category:** API Design
- **Status:** FIXED — Added `require(epp > 0)` validation.

### L6: `save(Stored)` always does full replace via UPSERT_REPLACE

- **File:** `karango/core/src/main/kotlin/vault/EntityRepository.kt:280-281`
- **Category:** API Design
- **Impact:** May surprise users expecting partial update. Design choice, not a bug.
- **Fix:** Document behavior, or add separate `update()` for partial updates.

---

## Test Coverage Notes

Strong test coverage (145 tests). Gaps: cursor thread-safety scenarios, UPSERT with special characters in `_key`,
multi-index `ensureIndexes()`, error message content verification.
