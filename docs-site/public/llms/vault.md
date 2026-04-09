## Vault — Shared DBAL Foundation

### Overview {#vault-overview}

# Vault — Shared DBAL Foundation

The entity model and repository abstractions shared by Karango (ArangoDB) and Monko (MongoDB). Define your domain
classes
once, persist them in either database.

## What Vault Provides

- **Storable Hierarchy** — `Stored<T>`, `New<T>`, `Ref<T>` wrappers that keep database metadata out of your domain
  classes.
- **Repository Interface** — CRUD operations, lifecycle hooks, and entity caching.
- **Flow-Based Cursors** — Lazy result streaming with 29 suspend convenience extensions.
- **KSP Annotation** — `@Vault` triggers compile-time generation of type-safe property paths.

Vault is not a standalone library. It is the shared foundation used by
[Karango](https://peekandpoke.io/llms/karango.md) and [Monko](https://peekandpoke.io/llms/monko.md).

---

### Storable Hierarchy {#vault-storable-hierarchy}

# Storable Hierarchy

Your data class is just data. Database metadata (`_id`, `_key`, `_rev`) lives in the wrapper, not in your class.

## The sealed base

`Storable<T>` is sealed. It defines two key operations:

```kotlin
sealed class Storable<T> {
    suspend fun resolve(): T
    suspend operator fun invoke(): T  // shorthand for resolve()
}
```

Three concrete types represent different entity states:

| Type        | When to use                      | Has value?        | Has ID?                     |
|-------------|----------------------------------|-------------------|-----------------------------|
| `New<T>`    | Entity not yet persisted         | Yes (`val value`) | Empty until inserted        |
| `Stored<T>` | Loaded from or saved to database | Yes (`val value`) | Yes (`_id`, `_key`, `_rev`) |
| `Ref<T>`    | Lazy reference to another entity | Via `resolve()`   | Yes                         |

## New<T> -- entities before persistence

```kotlin
val newPerson = New(Person("Alice", 30))

// Or pass the value directly -- insert() wraps it
repo.insert(Person("Alice", 30))

// With an explicit key
repo.insert("alice-key", Person("Alice", 30))
```

## Stored<T> -- the main entity wrapper

`Stored<T>` is what you work with most. It wraps your data with database metadata:

```kotlin
val stored: Stored<Person> = repo.findById("persons/abc123")!!

// Access data
stored.value.name  // "Alice"
stored.value.age   // 30

// Access metadata
stored._id         // "persons/abc123"
stored._key        // "abc123"
stored._rev        // revision string
stored.collection  // "persons"

// Modify the value -- creates a new Stored with the same metadata
val updated = stored.modify { it.copy(age = 31) }

// Or use withValue for a direct replacement
val replaced = stored.withValue(Person("Bob", 25))
```

### Modify and save pattern

```kotlin
val person = repo.findById(id)!!
val updated = person.modify { it.copy(name = "Alice Smith") }
repo.save(updated)
```

## Ref<T> -- lazy entity references

`Ref<T>` points to another entity by ID. It is lazy by default -- the referenced value is loaded on demand via
`suspend fun resolve()` or `suspend operator fun invoke()`. There is no separate `LazyRef` -- lazy and eager
behavior is unified in `Ref`.

```kotlin
@Vault
data class Comment(
    val text: String,
    val author: Ref<Person>,
)

val comment: Stored<Comment> = repo.findById(commentId)!!
comment.value.author._id              // "persons/abc123" -- available immediately

// Resolve the referenced entity (suspend)
val person: Person = comment.value.author.resolve()
// Or use invoke shorthand
val person2: Person = comment.value.author()
```

Creating refs explicitly:

```kotlin
// Eager ref -- value already loaded
val eager: Ref<Person> = Ref.eager(storedPerson)

// Lazy ref -- resolved on demand
val lazy: Ref<Person> = Ref.lazy("persons/abc123")
```

When serialized to the database, `Ref<T>` is stored as just the ID string. When deserialized, a lazy ref is created
and the entity is resolved on demand from the cache or database.

---

### Cursor {#vault-cursor}

# Cursor

Queries return `Cursor<T>`, a Flow-based wrapper for result sets.

## Flow-based access

```kotlin
val cursor = repo.findAll()

cursor.count      // number of results in current batch
cursor.fullCount  // total count (when using pagination)

// Stream results
cursor.asFlow(): Flow<T>

// Collect all results
cursor.toList(): List<T>  // suspend
```

## Suspend convenience extensions

Cursor provides 29 suspend convenience methods. All operate on the underlying Flow without materializing the full
result set upfront:

**Transform:** `map`, `mapNotNull`, `mapIndexed`, `flatMap`

**Filter:** `filter`, `filterNot`, `filterIsInstance`

**Find:** `firstOrNull`, `first`, `find`, `lastOrNull`

**Iterate:** `forEach`, `forEachIndexed`

**Test:** `any`, `none`, `all`

**Aggregate:** `fold`, `groupBy`, `associateBy`, `associate`, `partition`

**Order:** `sortedBy`, `sortedByDescending`

**Deduplicate:** `distinct`, `distinctBy`

**Slice:** `take`, `drop`

**Cache:** `cache()` (for `Cursor<Stored<T>>` -- caches entities in the EntityCache)

```kotlin
val cursor = repo.findAll()

cursor.map { it.value.name }
cursor.filter { it.value.published }
cursor.firstOrNull()
cursor.groupBy { it.value.author }
cursor.forEach { stored -> println(stored.value.title) }
```

All cursor convenience methods are `suspend` functions. Results are deserialized lazily.

---

### Repository Interface {#vault-repository}

# Repository Interface

The shared repository contract implemented by both Karango and Monko.

## CRUD operations

```kotlin
repo.findAll()                     // Cursor<Stored<T>>
repo.findById(id)                  // Stored<T>?
repo.insert(value)                 // Stored<T>
repo.insert(key, value)            // Stored<T> with explicit key
repo.save(stored)                  // Stored<T>
repo.remove(stored)                // remove by entity
repo.remove(id)                    // remove by ID
repo.removeAll()                   // remove all documents
```

## Lifecycle hooks

Hooks run during entity lifecycle events. Three hook interfaces:

| Hook            | Trigger               | Can modify entity? |
|-----------------|-----------------------|--------------------|
| `OnBeforeSave`  | Before insert or save | Yes                |
| `OnAfterSave`   | After insert or save  | No (read-only)     |
| `OnAfterDelete` | After remove          | No (read-only)     |

### Hook execution order

- **insert()**: OnBeforeSave -> persist -> OnAfterSave
- **save()**: OnBeforeSave -> persist -> OnAfterSave
- **remove()**: delete -> OnAfterDelete
- **batchInsert()**: OnBeforeSave (each) -> persist all -> OnAfterSave (each)

## EntityCache

The `EntityCache` deduplicates entities within a request scope. When the same entity is loaded multiple times
(directly or via `Ref<T>`), the cache returns the same instance.

```kotlin
// Synchronous
cache.getOrPut(id) { loadEntity() }

// Suspend
cache.getOrPutAsync(id) { loadEntityAsync() }
```

The cache is created per request (dynamic lifecycle in Kontainer) and shared across all repositories.

---

### Converting Between Types {#vault-converting}

# Converting Between Types

```kotlin
val stored: Stored<Person> = repo.findById(id)!!

// Stored -> Ref (eager, value already loaded)
val ref: Ref<Person> = stored.asRef

// Any Storable -> Stored
val backToStored: Stored<Person> = ref.asStored
```

---

### Identity Comparisons {#vault-identity}

# Identity Comparisons

```kotlin
val a: Stored<Person> = repo.findById("id1")!!
val b: Stored<Person> = repo.findById("id2")!!

a hasSameIdAs b    // false -- different IDs
a hasOtherIdThan b // true

val list = listOf(a, b)
a hasIdIn list     // true
```

---

### Type-Safe Casting {#vault-casting}

# Type-Safe Casting

```kotlin
sealed class Animal {
    data class Dog(val name: String) : Animal()
    data class Cat(val name: String) : Animal()
}

val stored: Stored<Animal> = repo.findById(id)!!

// Safe downcast -- returns null if the type doesn't match
val dog: Stored<Dog>? = stored.castTyped<Dog>()
val cat: Stored<Cat>? = stored.castTyped<Cat>()
```
