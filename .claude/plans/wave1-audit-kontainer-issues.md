# Audit: Kontainer — Issues Report

**Date:** 2026-03-30
**Auditor:** Code audit agent (Wave 1)
**Module:** ultra/kontainer (~69 source files, 20 test files)

---

## Summary

| Category       | CRITICAL | HIGH  | MEDIUM | LOW   |
|----------------|----------|-------|--------|-------|
| Logic          | 0        | 1     | 2      | 1     |
| Implementation | 0        | 2     | 2      | 2     |
| Security       | 0        | 0     | 1      | 0     |
| Code Style     | 0        | 0     | 1      | 3     |
| API Design     | 0        | 0     | 0      | 2     |
| **Total**      | **0**    | **3** | **6**  | **8** |

---

## HIGH

### H1: Race condition on `KontainerBlueprint.usages` counter — ✅ FIXED

- **File:** `ultra/kontainer/src/main/kotlin/KontainerBlueprint.kt:30-31,139`
- **Category:** Implementation
- **Status:** FIXED — Replaced `var usages` with `AtomicBoolean(false)` and `compareAndSet(false, true)`.

### H2: TypeLookup caches are non-thread-safe mutable maps — ✅ FIXED

- **File:** `ultra/kontainer/src/main/kotlin/TypeLookup.kt:48,71,76`
- **Category:** Implementation
- **Status:** FIXED — All three caches replaced with `ConcurrentHashMap`.

### H3: No circular dependency detection — ✅ FIXED

- **File:** `ultra/kontainer/src/main/kotlin/DependencyLookup.kt`
- **Category:** Logic
- **Status:** FIXED — Added DFS-based cycle detection in `DependencyLookup.detectCircularDependencies()`.
  Integrated into blueprint validation. Lazy injections excluded (they break cycles by design).
  Error message names the cycle and suggests using `Lazy<T>`.

---

## MEDIUM

### M1: Prototype instances list grows unboundedly — memory leak

- **File:** `ultra/kontainer/src/main/kotlin/ServiceProvider.kt:226-227`
- **Category:** Implementation
- **Impact:** `ForPrototype.instances` appends every created instance forever. Strong references retained for lifetime
  of kontainer. Memory leak for long-lived applications.
- **Fix:** Don't track prototype instances, or make tracking opt-in/debug-only.

### M2: `KontainerException` extends `Throwable` instead of `Exception` — ✅ FIXED

- **File:** `ultra/kontainer/src/main/kotlin/exception.kt:4`
- **Category:** Security / API Design
- **Status:** FIXED — Changed to extend `RuntimeException`.

### M3: `ForSingleton.provide()` double-checked locking uses arbitrary first caller's context — ✅ FIXED

- **File:** `ultra/kontainer/src/main/kotlin/ServiceProvider.kt:167-173`
- **Category:** Logic
- **Status:** FIXED — Added `@Volatile` to `instance` field for correct double-checked locking. Context
  issue is mitigated by the semi-dynamic promotion mechanism.

### M4: `KontainerBlueprint.extend()` discards config of extension builder

- **File:** `ultra/kontainer/src/main/kotlin/KontainerBlueprint.kt:119-127`
- **Category:** Logic
- **Impact:** Extension builder re-adds built-in definitions that overwrite originals in merge. Harmless in practice but
  confusing.
- **Fix:** Use lighter-weight mechanism or filter built-in services from extension.

### M5: `ServiceProducer.forClass` crashes with NPE on classes without primary constructor — ✅ FIXED

- **File:** `ultra/kontainer/src/main/kotlin/ServiceProducer.kt:54`
- **Category:** Implementation
- **Status:** FIXED — Replaced `!!` with explicit `?: throw InvalidClassProvided(...)` with descriptive message.

### M6: `getName()` is `internal` but used from test code

- **File:** `ultra/kontainer/src/main/kotlin/index.kt:32`
- **Category:** Code Style
- **Impact:** Tests import internal function. Works but signals it should be public.
- **Fix:** Make public or provide public equivalent.

---

## LOW

### L1: `helpers.kt` uses `!!` on type arguments without null guard

- **File:** `ultra/kontainer/src/main/kotlin/helpers.kt:44,49-50,55-56`
- **Category:** Implementation
- **Impact:** Star projections (e.g., `Lazy<*>`) cause unhelpful NPE.
- **Fix:** Add guard with clear error message.

### L2: `ForPrototype.provide()` synchronizes on every call — bottleneck

- **File:** `ultra/kontainer/src/main/kotlin/ServiceProvider.kt:232`
- **Category:** Implementation
- **Impact:** Serializes all concurrent prototype requests. Lock only needed for instance tracking (itself a leak).
- **Fix:** Remove sync if instance tracking is removed.

### L3: `ServiceDefinition.CodeLocation` captures stack trace on every definition

- **File:** `ultra/kontainer/src/main/kotlin/ServiceDefinition.kt:27-31`
- **Category:** API Design
- **Impact:** `Throwable()` for stack trace on every service definition. Overhead during blueprint construction.
- **Fix:** Make opt-in or debug-only.

### L4: `DependencyLookup.getAllDependents` could use BFS instead of iterative set growth

- **File:** `ultra/kontainer/src/main/kotlin/DependencyLookup.kt:70-87`
- **Category:** Logic
- **Impact:** Current implementation is correct but unclear. BFS would be more readable.
- **Fix:** Replace with standard BFS.

### L5: `data class` on `ForSingleton` / `ForPrototype` with mutable state

- **File:** `ultra/kontainer/src/main/kotlin/ServiceProvider.kt:152,207`
- **Category:** Code Style
- **Impact:** Mutable state in data class. equals/hashCode ignores it. Potentially confusing.
- **Fix:** Consider regular class.

### L6: Zero-parameter factory uses `(Any?) -> IMPL` with meaningless `Unit` param

- **File:** `ultra/kontainer/src/main/kotlin/KontainerBuilder.kt:98-109`
- **Category:** API Design
- **Impact:** `Any?` parameter always receives `Unit`. Source-breaking to fix post-v1.
- **Fix:** Use `() -> IMPL` before v1.0.0.

### L7: `@KontainerDsl` annotation has no scoping effect

- **File:** `ultra/kontainer/src/main/kotlin/index.kt:10-11`
- **Category:** Code Style
- **Impact:** DslMarker annotation exists but is not applied to `KontainerBuilder`, so it has no effect.
- **Fix:** Apply to `KontainerBuilder` or remove.

### L8: `KontainerDslSingleton` / `KontainerDslDynamic` / `KontainerDslPrototype` markers not on builder

- **Category:** Code Style
- **Impact:** Same as L7 — no scoping effect.

---

## Notable Positives

- Thread-safe singleton initialization (proper double-checked locking)
- Well-designed injection type upgrade system (thoroughly tested)
- Semi-dynamic promotion is correct
- Comprehensive test suite (17 files)
- `InjectionContext` caching uses `ConcurrentHashMap` correctly
- `DynamicsChecker` validates overrides with `isAssignableFrom`

---

## Test Coverage Gaps

No tests for: circular dependency error messages, star projection handling, extension blueprint config propagation,
classes without primary constructor.
