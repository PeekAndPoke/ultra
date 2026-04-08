# Plan: Non-Blocking DBAL — Flow Cursor, Suspend Storable, Ref Unification

## Context

The DBAL layers (vault, karango, monko) have 3 `runBlocking` calls that block coroutine threads, reducing application
throughput:

1. **KarangoCursor** batch pagination — blocks on every multi-batch iteration
2. **RefCodec** — blocks during eager entity reference resolution
3. **LazyRefCodec** — blocks on lazy entity reference resolution

This plan eliminates all blocking by:

- Making `Cursor` Flow-based (async chunk fetching)
- Making `Storable.value` private, accessed via `suspend fun resolve()` / `operator invoke()`
- Merging `LazyRef` into `Ref` (lazy by default, suspend resolver)

**Key finding:** No domain models currently use `Ref<T>` or `LazyRef<T>` fields. This makes the Ref changes low-risk.

---

## Part 1: Flow-based Cursor — ✅ DONE (2026-04-07)

**Shipped:** Cursor interface migrated from `Iterable<T>` to Flow-based. KarangoCursor rewritten with non-blocking
chunk pagination. 29 suspend convenience extensions added (map, filter, first, forEach, groupBy, sortedBy, etc.)
so consumers use the same API as stdlib collections. ~22 files changed, all tests pass.

### New Cursor interface

**File:** `ultra/vault/src/jvmMain/kotlin/Cursor.kt`

```kotlin
interface Cursor<T> {
    val count: Long
    val fullCount: Long?
    val timeMs: Double
    val entityCache: EntityCache
    val query: TypedQuery<T>

    /** Async iteration via Flow. Fetches chunks lazily on demand. */
    fun asFlow(): Flow<T>

    /** Collect all items. Convenience for asFlow().toList(). */
    suspend fun toList(): List<T> = asFlow().toList()
}
```

Removes `Iterable<T>` — no sync iteration, no blocking path.

Flow gives us `map`, `filter`, `take`, `first`, `toList`, `drop`, `flatMapConcat`, etc. — all suspend-native. Sequence
won't work because `sequence {}` uses `@RestrictsSuspension` which forbids arbitrary suspend calls like
`nextBatch().await()`.

### KarangoCursor — Flow emits items chunk-by-chunk

**File:** `karango/core/src/main/kotlin/cursor.kt`

```kotlin
class KarangoCursor<T>(
    private val itemFlow: Flow<T>,
    override val query: AqlTypedQuery<T>,
    override val entityCache: EntityCache,
    private val _count: Long,
    private val _fullCount: Long?,
    private val _timeMs: Double,
) : Cursor<T>, Closeable {

    companion object {
        fun <T> create(
            arangoCursor: ArangoCursorAsync<*>,
            query: AqlTypedQuery<T>,
            codec: KarangoCodec,
            profiler: QueryProfiler.Entry,
        ): KarangoCursor<T> {
            // Metadata from first response — available immediately
            val count = arangoCursor.count?.toLong() ?: 0L
            val fullCount = arangoCursor.extra?.stats?.fullCount

            val itemFlow = flow {
                var cursor = arangoCursor
                while (true) {
                    for (raw in cursor.result) {
                        emit(deserialize(raw, codec, profiler))
                    }
                    if (!cursor.hasMore()) break
                    cursor = cursor.nextBatch().await()  // suspend, non-blocking!
                }
            }

            return KarangoCursor(itemFlow, query, codec.entityCache, count, fullCount, ...)
        }
    }

    override val count get() = _count
    override val fullCount get() = _fullCount
    override val timeMs get() = _timeMs

    override fun asFlow(): Flow<T> = itemFlow
    override fun close() { /* no-op */
    }
}
```

### MonkoCursor — trivial Flow wrapper

**File:** `monko/core/src/main/kotlin/MonkoCursor.kt`

```kotlin
class MonkoCursor<T>(
    val entries: List<T>,
    override val query: TypedQuery<T>,
    override val entityCache: EntityCache,
    private val _fullCount: Long? = null,
    private val _timeMs: Double = 0.0,
) : Cursor<T> {
    override val count get() = entries.size.toLong()
    override val fullCount get() = _fullCount
    override val timeMs get() = _timeMs

    override fun asFlow(): Flow<T> = entries.asFlow()
}
```

### Cursor companion factories

Update `Cursor.empty()` and `Cursor.of()` to return Flow-based implementations.

### Usage in API handlers — before and after

```kotlin
// BEFORE:
val cursor = repo.findAll()
val paged = Paged(
    items = cursor.map { it.toApiModel() },   // Iterable.map
    fullItemCount = cursor.fullCount,
)

// AFTER:
val cursor = repo.findAll()
val paged = Paged(
    items = cursor.asFlow().map { it.toApiModel() }.toList(),  // Flow.map
    fullItemCount = cursor.fullCount,
)
```

### Files for Part 1

