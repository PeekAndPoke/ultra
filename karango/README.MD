# Karango — Type-Safe ArangoDB in Kotlin

Write ArangoDB queries in Kotlin with full type safety. KSP generates the boilerplate, you write the logic.

## Why Karango?

Writing AQL queries as strings is error-prone — typos in field names, wrong types, broken queries that only fail at
runtime. Karango gives you a Kotlin DSL that catches these errors at compile time.

## Quick example

```kotlin
@Vault
data class User(
    val name: String,
    val email: String,
    val age: Int
)

class UsersRepo(driver: KarangoDriver) : EntityRepository<User>(
    name = "users", storedType = kType(), driver = driver
)

// Type-safe query
val adults = usersRepo.findList {
    FOR(usersRepo) { user ->
        FILTER(user.age GTE 18)
        SORT(user.name.ASC)
        RETURN(user)
    }
}
```

## Key features

- **Type-safe query DSL** — FOR, FILTER, SORT, RETURN — all checked at compile time
- **KSP code generation** — Entity accessors generated automatically, no manual boilerplate
- **Repository pattern** — CRUD operations, pagination, and bulk operations out of the box
- **Kontainer integration** — Repositories as injectable services with per-request scoping
- **Indexes & hooks** — Declarative index definitions and entity lifecycle hooks

## Documentation

Full docs at **[peekandpoke.io/ultra/karango](https://peekandpoke.io/ultra/karango/)**.

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.peekandpoke.karango:core:<version>")
    implementation("io.peekandpoke.ultra:vault:<version>")  // karango core does not expose it
    ksp("io.peekandpoke.karango:ksp:<version>")
}
```
