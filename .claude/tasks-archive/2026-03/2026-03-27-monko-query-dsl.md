# Monko Query DSL — Design & Implementation Plan

**Date:** 2026-03-25
**Related:** [funktor-v1-roadmap.md](funktor-v1-roadmap.md)

---



## Commits & files changed

<!-- Generated 2026-07-28 from `git log --follow --name-status` over this task file.
     Commits that merely renamed the doc into the archive (R100) are excluded, since
     their code belongs to whatever task shipped alongside the move. -->

> **Attribution caveat.** No commit maps cleanly to this task — the entries below also carried unrelated work (a repo-wide change or a bulk task-doc move), so treat the file list as approximate.

| Commit | Date | Files | Subject |
|---|---|---:|---|
| `d71db9f8` | 2026-07-20 | 19 | funktor security vault structured concurrency fix |
| `3695944c` | 2026-03-27 | 14 | docs, tests, bug fixes monko: better dsl funktor: monko repos funktor: demo |

### Files changed (33)

**(root)**
- `CLAUDE.md`

**funktor-demo/adminapp**
- `funktor-demo/adminapp/src/jsMain/kotlin/api.kt`
- `funktor-demo/adminapp/src/jsMain/kotlin/layout/LoggedInLayout.kt`
- `funktor-demo/adminapp/src/jsMain/kotlin/nav.kt`
- `funktor-demo/adminapp/src/jsMain/kotlin/pages/showcase/CoreFeaturesPage.kt`
- `funktor-demo/adminapp/src/jsMain/kotlin/pages/showcase/RestFeaturesPage.kt`

**funktor-demo/common**
- `funktor-demo/common/build.gradle.kts`
- `funktor-demo/common/src/commonMain/kotlin/showcase/CoreShowcaseModels.kt`
- `funktor-demo/common/src/commonMain/kotlin/showcase/RestShowcaseModels.kt`
- `funktor-demo/common/src/commonMain/kotlin/showcase/ShowcaseApiClient.kt`

**funktor-demo/server**
- `funktor-demo/server/src/main/kotlin/api/showcase/CoreShowcaseApi.kt`
- `funktor-demo/server/src/main/kotlin/api/showcase/RestShowcaseApi.kt`
- `funktor-demo/server/src/main/kotlin/api/showcase/ShowcaseApiFeature.kt`
- `funktor-demo/server/src/main/kotlin/kontainer.kt`
- `funktor-demo/server/src/main/kotlin/showcase/ShowcaseModule.kt`

**funktor/auth**
- `funktor/auth/src/jvmTest/kotlin/index_jvmTest.kt`

**funktor/core**
- `funktor/core/src/jvmMain/kotlin/config/ktor/KtorConfig.kt`
- `funktor/core/src/jvmMain/kotlin/core_module.kt`
- `funktor/core/src/jvmMain/kotlin/lifecycle/VaultHookScopeBinder.kt`
- `funktor/core/src/jvmTest/kotlin/config/ktor/KtorConfigSpec.kt`

**funktor/rest**
- `funktor/rest/src/jvmMain/kotlin/ApiStatusPages.kt`
- `funktor/rest/src/jvmTest/kotlin/ApiStatusPagesSpec.kt`

**karango/core**
- `karango/core/src/main/kotlin/vault/EntityRepository.kt`
- `karango/core/src/main/kotlin/vault/KarangoDriver.kt`
- `karango/core/src/test/kotlin/e2e/common.kt`
- `karango/core/src/test/kotlin/e2e/crud/E2E-Crud-HookScope-Spec.kt`

**monko/core**
- `monko/core/src/main/kotlin/MonkoDriver.kt`
- `monko/core/src/main/kotlin/MonkoRepository.kt`

**ultra/vault**
- `ultra/vault/src/jvmMain/kotlin/Repository.kt`
- `ultra/vault/src/jvmMain/kotlin/VaultHookScope.kt`
- `ultra/vault/src/jvmMain/kotlin/vault_module.kt`
- `ultra/vault/src/jvmTest/kotlin/VaultHookScopeSpec.kt`
- `ultra/vault/src/jvmTest/kotlin/VaultHookScopeWiringSpec.kt`

## Context