| File                                                  | Change                                   |
|-------------------------------------------------------|------------------------------------------|
| `ultra/vault/src/jvmMain/kotlin/Cursor.kt`            | Remove Iterable, add asFlow() + toList() |
| `karango/core/src/main/kotlin/cursor.kt`              | Flow-based chunk iteration               |
| `karango/core/src/main/kotlin/vault/KarangoDriver.kt` | Adapt cursor creation                    |
| `monko/core/src/main/kotlin/MonkoCursor.kt`           | Add asFlow() via entries.asFlow()        |
| All API handlers consuming cursors                    | `.map {}` → `.asFlow().map {}.toList()`  |

---

## Part 2: Storable hierarchy — suspend resolve(), private value

### New Storable base

**File:** `ultra/vault/src/jvmMain/kotlin/domain.kt`

```kotlin
sealed class Storable<out T> {
    abstract val _id: String
    abstract val _key: String
    abstract val _rev: String

    /** Resolves the wrapped value. Instant for Stored/New, may suspend for Ref. */
    abstract suspend fun resolve(): T

    /** Shorthand for resolve(). */
    suspend operator fun invoke(): T = resolve()

    // Existing identity methods stay: hasSameIdAs, hasOtherIdThan, hasIdIn
    // Existing conversions stay: asRef, asStored, collection
}
```

### Stored — instant resolve, private value

```kotlin
data class Stored<out T>(
    @PublishedApi internal val _value: T,
    override val _id: String,
    override val _key: String = _id.ensureKey,
    override val _rev: String = "",
) : Storable<T>() {
    override suspend fun resolve(): T = _value

    override fun <X : @UnsafeVariance T> modify(fn: (oldValue: T) -> X): Stored<X> =
        withValue(fn(_value))
    override fun <X : @UnsafeVariance T> withValue(newValue: X): Stored<X> =
        Stored(newValue, _id, _key, _rev)
    override fun <N> transform(fn: (current: T) -> N): Stored<N> =
        Stored(fn(_value), _id, _key, _rev)
}
```

`@PublishedApi internal` allows inline functions within the module to access `_value` while hiding it from external
consumers.

### New — same pattern

```kotlin
data class New<out T>(
    @PublishedApi internal val _value: T,
    override val _id: String = "",
    override val _key: String = "",
    override val _rev: String = "",
) : Storable<T>() {
    override suspend fun resolve(): T = _value
    // ... transform methods same as Stored
}
```

### Ref — lazy, suspend resolver, replaces LazyRef

```kotlin
class Ref<out T>(
    override val _id: String,
    private val resolver: suspend () -> Storable<T>,
) : Storable<T>() {

    companion object {
        const val SERIAL_NAME = "ref"

        /** Wrap an already-loaded value. Used by .asRef, tests, save paths. */
        fun <T> eager(value: T, _id: String, _key: String, _rev: String): Ref<T> {
            val stored = Stored(value, _id, _key, _rev)
            return Ref(_id) { stored }
        }

        /** Create a lazy ref. Used by RefCodec during deserialization. */
        fun <T> lazy(_id: String, resolver: suspend () -> Storable<T>): Ref<T> =
            Ref(_id, resolver)
    }

    private var _cached: Storable<T>? = null

    override suspend fun resolve(): T {
        _cached?.let { return it.resolve() }
        val result = resolver()
        _cached = result
        return result.resolve()
    }

    override val _key: String get() = _id.ensureKey
    override val _rev: String get() = _cached?._rev ?: ""

    // Equality on _id only
    override fun equals(other: Any?): Boolean =
        this === other || (other is Ref<*> && _id == other._id)
    override fun hashCode(): Int = _id.hashCode()
    override fun toString(): String = "Ref(_id=$_id)"

    // Transform methods — return lazy Refs that resolve on demand
    override fun <X : @UnsafeVariance T> withValue(newValue: X): Ref<X> =
        eager(newValue, _id, _key, _rev)
    override fun <X : @UnsafeVariance T> modify(fn: (oldValue: T) -> X): Ref<X> =
        Ref(_id) { Stored(fn(resolve()), _id, _key, _rev) }
    override fun <N> transform(fn: (current: T) -> N): Ref<N> =
        Ref(_id) { Stored(fn(resolve()), _id, _key, _rev) }
}
```

**`LazyRef` is deleted entirely.**

### Storable conversion updates

```kotlin
// On Storable:
val asRef: Ref<T> get() = Ref.eager(resolve_internal(), _id, _key, _rev)
val asStored: Stored<T> get() = Stored(resolve_internal(), _id, _key, _rev)
// asLazyRef — removed

// resolve_internal() is a non-suspend internal accessor for Stored/New
// (they always have the value; Ref.asRef/asStored would need suspend)
```

Note: `asRef` and `asStored` on `Ref` itself may need to become suspend functions. This needs careful handling during
implementation — likely `suspend fun toRef()` / `suspend fun toStored()` as replacements.

