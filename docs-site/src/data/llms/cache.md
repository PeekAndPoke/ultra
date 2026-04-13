# Cache — In-Memory Caching

A coroutine-based in-memory cache with composable behaviours for Kotlin Multiplatform.

- Package: `io.peekandpoke.ultra:cache`
- Platforms: JVM, JS
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

```kotlin
val cache = fastCache<String, String> {
    expireAfterAccess(5.minutes)
}
```

### Expire After Write (TTL)

Evicts entries not written within a TTL window. Reads do NOT reset the timer.

```kotlin
val cache = fastCache<String, String> {
    expireAfterWrite(5.minutes)
}
// Use for: API tokens, DNS lookups, config values, exchange rates
```

### Max Entries (LRU)

Caps entry count. Least-recently-accessed entries evicted first.

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
        maxMemorySize = 50 * 1024 * 1024,
        estimator = ObjectSizeEstimator(),
    )
}
```

### Combining Behaviours

```kotlin
val cache = fastCache<String, ExpensiveResult> {
    expireAfterWrite(10.minutes)
    maxEntries(5000)
    maxMemoryUsage(100 * 1024 * 1024, ObjectSizeEstimator())
}
```

## Background Refresh (stale-while-revalidate)

Serves stale values while refreshing in the background. No reader ever sees a miss.

```kotlin
val cache = fastCache<String, UserProfile> {
    refreshAfterWrite(ttl = 5.minutes) { key ->
        userService.fetchProfile(key)  // suspend function, runs in background
    }
}
```

Features:

- **Deduplication**: only one refresh per key at a time
- **Hard TTL**: optional `hardTtl` parameter evicts entries past a hard deadline
- **Error retry**: failed loaders retry on the next loop iteration (~50ms)
- **Scope integration**: refresh coroutines run in the cache's `CoroutineScope`

```kotlin
val cache = fastCache<String, String> {
    refreshAfterWrite(
        ttl = 5.minutes,
        hardTtl = 30.minutes,  // evict if refresh can't complete in 30 min
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

Tracks hits, misses, puts, and evictions. Returns a handle for calling `snapshot()`.

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
  abandoned.
- **Structured concurrency** — pass a custom `CoroutineScope` for lifecycle control.
- **MissAction tracking** — cache misses are recorded as actions, enabling accurate statistics.

```kotlin
val scope = CoroutineScope(Dispatchers.Default)
val cache = fastCache<String, String>(scope = scope) {
    expireAfterAccess(5.minutes)
}
// When scope is cancelled, eviction loop stops
```

## Multiplatform

Works in `commonMain`, `jvmMain`, and `jsMain`. Platform differences are limited to the object size estimation
implementation (JVM: reflection, JS: dynamic property enumeration).