Monko queries currently require manual string extraction via `field {}` and raw `Filters.eq(...)` calls.
The auth repo shows the pain:

```kotlin
// BEFORE — current pain
find {
    filter(
        Filters.and(
            Filters.eq(field { it._type }, type),
            Filters.eq(field { it.realm }, realm),
            Filters.eq(field { it.ownerId }, owner),
        )
    )
    sort(Sorts.descending(field { it.createdAt.ts }))
    limit(1)
}
```

The goal: type-safe, IDE-discoverable queries using KSP-generated property paths that produce standard
MongoDB `Bson` objects. The syntax should feel MongoDB-native (not AQL-like), while providing the same
quality experience as Karango.

```kotlin
// AFTER — target experience
find { r ->
    filter(
        and(
            r._type eq type,
            r.realm eq realm,
            r.ownerId eq owner,
        )
    )
    sort(r.createdAt.ts.desc)
    limit(1)
}
```

---

## How It Works

### The Bridge: MongoPropertyPath → field string → Bson

KSP already generates typed property paths. For example, `r.realm` produces a
`MongoPropertyPath<String, String>` that internally knows its dot-notation path is `"realm"`.

The DSL adds **infix extension functions** on `MongoPropertyPath` that:

1. Extract the dot-notation string via `toFieldPath()`
2. Pass it to the standard MongoDB `Filters`/`Sorts`/`Updates` factory methods
3. Return standard `Bson` objects the driver already accepts

No custom query AST. No intermediate representation. Just a thin type-safe bridge.

---

## API Design

### Filter Operators

All operators are infix extensions on `MongoPropertyPath<*, T>`:

```kotlin
// Comparison
r.name eq "Alice"              // Filters.eq("name", "Alice")
r.name ne "Bob"                // Filters.ne("name", "Bob")
r.age gt 25                    // Filters.gt("age", 25)
r.age gte 25                   // Filters.gte("age", 25)
r.age lt 65                    // Filters.lt("age", 65)
r.age lte 65                   // Filters.lte("age", 65)

// Collection membership
r.status isIn listOf("A", "B") // Filters.in("status", [...])
r.status nin listOf("X")       // Filters.nin("status", [...])

// String
r.name regex "^Ali"            // Filters.regex("name", "^Ali")

// Existence
r.email.exists()               // Filters.exists("email")
r.email.exists(false)          // Filters.exists("email", false)

// Array
r.grades.elemMatch(filter)     // Filters.elemMatch("grades", filter)
```

**Naming choices:**

- `eq`/`ne`/`gt`/`gte`/`lt`/`lte` — lowercase, Kotlin-idiomatic, distinct from Karango's uppercase
- `isIn` not `in` — `in` is a Kotlin keyword
- `setTo` not `set` — avoids shadowing `kotlin.collections.set`

### Logical Combinators

Top-level functions wrapping MongoDB's combinators:

```kotlin
and(r.realm eq realm, r.ownerId eq owner)  // Filters.and(...)
or(r.status eq "A", r.status eq "B")       // Filters.or(...)
not(r.deleted eq true)                      // Filters.not(...)
```

### Sort

Property extensions returning `Bson`:

```kotlin
r.createdAt.ts.desc            // Sorts.descending("createdAt.ts")
r.name.asc                     // Sorts.ascending("name")
orderBy(r.name.asc, r.age.desc)  // Sorts.orderBy(...)
```

### Updates

```kotlin
r.name setTo "Alice"           // Updates.set("name", "Alice")
r.tempField.unset()            // Updates.unset("tempField")
r.count inc 1                  // Updates.inc("count", 1)
combine(r.name setTo "X", r.count inc 1)  // Updates.combine(...)
```

### Repository `find { r -> }` overload

A new overload that provides the typed root expression:

```kotlin
// New — root expression passed as lambda parameter
suspend fun find(
    build: MonkoDriver.FindQueryBuilder.(r: MongoIterableExpr<T>) -> Unit
): MonkoCursor<Stored<T>>

// Existing — kept for backward compatibility
suspend fun find(
    query: MonkoDriver.FindQueryBuilder.() -> Unit
): MonkoCursor<Stored<T>>
```

---

## Real-World Examples

### Auth: Find Latest Record

