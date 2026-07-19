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

### M1: Prototype instances list grows unboundedly — memory leak — BY DESIGN

- **File:** `ultra/kontainer/src/main/kotlin/ServiceProvider.kt:226-227`
- **Category:** Implementation
- **Status:** BY DESIGN — Intentional instrumentation for KontainerTools debugging. Instance tracking
  is a feature, not a leak. Each blueprint's kontainer instances have separate provider pools.

### M2: `KontainerException` extends `Throwable` instead of `Exception` — ✅ FIXED

- **File:** `ultra/kontainer/src/main/kotlin/exception.kt:4`
- **Category:** Security / API Design
- **Status:** FIXED — Changed to extend `RuntimeException`.

### M3: `ForSingleton.provide()` double-checked locking uses arbitrary first caller's context — ✅ FIXED

- **File:** `ultra/kontainer/src/main/kotlin/ServiceProvider.kt:167-173`
- **Category:** Logic
- **Status:** FIXED — Added `@Volatile` to `instance` field for correct double-checked locking. Context
  issue is mitigated by the semi-dynamic promotion mechanism.

### M4: `KontainerBlueprint.extend()` discards config of extension builder — BY DESIGN / WON'T FIX

- **File:** `ultra/kontainer/src/main/kotlin/KontainerBlueprint.kt:119-127`
- **Category:** Logic
- **Status:** BY DESIGN / WON'T FIX — Built-in definitions are re-registered but behave identically. Singletons are
  intentionally NOT shared across blueprint inheritance because extended blueprints may override
  dependencies that singletons inject, which would cause race conditions on first resolution.
  DO NOT change this behavior.

### M5: `ServiceProducer.forClass` crashes with NPE on classes without primary constructor — ✅ FIXED

- **File:** `ultra/kontainer/src/main/kotlin/ServiceProducer.kt:54`
- **Category:** Implementation
- **Status:** FIXED — Replaced `!!` with explicit `?: throw InvalidClassProvided(...)` with descriptive message.

### M6: `getName()` is `internal` but used from test code — BY DESIGN

- **File:** `ultra/kontainer/src/main/kotlin/index.kt:32`
- **Category:** Code Style
- **Status:** BY DESIGN — Tests are in the same module, so `internal` access is correct and appropriate.

---

## LOW

### L1: `helpers.kt` uses `!!` on type arguments without null guard — WON'T FIX

- **File:** `ultra/kontainer/src/main/kotlin/helpers.kt:44,49-50,55-56`
- **Category:** Implementation
- **Status:** WON'T FIX — Callers always validate the type before calling these helpers.
  Star projections never reach these code paths in practice.

### L2: `ForPrototype.provide()` synchronizes on every call — bottleneck — WON'T FIX

- **File:** `ultra/kontainer/src/main/kotlin/ServiceProvider.kt:232`
- **Category:** Implementation
- **Status:** WON'T FIX — Lock is needed for instance tracking (BY DESIGN for KontainerTools).

### L3: `ServiceDefinition.CodeLocation` captures stack trace on every definition — WON'T FIX

- **File:** `ultra/kontainer/src/main/kotlin/ServiceDefinition.kt:27-31`
- **Category:** API Design
- **Status:** WON'T FIX — Runs once at blueprint construction time; negligible overhead.

### L4: `DependencyLookup.getAllDependents` could use BFS — WON'T FIX

- **File:** `ultra/kontainer/src/main/kotlin/DependencyLookup.kt:70-87`
- **Category:** Logic
- **Status:** WON'T FIX — Current implementation is correct. Readability is subjective.

### L5: `data class` on `ForSingleton` / `ForPrototype` with mutable state — WON'T FIX

- **File:** `ultra/kontainer/src/main/kotlin/ServiceProvider.kt:152,207`
- **Category:** Code Style
- **Status:** WON'T FIX — equals/hashCode/copy are never called. data class is used for toString() only.

### L6: Zero-parameter factory uses `(Any?) -> IMPL` with meaningless `Unit` param — WON'T FIX

- **File:** `ultra/kontainer/src/main/kotlin/KontainerBuilder.kt:98-109`
- **Category:** API Design
- **Status:** WON'T FIX — Source-breaking change; not worth it.

### L7: `@KontainerDsl` annotation has no scoping effect — WON'T FIX

- **File:** `ultra/kontainer/src/main/kotlin/index.kt:10-11`
- **Category:** Code Style
- **Status:** WON'T FIX — Cosmetic; no runtime impact.

### L8: `KontainerDslSingleton` / `KontainerDslDynamic` / `KontainerDslPrototype` markers not on builder — WON'T FIX

- **Category:** Code Style
- **Status:** WON'T FIX — Same as L7.

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
