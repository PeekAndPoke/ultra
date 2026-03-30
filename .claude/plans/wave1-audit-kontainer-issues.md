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

### H1: Race condition on `KontainerBlueprint.usages` counter

- **File:** `ultra/kontainer/src/main/kotlin/KontainerBlueprint.kt:30-31,139`
- **Category:** Implementation
- **Impact:** `usages++` is non-atomic. Two concurrent `create()` calls can both read 0, skip validation entirely,
  creating kontainer instances with inconsistent configurations.
- **Fix:** Use `AtomicInteger` and `compareAndSet(0, 1)` to ensure exactly one thread validates.

### H2: TypeLookup caches are non-thread-safe mutable maps

- **File:** `ultra/kontainer/src/main/kotlin/TypeLookup.kt:48,71,76`
- **Category:** Implementation
- **Impact:** `ForBaseTypes.cache` and `ForSuperTypes.candidatesCache`/`lookupBlueprintCache` are plain HashMap.
  Concurrent read/write can cause infinite loops (rehashing), lost entries, or `ConcurrentModificationException`. Shared
  across all kontainer instances from same blueprint.
- **Fix:** Replace with `ConcurrentHashMap` (already used correctly elsewhere in the module).

### H3: No circular dependency detection

- **File:** `ultra/kontainer/src/main/kotlin/DependencyLookup.kt` (entire file)
- **Category:** Logic
- **Impact:** No check for circular dependencies. If A depends on B and B depends on A (both non-lazy),
  `StackOverflowError` at resolution time instead of a clear diagnostic. `DependencyLookup` builds a dependency graph
  but never checks for cycles.
- **Fix:** Add cycle detection (topological sort or DFS with visited-set), surface `KontainerInconsistent` error naming
  the cycle. Lazy injection already breaks cycles by design.

---

## MEDIUM

### M1: Prototype instances list grows unboundedly — memory leak

- **File:** `ultra/kontainer/src/main/kotlin/ServiceProvider.kt:226-227`
- **Category:** Implementation
- **Impact:** `ForPrototype.instances` appends every created instance forever. Strong references retained for lifetime
  of kontainer. Memory leak for long-lived applications.
- **Fix:** Don't track prototype instances, or make tracking opt-in/debug-only.

### M2: `KontainerException` extends `Throwable` instead of `Exception`

- **File:** `ultra/kontainer/src/main/kotlin/exception.kt:4`
- **Category:** Security / API Design
- **Impact:** `catch (e: Exception)` will not catch Kontainer errors. Surprising to users. Can cause uncaught errors in
  applications with top-level exception handlers. Binary-breaking change if fixed post-v1.
- **Fix:** Change to extend `Exception` or `RuntimeException`. Must do before v1.0.0.

### M3: `ForSingleton.provide()` double-checked locking uses arbitrary first caller's context

- **File:** `ultra/kontainer/src/main/kotlin/ServiceProvider.kt:167-173`
- **Category:** Logic
- **Impact:** First caller's `InjectionContext` is baked into singleton. Mitigated by semi-dynamic promotion mechanism,
  but could yield non-deterministic behavior if promotion is missed.
- **Fix:** Add assertion/warning if true global singleton attempts to inject `InjectionContext`.

### M4: `KontainerBlueprint.extend()` discards config of extension builder

- **File:** `ultra/kontainer/src/main/kotlin/KontainerBlueprint.kt:119-127`
- **Category:** Logic
- **Impact:** Extension builder re-adds built-in definitions that overwrite originals in merge. Harmless in practice but
  confusing.
- **Fix:** Use lighter-weight mechanism or filter built-in services from extension.

### M5: `ServiceProducer.forClass` crashes with NPE on classes without primary constructor

- **File:** `ultra/kontainer/src/main/kotlin/ServiceProducer.kt:54`
- **Category:** Implementation
- **Impact:** `cls.primaryConstructor!!` throws generic NPE for Java classes or Kotlin classes with only secondary
  constructors.
- **Fix:** Explicit check: `val ctor = cls.primaryConstructor ?: throw InvalidClassProvided(...)`.

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
