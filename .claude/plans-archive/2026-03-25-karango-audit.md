# Karango Library Audit: Code Fixes, KDoc, Missing Tests

## Context

Karango is a type-safe Kotlin DSL for ArangoDB with 47 source files, 131 test files (E2E, requiring ArangoDB), and 20
TODOs. The code is generally clean but lacks KDoc on public APIs and has some dead code. Tests are strong on AQL
functions (85+ covered) but weak on infrastructure (driver, codec, indexes, repository methods).

## Batch 1: Code Fixes (small, safe)

| File                     | Fix                                                                                                                      |
|--------------------------|--------------------------------------------------------------------------------------------------------------------------|
| `entity.kt`              | Entire file is commented-out dead code — remove the file or clean it up                                                  |
| `cursor.kt:14,51`        | 2 TODOs about async — leave as-is (architectural, not a quick fix), but add a comment explaining the blocking constraint |
| `vault/IndexBuilder.kt`  | Remove commented debug code (lines 41-43), address unused `deleted` var (line 160)                                       |
| `vault/KarangoDriver.kt` | Remove commented debug code (lines 98-100, 114-115)                                                                      |
| `index.kt:61`            | TODO about DB shutdown check — leave as-is, it's an enhancement                                                          |

## Batch 2: KDoc for Public API

Add concise KDoc to these public-facing files only:

| File                        | What to document                                                                              |
|-----------------------------|-----------------------------------------------------------------------------------------------|
| `config/ArangoDbConfig.kt`  | Data class, connection parameters, `forUnitTests`                                             |
| `vault/EntityRepository.kt` | Class, key methods: insert, save, remove, find*, batchInsert, modifyById, hooks, buildIndexes |
| `vault/KarangoDriver.kt`    | Class purpose, query execution                                                                |
| `vault/base.kt`             | KarangoRepository interface                                                                   |
| `vault/IndexBuilder.kt`     | Index types (persistent, TTL), field DSL                                                      |
| `slumber/KarangoCodec.kt`   | Class purpose, Slumber integration                                                            |
| `cursor.kt`                 | KarangoCursor, iteration, count                                                               |
| `query.kt`                  | buildAqlQuery, AqlTypedQuery                                                                  |
| `exceptions.kt`             | KarangoException, KarangoQueryException                                                       |
| `aql/for.kt`                | FOR loop DSL                                                                                  |
| `aql/filter.kt`             | FILTER, comparison operators                                                                  |
| `aql/return.kt`             | RETURN variants                                                                               |
| `aql/insert.kt`             | INSERT INTO                                                                                   |
| `aql/update.kt`             | UPDATE DSL                                                                                    |
| `aql/remove.kt`             | REMOVE IN                                                                                     |

Skip KDoc for: internal expression classes, function implementations (func_*.kt), printer, base classes.

## Batch 3: Missing Tests — Query Building (unit tests, no DB needed)

These test that the AQL DSL generates correct query strings via `AqlPrinter.print()`:

**New file: `SortSpec.kt`** — test SORT ASC/DESC/multi-field
**New file: `LimitSpec.kt`** — test LIMIT(n), LIMIT(offset, n), PAGE(page, epp), SKIP(n)
**New file: `CollectSpec.kt`** — test COLLECT, COLLECT_WITH(COUNT), COLLECT_AGGREGATE

## Batch 4: Missing Tests — Repository Methods (E2E, needs ArangoDB)

Extend existing E2E test structure. Follow `E2E-Crud-Insert-Spec.kt` pattern.

**New file: `E2E-Crud-Count-Spec.kt`** — test count() after inserts/removes
**New file: `E2E-Crud-FindByIds-Spec.kt`** — test findByIds(), findFirst(), findList()
**New file: `E2E-Crud-BatchInsert-Spec.kt`** — test batchInsert() with hooks
**New file: `E2E-Crud-ModifyById-Spec.kt`** — test modifyById(), modifyByIdWhen()

## Batch 5: Missing Tests — Boolean Operators (resolves TODOs)

Extend `OperationBooleanSpec.kt` or create new specs:

- `ANY_IN` — resolves TODO at operator_boolean.kt:57
- `NONE_IN` — resolves TODO at operator_boolean.kt:70
- `ALL_IN` — resolves TODO at operator_boolean.kt:83

## Verification

- Batch 1-2: `./gradlew :karango:core:compileKotlinJvm` (no DB needed)
- Batch 3: Unit tests only if they don't import `database` — verify with compile
- Batch 4-5: `./gradlew :karango:core:test` (requires ArangoDB running)

## Execution Order

1. Batch 1 (code fixes) — safe, no test dependency
2. Batch 2 (KDoc) — safe, no test dependency
3. Batch 3 (query building unit tests) — may or may not need DB
4. Batch 4 (repository E2E tests) — needs ArangoDB
5. Batch 5 (boolean operator E2E tests) — needs ArangoDB
