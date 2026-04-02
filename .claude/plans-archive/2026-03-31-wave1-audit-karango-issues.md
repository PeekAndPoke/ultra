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

### H2: Cursor uses `runBlocking` inside iterator for batch pagination — DEFERRED

- **File:** `karango/core/src/main/kotlin/cursor.kt:59`
- **Category:** Implementation
- **Status:** DEFERRED — Requires redesigning the Cursor interface from synchronous Iterator to
  async Flow. The `runBlocking` is forced by the synchronous `Iterator.next()` signature. TODO in code.

### H3: Cursor iterator is not thread-safe despite claiming to handle concurrent access — ✅ FIXED

- **File:** `karango/core/src/main/kotlin/cursor.kt:35-87`
- **Category:** Implementation
- **Status:** FIXED — Misleading comment clarified. Cursors are single-threaded by design.

---

## MEDIUM

### M1: `KarangoCursor.close()` silently swallows all exceptions — ✅ FIXED

- **File:** `karango/core/src/main/kotlin/cursor.kt:101-106`
- **Category:** Implementation
- **Status:** FIXED — Empty `finally` replaced with `catch` block that logs to stderr.

### M2: `LIMIT` directly interpolates Int values into AQL — BY DESIGN

- **File:** `karango/core/src/main/kotlin/aql/limit.kt:6`
- **Category:** Security
- **Status:** BY DESIGN — Values are Kotlin `Int` parameters, not user input. Type system prevents injection.

### M3: Global mutable `dbCache` with stale connection risk — DEFERRED

- **File:** `karango/core/src/main/kotlin/index.kt:57-65`
- **Category:** Implementation
- **Status:** DEFERRED — Access is properly synchronized. Stale connection risk is real but depends on
  application usage patterns. TODO in code tracks the need for health checks.

### M4: `AqlQueryOptionProvider` discards `count(true)` when provider is null — ✅ FIXED

- **File:** `karango/core/src/main/kotlin/vault/KarangoDriver.kt:105-109`
- **Category:** Logic
- **Status:** FIXED — Changed to `optionsProvider?.invoke(it) ?: it` to preserve default options.

### M5: Query and bind variables exposed in error messages — ✅ FIXED

- **File:** `karango/core/src/main/kotlin/vault/KarangoDriver.kt:123-126`
- **Category:** Implementation
- **Status:** FIXED — Error message now only includes bind variable keys (not values).

### M6: KSP generated code uses wildcard imports and fully-qualified names — ✅ FIXED

- **File:** `karango/ksp/src/main/kotlin/KarangoKspProcessor.kt:107-110, 198-206`
- **Category:** Code Style
- **Status:** FIXED — Replaced wildcard imports with explicit imports (AqlExpression, AqlIterableExpr,
  AqlPropertyPath, L1-L5). Removed unused `io.peekandpoke.karango.*` import.

---

## LOW

### L1: Typo in log message: "crate" instead of "create" — ✅ FIXED

- **File:** `karango/core/src/main/kotlin/vault/EntityRepository.kt:194`
- **Category:** Code Style
- **Status:** FIXED — Already corrected in previous fix.

### L2: `version` property uses `runBlocking` in lazy initializer — DEFERRED

- **File:** `karango/core/src/main/kotlin/vault/KarangoDriver.kt:62-64`
- **Category:** Implementation
- **Status:** DEFERRED — Lazy init runs once on first access. Architectural limitation shared with
  other drivers. Fixing requires making `getDatabaseVersion()` a suspend function.

### L3: Wildcard import in hand-written code — ✅ FIXED

- **File:** `karango/core/src/main/kotlin/index.kt:19`
- **Category:** Code Style
- **Status:** FIXED — Replaced with `import java.util.Base64`.

### L4: Incomplete `AqlFunc` enum — appears to be dead code — WON'T FIX

- **File:** `karango/core/src/main/kotlin/aql/base_func.kt:8-43`
- **Category:** API Design
- **Status:** WON'T FIX — Dead code, never referenced. Actual implementations use `aqlFunc<>()` directly.

### L5: `PAGE()` does not validate negative `epp` values — ✅ FIXED

- **File:** `karango/core/src/main/kotlin/aql/for.kt:93-95`
- **Category:** API Design
- **Status:** FIXED — Added `require(epp > 0)` validation.

### L6: `save(Stored)` always does full replace via UPSERT_REPLACE — BY DESIGN

- **File:** `karango/core/src/main/kotlin/vault/EntityRepository.kt:280-281`
- **Category:** API Design
- **Status:** BY DESIGN — Intentional. `save()` = full replace, `modifyById()` exists for partial updates.

---

## Test Coverage Notes

Strong test coverage (145 tests). Gaps: cursor thread-safety scenarios, UPSERT with special characters in `_key`,
multi-index `ensureIndexes()`, error message content verification.
