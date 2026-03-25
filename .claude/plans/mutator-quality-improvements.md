# Plan: Mutator Library — Quality & Completeness Improvements

## Context

The Mutator library generates mutation utilities for immutable Kotlin data classes via KSP.
A multi-perspective review (dev advocate, senior engineer, QA) identified issues that undermine
trust and adoption: compiler warnings in generated code, untested code paths, and MutableCollection
contract violations. This plan addresses them in priority order.

## Phase 1 — Trust (eliminate blockers)

### 1.1 Stop generating `filterMutatorsOf()` for final classes

**Problem:** Every final `@Mutable` class produces 2 compiler warnings ("Type is final, so the
value of the type parameter is predetermined"). A project with 50 classes gets 100 warnings.
Teams with `-Werror` cannot use the library at all.

**Fix:** In `BuiltInMutableObjectsPlugin.generateMutatorFor()`, wrap the `filterMutatorsOf()`
generation in a check: only emit it when the class is sealed or abstract.

**File:** `mutator/ksp/src/main/kotlin/builtin/BuiltInMutableObjectsPlugin.kt`

### 1.2 Implement or fix `ListMutator.subList()`

**Problem:** `subList()` throws `TODO("Not yet implemented")` at runtime. This violates the
`MutableList` contract and can be triggered by standard library functions like `chunked()`.

**Fix:** Throw `UnsupportedOperationException("ListMutator does not support subList()")` with
a clear message instead of a bare TODO. Full implementation is complex (needs to propagate
mutations back to the parent list) and can be deferred.

**File:** `mutator/core/src/commonMain/kotlin/ListMutator.kt` (line ~259)

### 1.3 Fix `MapMutator.keys` exposing unguarded live set

**Problem:** `MapMutator.keys` returns the internal mutable map's key set directly. Removing a
key via `.keys.remove()` modifies the map but does NOT trigger observer notifications, silently
breaking the change propagation chain.

**Fix:** Return a read-only view (`keys` as `Set<K>`, not `MutableSet<K>`) or wrap in a
delegating set that intercepts `remove()`/`clear()` and calls the proper `MapMutator.remove()`.
The simplest correct fix is returning an unmodifiable view.

**File:** `mutator/core/src/commonMain/kotlin/MapMutator.kt` (line ~38)

Also fix `values` (line ~40-41) which returns a disconnected `MutableList` — return a read-only
view instead.

## Phase 2 — Polish (generated code quality)

### 2.1 Remove unnecessary backtick wrapping

**Problem:** All identifiers get backticks (`` `firstName` ``) even though only Kotlin reserved
words need them. This makes generated code look unprofessional and requires a blanket
`@file:Suppress("RemoveRedundantBackticks")`.

**Fix:** In `MutatorCodeBlocks.wrapWithBackticks()`, check if the identifier is a Kotlin
reserved word before wrapping. Use a hardcoded set of ~40 soft/hard keywords.

**Files:**

- `mutator/ksp/src/main/kotlin/MutatorCodeBlocks.kt` — `wrapWithBackticks()` method
- `mutator/ksp/src/main/kotlin/MutatorKspProcessor.kt` — remove `RemoveRedundantBackticks`
  from the `@file:Suppress` list

### 2.2 Replace wildcard import with explicit imports

**Problem:** Every generated file uses `import io.peekandpoke.mutator.*` which conflicts
with common linting rules.

**Fix:** Generate explicit imports for only the types actually used: `ObjectMutator`,
`ListMutator`, `SetMutator`, `MapMutator`, `Mutator`, `MutatorDsl`, and conditionally
`mutator` (the extension function).

**File:** `mutator/ksp/src/main/kotlin/MutatorKspProcessor.kt` (line ~105)

### 2.3 Narrow `@file:Suppress` annotations

**Problem:** Blanket `@file:Suppress("unused", "NOTHING_TO_INLINE")` hides real issues.

**Fix:** After fixing backticks (2.1), remove `RemoveRedundantBackticks`. Keep `NOTHING_TO_INLINE`
(generated inline functions genuinely trigger this). Remove `unused` — if generated functions
are truly unused, that's a signal the generator is producing unnecessary code.

**File:** `mutator/ksp/src/main/kotlin/MutatorKspProcessor.kt` (line ~101)

## Phase 3 — Test coverage (critical gaps)

### 3.1 Add nullable `@Mutable` property tests

**Problem:** Generated code has nullable-specific paths (`?.mutator()?.onChange`) that are
completely untested.

**Fix:** Add to the test domain:

```kotlin
data class PersonWithNullableAddress(
    val name: String,
    val address: Address?,
)
```

Write tests for: mutating null→non-null, non-null→null, non-null→different, null→null.

**Files:**

- `mutator/core/src/commonTest/kotlin/domain/domain.kt` — add domain type
- New test file: `mutator/core/src/jvmTest/kotlin/NullableMutatorSpec.kt`

### 3.2 Add Set and Map property tests

**Problem:** Only `List<Address>` properties are tested via `AddressBook`. No data class with
`Set<T>` or `Map<K, V>` properties exists in the test domain.

**Fix:** Add to the test domain:

```kotlin
data class WithCollections(
    val tags: Set<Address>,
    val lookup: Map<String, Address>,
)
```

Write tests for mutation through parent mutator.

**Files:**

- `mutator/core/src/commonTest/kotlin/domain/domain.kt` — add domain type
- New test file: `mutator/core/src/jvmTest/kotlin/e2e/CollectionPropertiesSpec.kt`

### 3.3 Test `commit()`, `set()`, `getInitialValue()`

**Problem:** Core `Mutator` API methods with zero test coverage.

**Fix:** Write tests verifying:

- `commit()` resets `isModified()` to false
- `getInitialValue()` returns initial value, then committed value after `commit()`
- `set(value)` / `invoke(value)` replaces the value and notifies

**File:** New test file: `mutator/core/src/jvmTest/kotlin/MutatorBaseSpec.kt`

### 3.4 Test `subList()` throws

**Fix:** Assert that `subList()` throws `UnsupportedOperationException` (after Phase 1 fix).

**File:** `mutator/core/src/jvmTest/kotlin/ListMutatorSpec.kt` — add test case

## Phase 4 — Correctness fixes (collection contracts)

### 4.1 Fix `SetMutator.replace()` iteration order

**Problem:** `remove(old)` + `add(new)` moves the element to the end in `LinkedHashSet`,
silently changing iteration order.

**Fix:** Rebuild the set with the replacement in the original position:

```kotlin
fun replace(old: V, new: V) {
    val set = doGet()
    val rebuilt = set.mapTo(mutableSetOf()) { if (it == old) new else it }
    // ... assign rebuilt
}
```

**File:** `mutator/core/src/commonMain/kotlin/SetMutator.kt` (line ~158-164)

### 4.2 Fix `MapMutator.entries` snapshot semantics

**Problem:** Every access to `entries` creates new wrapper objects with new `onChange` handlers.
`map.entries === map.entries` is always false.

**Fix:** Cache the entries wrapper and invalidate on structural changes, or document the
limitation clearly. Caching is preferred.

**File:** `mutator/core/src/commonMain/kotlin/MapMutator.kt` (line ~16-36)

## Verification

After each phase:

1. `./gradlew :mutator:core:jvmTest` — all tests pass
2. `./gradlew :mutator:core:compileTestKotlinJvm --rerun-tasks 2>&1 | grep "^w:"` — zero warnings
   after Phase 1
3. Inspect a generated file to verify clean output after Phase 2

## Files Summary

| File                                                                   | Phases   |
|------------------------------------------------------------------------|----------|
| `mutator/ksp/src/main/kotlin/builtin/BuiltInMutableObjectsPlugin.kt`   | 1.1      |
| `mutator/ksp/src/main/kotlin/MutatorCodeBlocks.kt`                     | 2.1      |
| `mutator/ksp/src/main/kotlin/MutatorKspProcessor.kt`                   | 2.2, 2.3 |
| `mutator/core/src/commonMain/kotlin/ListMutator.kt`                    | 1.2      |
| `mutator/core/src/commonMain/kotlin/MapMutator.kt`                     | 1.3, 4.2 |
| `mutator/core/src/commonMain/kotlin/SetMutator.kt`                     | 4.1      |
| `mutator/core/src/commonTest/kotlin/domain/domain.kt`                  | 3.1, 3.2 |
| New: `mutator/core/src/jvmTest/kotlin/NullableMutatorSpec.kt`          | 3.1      |
| New: `mutator/core/src/jvmTest/kotlin/e2e/CollectionPropertiesSpec.kt` | 3.2      |
| New: `mutator/core/src/jvmTest/kotlin/MutatorBaseSpec.kt`              | 3.3      |
| `mutator/core/src/jvmTest/kotlin/ListMutatorSpec.kt`                   | 3.4      |
