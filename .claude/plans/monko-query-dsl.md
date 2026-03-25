# Monko Query DSL — Design & Implementation Plan

**Date:** 2026-03-25
**Related:** [funktor-v1-roadmap.md](funktor-v1-roadmap.md)

---

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
