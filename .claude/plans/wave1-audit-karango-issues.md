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

### C1: AQL injection via `_key` in UPSERT statement

- **File:** `karango/core/src/main/kotlin/aql/upsert.kt:66`
- **Category:** Security
- **Impact:** `_key` from `Storable` entity is directly interpolated into AQL:
  `p.append("UPSERT { _key: \"").append(entity._key).append("\" }")`. Not a bind variable. If `_key` contains a
  double-quote (from crafted `New` object), this breaks out of the string literal and allows arbitrary AQL injection.
  The `New` class allows arbitrary `_key` values via `EntityRepository.insert(key, new)`.
- **Fix:** Use bind variable: `p.append("UPSERT { _key: ").value("upsert_key", entity._key).append(" }")`.

### C2: `ensureIndexes()` aborts after first successful index

- **File:** `karango/core/src/main/kotlin/vault/EntityRepository.kt:161-214`
- **Category:** Logic
- **Impact:** The `return` statements inside `results.forEach` lambda (lines 174, 181, 188) cause the entire function to
  exit after processing the **first** index result. If a repository defines multiple indexes, only the first is created;
  the rest are silently skipped. Production deployments may be missing indexes.
- **Fix:** Replace `return` in each success branch with `Unit` or remove it. Only return from outer `while` loop after
  all results are processed.

---

## HIGH

### H1: `_key` string interpolation in `DOCUMENT()` function

- **File:** `karango/core/src/main/kotlin/aql/functions_document.kt:35`
- **Category:** Security → actually LOW risk
- **Impact:** `"${collection.name}/${id.ensureKey}".aql("id")` constructs a string that goes through `.aql("id")` which
  creates a bind variable. The bind variable protects against injection. `collection.name` is set at class definition
  time. Safe in practice but pattern is fragile.
- **Fix:** Document that `collection.name` must be trusted. Consider validating collection names contain only
  `[a-zA-Z0-9_-]`.

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

### M1: `KarangoCursor.close()` silently swallows all exceptions

- **File:** `karango/core/src/main/kotlin/cursor.kt:101-106`
- **Category:** Implementation
- **Impact:** Empty `finally` block hides resource leaks or driver errors.
- **Fix:** Log exceptions in catch block.

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

### M4: `AqlQueryOptionProvider` discards `count(true)` when provider is null

- **File:** `karango/core/src/main/kotlin/vault/KarangoDriver.kt:105-109`
- **Category:** Logic
- **Impact:** When `optionsProvider` is null, `let` returns null, discarding `count(true)`. Cursor count will be null/0.
- **Fix:** `optionsProvider?.invoke(opts) ?: opts`.

### M5: Query and bind variables exposed in error messages

- **File:** `karango/core/src/main/kotlin/vault/KarangoDriver.kt:123-126`
- **Category:** Implementation
- **Impact:** `KarangoQueryException` includes full AQL query and bind variable values. If exposed to end users, leaks
  DB structure and data.
- **Fix:** Sanitize exception message. Keep details for server-side logging only.

### M6: KSP generated code uses wildcard imports and fully-qualified names

- **File:** `karango/ksp/src/main/kotlin/KarangoKspProcessor.kt:107-110, 198-206`
- **Category:** Code Style
- **Impact:** Generated code uses `import io.peekandpoke.karango.*` and fully-qualified class names like
  `io.peekandpoke.karango.testdomain.TestPersonDetails`. Violates project conventions.
- **Fix:** Generate explicit imports, use short names.

---

## LOW

### L1: Typo in log message: "crate" instead of "create"

- **File:** `karango/core/src/main/kotlin/vault/EntityRepository.kt:194`
- **Category:** Code Style
- **Fix:** Change `"crate"` to `"create"`.

### L2: `version` property uses `runBlocking` in lazy initializer

- **File:** `karango/core/src/main/kotlin/vault/KarangoDriver.kt:62-64`
- **Category:** Implementation
- **Impact:** First access blocks coroutine's thread. Same pattern as Monko driver.
- **Fix:** Make `getDatabaseVersion()` a suspend function, or document blocking behavior.

### L3: Wildcard import in hand-written code

- **File:** `karango/core/src/main/kotlin/index.kt:19`
- **Category:** Code Style
- **Fix:** Replace `import java.util.*` with `import java.util.Base64`.

### L4: Incomplete `AqlFunc` enum — appears to be dead code

- **File:** `karango/core/src/main/kotlin/aql/base_func.kt:8-43`
- **Category:** API Design
- **Impact:** Many entries with TODOs, several declared but not implemented. Actual implementations use `aqlFunc<>()`
  directly.
- **Fix:** Complete or remove as dead code.

### L5: `PAGE()` does not validate negative `epp` values

- **File:** `karango/core/src/main/kotlin/aql/for.kt:93-95`
- **Category:** API Design
- **Fix:** Add `require(epp > 0)`.

### L6: `save(Stored)` always does full replace via UPSERT_REPLACE

- **File:** `karango/core/src/main/kotlin/vault/EntityRepository.kt:280-281`
- **Category:** API Design
- **Impact:** May surprise users expecting partial update. Design choice, not a bug.
- **Fix:** Document behavior, or add separate `update()` for partial updates.

---

## Test Coverage Notes

Strong test coverage (145 tests). Gaps: cursor thread-safety scenarios, UPSERT with special characters in `_key`,
multi-index `ensureIndexes()`, error message content verification.
