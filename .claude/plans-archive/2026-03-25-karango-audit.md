# Karango Library Audit: Code Fixes, KDoc, Missing Tests — COMPLETED

## Context

Karango is a type-safe Kotlin DSL for ArangoDB with 47 source files, 131 test files (E2E, requiring ArangoDB), and 20
TODOs. The code is generally clean but lacks KDoc on public APIs and has some dead code. Tests are strong on AQL
functions (85+ covered) but weak on infrastructure (driver, codec, indexes, repository methods).

## Batch 1: Code Fixes (small, safe) — DONE (prior session)

| File                     | Fix                                                          |
|--------------------------|--------------------------------------------------------------|
| `entity.kt`              | Removed (was commented-out dead code)                        |
| `cursor.kt:14,51`        | 2 TODOs about async — left as-is (architectural), KDoc added |
| `vault/IndexBuilder.kt`  | Debug code removed, unused var addressed                     |
| `vault/KarangoDriver.kt` | Debug code removed                                           |
| `index.kt:61`            | TODO about DB shutdown check — left as-is (enhancement)      |

## Batch 2: KDoc for Public API — DONE (prior session)

All files documented. `vault/base.kt` already had KDoc added.

## Batch 3: Missing Tests — Query Building (unit tests, no DB needed) — DONE

- `SortSpec.kt` — SORT ASC/DESC/multi-field/sort() helper/default direction
- `LimitSpec.kt` — LIMIT(n), LIMIT(offset, n), PAGE(page, epp), PAGE clamping, SKIP(n)
- `CollectSpec.kt` — COLLECT, COLLECT_WITH(COUNT), COLLECT_INTO, COLLECT_AGGREGATE
- `ReturnVariantsSpec.kt` — RETURN, RETURN_DISTINCT, RETURN_COUNT (with custom var name), FILTER_ANY

## Batch 4: Missing Tests — Repository Methods (E2E, needs ArangoDB) — DONE

- `E2E-Crud-Count-Spec.kt` — count() on empty, after inserts, after remove, after removeAll
- `E2E-Crud-FindByIds-Spec.kt` — findByIds varargs/collection, non-existing, by key, findById
- `E2E-Crud-BatchInsert-Spec.kt` — multiple items, empty list, reloadable
- `E2E-Crud-ModifyById-Spec.kt` — modifyById, non-existing, modifyByIdWhen true/false condition

## Batch 5: Missing Tests — Boolean Operators (resolves TODOs) — DONE

Added to `OperationBooleanSpec.kt`:

- `ANY_IN` — TODO removed from operator_boolean.kt
- `NONE_IN` — TODO removed from operator_boolean.kt
- `ALL_IN` — TODO removed from operator_boolean.kt

## Batch 6: Untested Public Functions — DONE

Identified 11 public functions with zero test coverage, then wrote tests for all of them:

**Unit tests:**

- `ReturnVariantsSpec.kt` — RETURN_DISTINCT, RETURN_COUNT, FILTER_ANY

**E2E function tests:**

- `E2E_Func_STARTS_WITH_Spec.kt` — matching, non-matching, empty prefix, empty string (TODO removed from func_stu.kt)
- `E2E_Func_MERGE_Spec.kt` — merge two docs, via LET, overwrite behavior
- `E2E_Func_UNSET_Spec.kt` — remove single/multiple attributes

**E2E CRUD tests:**

- `E2E-Crud-Remove-Query-Spec.kt` — REMOVE by stored entity, by key, via FOR loop
- `E2E-Crud-Upsert-Spec.kt` — UPSERT insert new, update existing, UPSERT_REPLACE
- `E2E-Crud-Document-Spec.kt` — DOCUMENT by collection+key, multiple keys, full ID

**Note:** RETURN_OLD / RETURN_NEW have implicit coverage (REMOVE always returns OLD, UPSERT always returns NEW).

## Verification

All tests pass: `./gradlew :karango:core:test` — 1644+ tests, 0 failures.