### Files for Part 2

| File                                                            | Change                                          |
|-----------------------------------------------------------------|-------------------------------------------------|
| `ultra/vault/src/jvmMain/kotlin/domain.kt`                      | Rewrite Storable/Stored/New/Ref, delete LazyRef |
| `ultra/vault/src/jvmMain/kotlin/Repository.kt`                  | Remove `is LazyRef` branch, adapt value access  |
| `ultra/vault/src/jvmMain/kotlin/helpers.kt`                     | Adapt any value access                          |
| `ultra/vault/src/jvmMain/kotlin/slumber/StoredAwaker.kt`        | Use _value via internal access                  |
| `ultra/vault/src/jvmMain/kotlin/slumber/StoredSlumberer.kt`     | Use internal access                             |
| `ultra/vault/src/jvmTest/kotlin/StorableSpec.kt`                | Update tests for new API                        |
| `funktor/core/src/jvmMain/kotlin/fixtures/RepoFixtureLoader.kt` | Remove asLazyRef()                              |
| All code using `.value` on Storable                             | Migrate to `()` or `.resolve()`                 |

---

## Part 3: Unified RefCodec — zero runBlocking

**File:** `ultra/vault/src/jvmMain/kotlin/slumber/RefCodec.kt`

```kotlin
object RefCodec : Awaker, Slumberer {

    override fun awake(data: Any?, context: Awaker.Context): Any? {
        if (data !is String) return null
        val database = context.attributes[VaultSlumberModule.DatabaseKey] ?: return null
        val cache = context.attributes[VaultSlumberModule.EntityCacheKey] ?: NullEntityCache
        val coll = data.split("/").first()

        // Lazy — the suspend resolver is stored, NOT executed during awake()
        return Ref.lazy(_id = data) {
            cache.getOrPutAsync(data) {
                database.getRepository(coll).findById(data)  // suspend, no runBlocking!
            } ?: throw VaultException("Referenced entity not found: $data")
        }
    }

    override fun slumber(data: Any?, context: Slumberer.Context): Any? = when (data) {
        is Ref<*> -> data._id
        else -> null
    }
}
```

The resolver lambda is `suspend` — it calls `findById()` directly. Resolution happens when the user calls `ref()` or
`ref.resolve()`, always in a suspend context. **Zero `runBlocking` anywhere.**

### Files for Part 3

| File                                                           | Change                                        |
|----------------------------------------------------------------|-----------------------------------------------|
| `ultra/vault/src/jvmMain/kotlin/slumber/RefCodec.kt`           | Merge LazyRefCodec behavior, suspend resolver |
| `ultra/vault/src/jvmMain/kotlin/slumber/LazyRefCodec.kt`       | **Delete**                                    |
| `ultra/vault/src/jvmMain/kotlin/slumber/VaultSlumberModule.kt` | Remove LazyRef routing                        |

---

## Part 4: EntityCache suspend variant

**File:** `ultra/vault/src/jvmMain/kotlin/caching.kt`

```kotlin
interface EntityCache {
    fun clear()
    fun <T> put(id: String, value: T): T
    fun <T> getOrPut(id: String, provider: () -> T?): T?

    /** Suspend variant for async resolution paths (RefCodec). */
    suspend fun <T> getOrPutAsync(id: String, provider: suspend () -> T?): T?
}
```

`DefaultEntityCache` uses `Mutex` for the async variant:

```kotlin
class DefaultEntityCache : EntityCache {
    private val mutex = Mutex()

    override suspend fun <T> getOrPutAsync(id: String, provider: suspend () -> T?): T? =
        mutex.withLock {
            @Suppress("UNCHECKED_CAST")
            (entries[id] as? T) ?: provider()?.also { entries[id] = it }
        }
}
```

---

## Execution Order

1. **Part 1** (Flow Cursor) + **Part 2** (Storable hierarchy) — can start in parallel
2. **Part 4** (EntityCache async) — needed before Part 3
3. **Part 3** (RefCodec merge) — builds on Parts 2 + 4

---

## Verification

1. `./gradlew :ultra:vault:jvmTest` — Storable, Ref, EntityCache, Cursor interface
2. `./gradlew :karango:core:test` — KarangoCursor Flow
3. `./gradlew :monko:core:test` — MonkoCursor asFlow()
4. `./gradlew build` — full build, no regressions
5. Grep `LazyRef` — zero hits
6. Grep `runBlocking` in karango/core and ultra/vault/slumber — zero hits
7. Grep `\.value` on Storable types — zero public access (all migrated to `()` / `resolve()`)

---

## What this does NOT change

- **Codec.awake()** stays synchronous — RefCodec stores a suspend lambda, doesn't execute it during awake()
- **MonkoCursor internals** — already list-based, just adds `asFlow()` wrapper
- **Slumber framework** — no suspend Awaker rewrite needed
