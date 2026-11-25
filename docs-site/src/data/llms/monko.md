## Monko -- Type-Safe MongoDB

### Overview {#monko-overview}

# Monko -- Type-Safe MongoDB in Kotlin

Type-safe MongoDB repositories for Kotlin. Typed queries, KSP-generated property paths, and the same entity model
as [Karango](https://peekandpoke.io/llms/karango.md).

## The Problem

MongoDB's Kotlin driver works. But you write string-based field paths everywhere:

```kotlin
// Without Monko
collection.find(Filters.eq("address.city", "Berlin"))
collection.find(Filters.gt("age", 18))
// Rename a field? Find-and-replace across the codebase. Miss one? Silent runtime failure.
```

## The Solution

```kotlin
// With Monko
repo.find { r ->
    filter(and(r.address.city.eq("Berlin"), r.age.gt(18)))
    sort(r.name.asc)
}
```

Rename a field? The compiler tells you every typed query that needs updating.

## How It Works

1. Annotate your data class with `@Vault`
2. KSP generates type-safe property paths at compile time
3. Write queries using a Kotlin DSL with MongoDB filter/sort operators
4. Monko compiles the DSL to MongoDB operations and executes them

## What You Get

- **Type-Safe Queries** -- KSP generates property paths for every `@Vault` entity. Filter, sort, and index by field
  name with compile-time checks.
- **Repository Pattern** -- CRUD with lifecycle hooks, typed queries, cursors, and a read-modify-write helper.
- **Index DSL** -- Define persistent, unique, sparse, and TTL indexes with the same type-safe property paths.
- **Shared Entity Model** -- Uses the same `Stored<T>`, `New<T>`, `Ref<T>` wrappers as Karango
  (shared via [Vault](https://peekandpoke.io/llms/vault.md)). Switch databases without changing domain code.
- **Slumber Integration** -- Serialization via Slumber. Sealed classes, nullable types, and custom codecs just work.
- **Funktor Integration** -- Pluggable backend for Funktor modules. Auth, logging, cluster, and messaging can all
  use Monko.

## How It Compares to Karango

Monko and Karango share the same architecture -- entity wrappers, repository pattern, lifecycle hooks, KSP code
generation, and Kontainer integration. The difference is the database underneath:

| Aspect          | Karango                                  | Monko                   |
|-----------------|------------------------------------------|-------------------------|
| Database        | ArangoDB                                 | MongoDB                 |
| Query language  | AQL DSL (FOR, FILTER, SORT)              | MongoDB filter/sort DSL |
| Entity model    | Stored, New, Ref                         | Same (shared via Vault) |
| KSP codegen     | AQL property paths                       | MongoDB property paths  |
| Lifecycle hooks | OnBeforeSave, OnAfterSave, OnAfterDelete | Same (shared via Vault) |

Both implement Vault's `Repository<T>`; typed queries are written per database.

---

### Getting Started {#monko-getting-started}

# Getting Started

Connect to MongoDB, define an entity, and run your first type-safe query.

## 1. Add the dependencies

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("io.peekandpoke.monko:core:{{ultraVersion}}")
    implementation("io.peekandpoke.ultra:vault:{{ultraVersion}}")  // @Vault, Stored, kType(): monko core does not expose it
    ksp("io.peekandpoke.monko:ksp:{{ultraVersion}}")  // KSP processor for type-safe property paths
}
```

## 2. Define an entity

Annotate your data class with `@Vault`. KSP generates type-safe property accessors at compile time.

```kotlin
import io.peekandpoke.ultra.vault.Vault

@Vault
data class Person(
    val name: String,
    val age: Int,
    val email: String,
    val address: Address = Address(),
)

@Vault
data class Address(
    val city: String = "",
    val zip: String = "",
)
```

## 3. Create a repository

```kotlin
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.ultra.reflection.kType

class PersonsRepo(driver: MonkoDriver) : MonkoRepository<Person>(
    name = "persons",
    storedType = kType(),
    driver = driver,
)
```

## 4. Configure the connection

```kotlin
import io.peekandpoke.monko.MongoDbConfig
import io.peekandpoke.ultra.common.model.Redacted

val config = MongoDbConfig(
    connectionString = Redacted("mongodb://root:root@localhost:27017"),
    database = "my_app",
)
```

Register the config and the repository in Kontainer, see
[Kontainer Integration](https://peekandpoke.io/llms/monko.md#monko-kontainer-integration). The `repo` below is the
`PersonsRepo` you get from the kontainer.

## 5. Basic CRUD

```kotlin
// Insert with explicit key
val stored = repo.insert("person-1", Person(name = "Alice", age = 30, email = "alice@example.com"))
// stored._id   -> "persons/person-1"
// stored._key  -> "person-1"
// stored() -> Person(name="Alice", ...)

// Find by ID
val found = repo.findById("persons/person-1")

// Update
val updated = found!!.modify { it.copy(age = 31) }
repo.save(updated)

// Read-modify-write: findById, then save (not atomic)
repo.modifyById("persons/person-1") { it.copy(age = it.age + 1) }

// Delete
repo.remove("persons/person-1")

// Find all
val everyone = repo.findAll()
```

## 6. Your first typed query

```kotlin
val adults = repo.find { r ->
    filter(r.age.gte(18))
    sort(r.name.asc)
    limit(20)
}
```

The `r.age` and `r.name` accessors are generated by KSP from the `Person` data class. Rename a field and the query
breaks at compile time, not at runtime.

---

### Entity Model {#monko-entity-model}

# Entity Model

Monko uses the same entity model as Karango -- the `Storable<T>` hierarchy from the shared
[Vault](https://peekandpoke.io/llms/vault.md) module. This means you can switch between MongoDB and ArangoDB without
changing your domain classes.

For the complete Storable reference -- including `New<T>`, `Stored<T>`, `Ref<T>`, type conversions, identity
comparisons, and type-safe casting -- see the [Vault documentation](https://peekandpoke.io/llms/vault.md).

## Defining entities

Annotate with `@Vault`. Your data class contains only domain fields. The wrapper adds `_id`, `_key`, and `_rev`.

```kotlin
@Vault
data class Person(
    val name: String,
    val age: Int,
    val email: String,
    val address: Address = Address(),
)

@Vault
data class Address(
    val city: String = "",
    val zip: String = "",
    val country: String = "",
)
```

## Working with Stored

```kotlin
val stored: Stored<Person> = repo.insert("key-1", Person("Alice", 30, "alice@example.com"))

// Access metadata
stored._id      // "persons/key-1"
stored._key     // "key-1"
stored()        // Person(name="Alice", age=30, ...)

// Modify (creates new Stored with same metadata)
val updated = stored.modify { it.copy(age = 31) }
repo.save(updated)

// Transform to a different type
val nameOnly = stored.transform { it.name }  // Stored<String>

// Identity checks
stored.hasSameIdAs(other)
stored.hasIdIn(listOf(other1, other2))
```

## MongoDB document IDs

Monko maps MongoDB document IDs as follows:

- If you provide a key via `insert(key, value)`, the document's `_id` is that key, and `Stored._id` is
  `"collection/key"`
- If no key is provided, MongoDB assigns an `ObjectId`; `_key` is its hex string
- The `_key` is always the portion after the slash in `Stored._id`

---

### Query DSL {#monko-query-dsl}

# Query DSL

Type-safe filters and sorts using KSP-generated property paths.

## Building queries

Queries use the `find` method with a builder lambda. The builder receives a typed expression `r` representing your
entity, with KSP-generated property accessors.

```kotlin
val results = repo.find { r ->
    filter(and(r.age.gte(18), r.address.city.eq("Berlin")))
    sort(r.name.asc)
    limit(20)
}
```

Each of `filter`, `sort`, `limit` and `skip` sets one value: a second `filter()` call replaces the first. Combine
conditions with `and()`.

## Comparison operators

| Operator | MongoDB | Example                                      |
|----------|---------|----------------------------------------------|
| `eq`     | $eq     | `r.name.eq("Alice")`                         |
| `ne`     | $ne     | `r.status.ne("deleted")`                     |
| `gt`     | $gt     | `r.age.gt(18)`                               |
| `gte`    | $gte    | `r.age.gte(18)`                              |
| `lt`     | $lt     | `r.score.lt(50)`                             |
| `lte`    | $lte    | `r.score.lte(100)`                           |
| `isIn`   | $in     | `r.status.isIn(listOf("active", "pending"))` |
| `nin`    | $nin    | `r.status.nin(listOf("deleted"))`            |

A value of your own `@JvmInline value class` is reduced to its backing value. Only `String`, `Number` and `Boolean`
backings are supported; any other backing throws `IllegalArgumentException` when the filter is built.

## String operators

```kotlin
// Regex matching
filter(r.email.regex(".*@example\\.com"))
filter(r.name.regex(Regex("^A", RegexOption.IGNORE_CASE)))
```

## Logical operators

```kotlin
import io.peekandpoke.monko.lang.dsl.and
import io.peekandpoke.monko.lang.dsl.or
import io.peekandpoke.monko.lang.dsl.not

// AND
filter(and(r.age.gte(18), r.status.eq("active")))

// OR
filter(
    or(
        r.status.eq("active"),
        r.status.eq("pending"),
    )
)

filter(
    and(
        r.age.gte(18),
        or(
            r.address.city.eq("Berlin"),
            r.address.city.eq("Munich"),
        ),
    )
)
```

## Array operators

```kotlin
// Element match
filter(r.tags.elemMatch(Filters.eq("name", "kotlin")))

// Array size
filter(r.tags.size(3))

// All elements in array
filter(r.tags.all(listOf("kotlin", "mongodb")))

// Existence check
filter(r.middleName.exists(true))
filter(r.deletedAt.exists(false))
```

## Sorting

```kotlin
// Single field (a second sort() call replaces the first)
sort(r.name.asc)

// Multiple fields
sort(orderBy(r.name.asc, r.age.desc))
```

## Pagination

```kotlin
// Limit results
limit(20)

// Skip + limit
skip(40)
limit(20)

// The cursor provides fullCount for pagination UI
val cursor = repo.find { r ->
    filter(r.status.eq("active"))
    sort(r.createdAt.desc)
    skip((page - 1) * pageSize)
    limit(pageSize)
}
// cursor.fullCount -> total matching documents (null when the query has no filter)
```

## Nested property access

KSP generates accessors for nested types. Access deeply nested fields with dot notation:

```kotlin
// r.address is generated because Person has an Address field
filter(and(r.address.city.eq("Berlin"), r.address.zip.regex("^10")))
sort(r.address.country.asc)
```

Every property path in a query is validated at compile time. If `Address` no longer has a `city` field, the query
will not compile.

---

### Repositories {#monko-repositories}

# Repositories

MonkoRepository provides typed CRUD, queries, and cursors.

## Defining a repository

```kotlin
class PersonsRepo(driver: MonkoDriver) : MonkoRepository<Person>(
    name = "persons",
    storedType = kType(),
    driver = driver,
)
```

The repository is registered as a singleton in Kontainer. The `MonkoDriver` it takes is dynamic (per request), so
Kontainer makes the repository semi-dynamic: one instance per kontainer.

## CRUD operations

```kotlin
// Insert with explicit key
val stored = repo.insert("alice-1", Person(name = "Alice", age = 30, email = "alice@example.com"))

// Insert with auto-generated ObjectId
val bob = repo.insert(New(Person(name = "Bob", age = 25, email = "bob@example.com")))

// Find by ID
val person = repo.findById("persons/alice-1")  // returns Stored<Person>?

// Find all
val everyone = repo.findAll()  // returns MonkoCursor<Stored<Person>>

// Save (update)
val modified = person!!.modify { it.copy(age = 31) }
repo.save(modified)

// Read-modify-write: findById, then save (not atomic)
repo.modifyById("persons/alice-1") { it.copy(age = it.age + 1) }

// Delete by id (runs no hooks)
repo.remove("persons/alice-1")

// Delete a Stored entity (runs the OnAfterDelete hooks)
repo.remove(bob)

// Delete all
repo.removeAll()
```

## Custom queries

```kotlin
// Type-safe query with builder
val berliners = repo.find { r ->
    filter(and(r.address.city.eq("Berlin"), r.age.gte(18)))
    sort(r.name.asc)
    limit(50)
}

// Access results
berliners.toList()       // List<Stored<Person>>
berliners.count           // number of results returned
berliners.fullCount       // total matching (for pagination), null without a filter
berliners.forEach { ... } // iterate
```

## Cursors

`MonkoCursor<T>` wraps query results with metadata:

```kotlin
val cursor = repo.find { r ->
    filter(r.address.city.eq("Berlin"))
    sort(r.name.asc)
    skip(20)
    limit(10)
}

cursor.count       // 10 (results in this page)
cursor.fullCount   // 253 (total matching documents)
cursor.entries     // List<Stored<Person>>
```

See the [Vault documentation](https://peekandpoke.io/llms/vault.md#vault-cursor) for the full list of cursor
convenience extensions (map, filter, forEach, groupBy, etc.).

## Batch operations

```kotlin
// Insert multiple
val people = listOf(
    Person("Alice", 30, "alice@example.com"),
    Person("Bob", 25, "bob@example.com"),
    Person("Charlie", 35, "charlie@example.com"),
)

people.forEach { repo.insert(it) }  // Monko has NO batch insert (Karango does)
```

## Collection stats

```kotlin
val stats = repo.getStats()
// stats.storage.count, stats.indexes, etc.
```

---

### Indexes & Hooks {#monko-indexes-and-hooks}

# Indexes & Hooks

Define indexes with the same type-safe property paths you use in queries. Hook into entity lifecycle for timestamps,
validation, and side effects.

## Defining indexes

Override `buildIndexes()` in your repository. Index fields use the same KSP-generated property paths as queries.
This example assumes `Person` also has `expiresAt` and `middleName` fields.

```kotlin
class PersonsRepo(driver: MonkoDriver) : MonkoRepository<Person>(
    name = "persons",
    storedType = kType(),
    driver = driver,
) {
    override fun MonkoIndexBuilder<Person>.buildIndexes() {
        // Single field index
        persistentIndex {
            field { it.email }
        }

        // Compound index
        persistentIndex {
            field { it.address.city }
            field { it.address.zip }
        }

        // Unique index with custom name
        uniqueIndex {
            name("idx_email_unique")
            field { it.email }
        }

        // TTL index: MongoDB deletes expired documents (the field must hold a BSON Date)
        ttlIndex {
            field { it.expiresAt }
            expireAfter(0)  // delete when expiresAt is in the past
        }

        // Sparse index -- only index documents where the field exists
        uniqueIndex {          // sparseness is a FLAG, not an index type
            field { it.middleName }
            sparse()
        }
    }
}
```

## Index types

| Builder           | MongoDB Type   | Use case                   |
|-------------------|----------------|----------------------------|
| `persistentIndex` | Standard index | Query optimization         |
| `uniqueIndex`     | Unique index   | Enforce uniqueness         |
| `ttlIndex`        | TTL index      | Auto-expire documents      |

A TTL index only expires documents whose field holds a BSON Date. Slumber stores every date type (`MpInstant`,
`Instant`, `Date`, ...) as an object and an epoch timestamp as a number, so a TTL index on such a field deletes nothing.

## Index management

```kotlin
// Create missing indexes. An index whose name exists with other fields, unique, sparse or
// partial filter is dropped and re-created; a changed TTL expiry is not detected. Default names
// come from the fields, so a changed field list creates a new index and leaves the old one
// (use recreateIndexes()).
// A failed create is logged as a warning, not thrown.
repo.ensureIndexes()

// Drop all indexes except _id_, then ensure
repo.recreateIndexes()

// Validate without creating
val info = repo.validateIndexes()
```

On application startup, Funktor calls `ensureIndexes()` automatically via the `EnsureRepositoriesOnAppStarting`
lifecycle hook.

## Lifecycle hooks

Hooks run during entity lifecycle events. They use the same Vault hook interfaces as Karango.
See the [Vault documentation](https://peekandpoke.io/llms/vault.md#vault-repository) for hook interface details.

### Timestamped

The most common hook. Sets `createdAt` and `updatedAt` on insert; on save it sets `updatedAt` (and `createdAt` only
while unset), in every repository that has
`timestamped.onBeforeSave()` among its hooks.

```kotlin
@Vault
data class Article(
    val title: String,
    val body: String,
    override val createdAt: MpInstant = MpInstant.Epoch,
    override val updatedAt: MpInstant = MpInstant.Epoch,
) : Timestamped {
    override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
    override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
}
```

### Custom hooks

```kotlin
class ArticlesRepo(driver: MonkoDriver, timestamped: TimestampedHook) : MonkoRepository<Article>(
    name = "articles",
    storedType = kType(),
    driver = driver,
    hooks = Hooks.of(          // takes hooks directly; sorted by type internally
        timestamped.onBeforeSave(),
        MyAfterSaveHook(),
        MyAfterDeleteHook(),
    ),
)
```

### Hook execution order

| Operation         | Hook sequence                          |
|-------------------|----------------------------------------|
| `insert()`        | OnBeforeSave -> persist -> OnAfterSave |
| `save()`          | OnBeforeSave -> persist -> OnAfterSave |
| `remove(stored)`  | delete -> OnAfterDelete                |
| `remove(idOrKey)` | delete (no hooks)                      |

---

### Kontainer Integration {#monko-kontainer-integration}

# Kontainer Integration

Register MongoDB connections and repositories in Kontainer.

## Setup

```kotlin
import io.peekandpoke.monko.MongoDbConfig
import io.peekandpoke.monko.monko
import io.peekandpoke.ultra.common.model.Redacted
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.log.ultraLogging
import io.peekandpoke.ultra.vault.VaultConfig
import io.peekandpoke.ultra.vault.ultraVault

val blueprint = kontainer {
    // Monko builds on these: logging, Vault, and a Kronos for the Vault timestamp hooks
    singleton(Kronos::class) { Kronos.systemUtc }
    ultraLogging()
    ultraVault(VaultConfig())

    monko(MongoDbConfig(
        connectionString = Redacted("mongodb://root:root@localhost:27017"),
        database = "my_app",
    ))

    // Register your repositories
    singleton(PersonsRepo::class)
    singleton(ArticlesRepo::class)
}
```

Funktor registers the first three for you (see below).

The `monko(config)` call registers:

- `MongoClient` -- singleton, shared connection pool
- `MongoDatabase` -- singleton, database handle
- `MonkoDriver` -- dynamic, created per request (carries profiler context)
- `MonkoCodec` -- dynamic, created per request (carries EntityCache)

The `MongoClient` is cached and reused across container instances. Only the driver and codec are created fresh per
request -- they carry per-request state like query profiling and entity deduplication.

## With Funktor

In a Funktor application, Monko integrates as a pluggable backend. Each module declares its storage needs
independently:

```kotlin
fun createBlueprint(config: MyAppConfig) = kontainer {
    funktor(
        config = config,
        auth = { useMonko() },       // Auth records in MongoDB
        logging = { useMonko() },    // Logs in MongoDB
        cluster = { useMonko() },    // Jobs, locks, storage in MongoDB
        messaging = { useMonko() },  // Sent messages in MongoDB
    )

    // MongoDB connection
    monko(config.mongodb)

    // Your repositories
    singleton(UsersRepo::class)
}
```

## Mixing databases

You can use both Monko and Karango in the same application. Each Funktor module chooses its backend independently:

```kotlin
funktor(
    config = config,
    auth = { useKarango() },     // Auth in ArangoDB
    logging = { useMonko() },    // Logs in MongoDB
    cluster = { useKarango() },  // Cluster in ArangoDB
    messaging = { useMonko() },  // Messages in MongoDB
)

karango(config.arangodb)
monko(config.mongodb)
```

## Database lifecycle

```kotlin
// Access through Kontainer
val driver = kontainer.get(MonkoDriver::class)

// Or in Funktor handlers, repositories are available via Database
val Database.persons get() = getRepository<PersonsRepo>()
```

On startup, Funktor's `EnsureRepositoriesOnAppStarting` hook automatically calls `ensureIndexes()` on all
registered repositories.