```kotlin
override suspend fun findLatest(realm: String, type: String, owner: String): Stored<AuthRecord>? {
    val found = find { r ->
        filter(
            and(
                r._type eq type,
                r.realm eq realm,
                r.ownerId eq owner,
            )
        )
        sort(r.createdAt.ts.desc)
        limit(1)
    }
    return found.firstOrNull()
}
```

### Auth: Find by Token

```kotlin
override suspend fun findByToken(realm: String, type: String, token: String): Stored<AuthRecord>? {
    val found = find { r ->
        filter(
            and(
                r._type eq type,
                r.realm eq realm,
                r.token eq token,
            )
        )
        limit(1)
    }
    return found.firstOrNull()
}
```

### Logs: Severity Filter with Paging

```kotlin
val found = find { r ->
    filter(
        and(
            r.severity isIn listOf("ERROR", "WARN"),
            r.createdAt.ts gte startTs,
            r.createdAt.ts lte endTs,
        )
    )
    sort(r.createdAt.ts.desc)
    skip(offset)
    limit(pageSize)
}
```

### Index Creation (already works, no DSL change)

```kotlin
override suspend fun ensureIndexes() {
    driver.createIndex(
        collection = name,
        keys = Document(
            mapOf(
            field { it.realm } to 1,
            field { it.ownerId } to 1,
            field { it._type } to 1,
        )),
    )
}
```

---

## Files to Create

All new files under `monko/core/src/main/kotlin/lang/dsl/`:

| File            | Contents                                                                                                                                                           |
|-----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `path_utils.kt` | `MongoPropertyPath.toFieldPath()` — extracts dot-notation string                                                                                                   |
| `filters.kt`    | All filter infix operators (`eq`, `ne`, `gt`, `gte`, `lt`, `lte`, `isIn`, `nin`, `regex`, `exists`, `elemMatch`) + logical combinators (`and`, `or`, `not`, `nor`) |
| `sorts.kt`      | `.asc`, `.desc` property extensions + `orderBy()`                                                                                                                  |
| `updates.kt`    | `setTo`, `unset`, `inc`, `combine`                                                                                                                                 |

## Files to Modify

| File                                       | Change                                                                                     |
|--------------------------------------------|--------------------------------------------------------------------------------------------|
| `monko/core/.../MonkoRepository.kt`        | Add typed `find { r -> }` overload. Refactor `field {}` to use `toFieldPath()` internally. |
| `funktor/auth/.../MonkoAuthRecordsRepo.kt` | Migrate to new DSL as validation.                                                          |

## Files NOT Changed

| File                    | Why                                                                      |
|-------------------------|--------------------------------------------------------------------------|
| `MonkoDriver.kt`        | `FindQueryBuilder` already accepts `Bson` — no changes needed            |
| `MonkoKspProcessor.kt`  | Already generates the right property path extensions — no changes needed |
| `base_property_path.kt` | `toFieldPath()` goes in a new file, not here                             |

---

## Implementation Order

1. **`path_utils.kt`** — Extract `toFieldPath()` from `MonkoRepository.field {}` logic
2. **`filters.kt`** — All filter operators + combinators
3. **`sorts.kt`** — Sort extensions
4. **`updates.kt`** — Update extensions
5. **`MonkoRepository.kt`** — Add typed `find { r -> }`, refactor `field {}`
6. **`MonkoAuthRecordsRepo.kt`** — Migrate as proof that the DSL works

## Verification

1. Build the project: `./gradlew :monko:core:compileKotlin`
2. Verify MonkoAuthRecordsRepo compiles with new DSL
3. Run existing auth tests: `./gradlew :funktor:auth:jvmTest`
4. Verify backward compatibility — old `find {}` overload still works

---

## Design Principles

- **Thin wrappers** — Produce standard `Bson`, not a custom AST. Mix DSL with raw `Filters.*` freely.
- **No lock-in** — Everything returns `Bson`. Drop down to raw MongoDB API anytime.
- **No new concepts** — Uses existing `MongoPropertyPath` and `FindQueryBuilder`. Just adds ergonomics.
- **Lowercase operators** — `eq`/`gt`/`desc` feel Kotlin-native, distinct from Karango's `EQ`/`GT`/`DESC`.
