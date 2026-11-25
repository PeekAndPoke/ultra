# Cache — In-Memory Caching

A coroutine-based in-memory cache with composable behaviours for Kotlin Multiplatform.

- Package: `io.peekandpoke.ultra:cache`
- Platforms: JVM, JS, Native (linuxX64, linuxArm64, macosArm64, mingwX64)
- Docs: https://peekandpoke.io/ultra/cache/

## Quick Start

```kotlin
import io.peekandpoke.ultra.cache.fastCache
import kotlin.time.Duration.Companion.minutes

val cache = fastCache<String, UserProfile> {
    expireAfterWrite(10.minutes)
    maxEntries(1000)
    onEviction { key, _ -> logger.debug("evicted $key") }
}

cache.put("key", value)
val result = cache.get("key")
val computed = cache.getOrPut("key") { expensiveComputation() }
```

## Cache Interface

`FastCache` implements `Cache<K, V>`:

| Method                  | Returns     | Behavior                                    |
|-------------------------|-------------|---------------------------------------------|
| `get(key)`              | `V?`        | Returns value or null (records hit or miss) |
| `put(key, value)`       | `Unit`      | Stores, replacing existing                  |
| `remove(key)`           | `V?`        | Removes and returns                         |
| `has(key)`              | `Boolean`   | Key existence check (records hit or miss)   |
| `getOrPut(key) { ... }` | `V`         | Compute on miss                             |
| `clear()`               | `Unit`      | Remove all entries                          |
| `size`                  | `Int`       | Entry count                                 |
| `keys`                  | `Set<K>`    | Snapshot of keys                            |
| `values`                | `List<V>`   | Snapshot of values                          |
| `entries`               | `Map<K, V>` | Immutable snapshot                          |

## Behaviour Matrix

| Behaviour           | Builder DSL                                       | Trigger                          | TTL resets on       |
|---------------------|---------------------------------------------------|----------------------------------|---------------------|
| `expireAfterAccess` | `expireAfterAccess(5.minutes)`                    | No read/write within TTL         | Read + Write        |
| `expireAfterWrite`  | `expireAfterWrite(5.minutes)`                     | No write within TTL              | Write only          |
| `maxEntries`        | `maxEntries(1000)`                                | Entry count exceeds limit        | n/a (LRU by access) |
| `maxMemoryUsage`    | `maxMemoryUsage(bytes, estimator)`                | Estimated bytes exceed budget    | n/a (LRU by access) |
| `refreshAfterWrite` | `refreshAfterWrite(ttl, hardTtl?) { key -> ... }` | Soft: async reload; Hard: evicts | Write only          |
| `onEviction`        | `onEviction { key, value -> ... }`                | Observer only                    | n/a                 |
| `statistics`        | `statistics()`                                    | Observer only                    | n/a                 |

## Eviction Behaviours

### Expire After Access (TTL)

Evicts entries not read or written within a TTL window. Timer resets on every access.

The TTL counts from the loop pass that sees the access, so an entry can be served up to its TTL plus two loop periods.
TTLs run on a monotonic clock, not the system time.

```kotlin
val cache = fastCache<String, String> {
    expireAfterAccess(5.minutes)
}
```

### Expire After Write (TTL)

Evicts entries not written within a TTL window. Reads do NOT reset the timer; a write (`put`, `getOrPut` on a miss)
does.

```kotlin
val cache = fastCache<String, String> {
    expireAfterWrite(5.minutes)
}
// Use for: API tokens, DNS lookups, config values, exchange rates
```

### Max Entries (LRU)

Caps entry count. Least-recently-accessed entries evicted first, by the next loop pass.

`maxEntries` and `maxMemoryUsage` are soft bounds: an entry read or written in every loop pass is not evicted by them,
so under such load the cache holds up to the number of distinct keys touched within one pass.

```kotlin
val cache = fastCache<String, String> {
    maxEntries(100)
}
```

### Max Memory Usage

Caps estimated memory. Uses `ObjectSizeEstimator` to recursively measure object graphs.

```kotlin
val cache = fastCache<String, MyData> {
    maxMemoryUsage(
        maxMemorySize = 50L * 1024 * 1024,
        estimator = ObjectSizeEstimator(),
    )
}
```

The estimate is a pressure signal, not a bound, and its error differs per platform: native has no reflection and
charges any object other than a string, array, collection or map a flat 32 bytes; JS charges every number 1 byte; the
JVM under-counts maps several-fold and charges types whose fields JPMS keeps closed (`Instant`, `UUID`, `BigDecimal`)
16 bytes. `ObjectSizeEstimator` is an interface (`estimate(obj: Any?): Long`); pass your own where the number has to
mean something.

### Combining Behaviours

```kotlin
val cache = fastCache<String, ExpensiveResult> {
    expireAfterWrite(10.minutes)
    maxEntries(5000)
    maxMemoryUsage(100L * 1024 * 1024, ObjectSizeEstimator())
}
```

## Background Refresh (stale-while-revalidate)

Serves stale values while refreshing in the background. Readers never wait for the loader and see no gap, unless
a `hardTtl` or another behaviour evicts the entry.

```kotlin
val cache = fastCache<String, UserProfile> {
    refreshAfterWrite(ttl = 5.minutes) { key ->
        userService.fetchProfile(key)  // suspend function, runs in background
    }
}
```

Features:

- **Deduplication**: no second refresh for a key while one is in flight, unless the key is written meanwhile: the write
  frees it, so a later refresh can run beside the older one
- **Only the value it started from**: if the entry was written, removed or evicted while the loader ran, the result is
  dropped without notice; the compare is by identity, so writing back the same instance still lets the refresh replace it
- **Counts as a put**: the result is an access for `expireAfterAccess` and the LRU order, and a put in the statistics
- **Hard TTL**: optional `hardTtl` parameter evicts entries past a hard deadline
- **Error retry**: a failed load keeps the stale value and retries on the next loop iteration (~50ms)
- **Scope integration**: refresh coroutines run in the cache's `CoroutineScope`

```kotlin
val cache = fastCache<String, String> {
    refreshAfterWrite(
        ttl = 5.minutes,
        hardTtl = 30.minutes,  // evict 30 min after the last write
    ) { key -> fetchFromApi(key) }
}
```

## Observability

### onEviction

Callback fired when entries are evicted by behaviours. Does NOT fire on explicit `remove()`.

```kotlin
val cache = fastCache<String, Connection> {
    expireAfterAccess(5.minutes)
    onEviction { key, value ->
        value.close()
        logger.info("Evicted: $key")
    }
}
```

### statistics

Tracks hits, misses, puts (refresh results included), and evictions. Returns a handle for calling `snapshot()`.
Counting happens in the background loop: an operation shows in a snapshot after the next loop pass.

```kotlin
lateinit var stats: FastCache.StatisticsBehaviour<String, String>

val cache = fastCache<String, String> {
    stats = statistics()
}

val snapshot = stats.snapshot()
snapshot.hitCount       // Long
snapshot.missCount      // Long
snapshot.putCount       // Long
snapshot.evictionCount  // Long
snapshot.requestCount   // Long (hits + misses)
snapshot.hitRate        // Double (0.0..1.0, NaN if no requests)
```

## Architecture

- **Coroutine-based eviction** — background loop processes evictions asynchronously (default: 50ms interval). Reads and
  writes never pay for eviction on the calling thread.
- **Thread-safe** — all operations synchronized internally. Safe for concurrent coroutine access.
- **WeakReference cleanup** — the eviction loop holds a weak reference to the cache, allowing GC if the cache is
  abandoned; the loop then ends.
- **Structured concurrency** — pass a custom `CoroutineScope` for lifecycle control. The default,
  `Cache.defaultCoroutineScope`, is one scope on `Dispatchers.Default` shared by all caches.
- **MissAction tracking** — cache misses are recorded as actions, enabling accurate statistics.

```kotlin
val scope = CoroutineScope(Dispatchers.Default)
val cache = fastCache<String, String>(scope = scope) {
    expireAfterAccess(5.minutes)
}
// When scope is cancelled, eviction loop stops
```

## Multiplatform

Works in `commonMain`, `jvmMain`, `jsMain` and `nativeMain`. Platform differences are limited to the object size
estimation implementation (JVM: reflection, JS: dynamic property enumeration, native: none, a flat charge per object).
