# Cache — In-Memory Caching

A coroutine-based in-memory cache with pluggable eviction policies for Kotlin Multiplatform.

- Package: `io.peekandpoke.ultra:cache`
- Platforms: JVM, JS
- Docs: https://peekandpoke.io/ultra/cache/

## Quick Start

```kotlin
import io.peekandpoke.ultra.cache.fastCache
import kotlin.time.Duration.Companion.minutes

val cache = fastCache<String, UserProfile> {
    expireAfterAccess(5.minutes)
    maxEntries(1000)
}

cache.put("key", value)
val result = cache.get("key")
val computed = cache.getOrPut("key") { expensiveComputation() }
```

## Cache Interface

| Method                  | Returns     | Behavior                   |
|-------------------------|-------------|----------------------------|
| `get(key)`              | `V?`        | Returns value or null      |
| `put(key, value)`       | `Unit`      | Stores, replacing existing |
| `remove(key)`           | `V?`        | Removes and returns        |
| `has(key)`              | `Boolean`   | Key existence check        |
| `getOrPut(key) { ... }` | `V`         | Compute on miss            |
| `clear()`               | `Unit`      | Remove all entries         |
| `size`                  | `Int`       | Entry count                |
| `keys`                  | `Set<K>`    | Snapshot of keys           |
| `values`                | `List<V>`   | Snapshot of values         |
| `entries`               | `Map<K, V>` | Immutable snapshot         |

## Eviction Policies

Three pluggable behaviors — use one, two, or all three:

### Expire After Access (TTL)

Evicts entries not read or written within a TTL window. Timer resets on every access.

```kotlin
val cache = fastCache<String, String> {
    expireAfterAccess(5.minutes)
}
```

### Max Entries (LRU)

Caps entry count. Least-recently-accessed entries evicted first.

```kotlin
val cache = fastCache<String, String> {
    maxEntries(100)
}
```

### Max Memory Usage

Caps estimated memory. Uses `ObjectSizeEstimator` to recursively measure object graphs. LRU eviction when budget
exceeded.

```kotlin
val cache = fastCache<String, MyData> {
    maxMemoryUsage(
        maxMemorySize = 50 * 1024 * 1024,  // 50 MB
        estimator = ObjectSizeEstimator(),
    )
}
```

### Combining Behaviors

```kotlin
val cache = fastCache<String, ExpensiveResult> {
    expireAfterAccess(10.minutes)
    maxEntries(5000)
    maxMemoryUsage(100 * 1024 * 1024, ObjectSizeEstimator())
}
```

## Architecture

- **Coroutine-based eviction** — background loop processes evictions asynchronously (default: 50ms interval). Reads and
  writes never pay for eviction on the calling thread.
- **Thread-safe** — all operations synchronized internally. Safe for concurrent coroutine access.
- **WeakReference cleanup** — the eviction loop holds a weak reference to the cache, allowing GC if the cache is
  abandoned.
- **Structured concurrency** — pass a custom `CoroutineScope` for lifecycle control.

```kotlin
val scope = CoroutineScope(Dispatchers.Default)
val cache = fastCache<String, String>(scope = scope) {
    expireAfterAccess(5.minutes)
}
// When scope is cancelled, eviction loop stops
```

## Object Size Estimation

The `ObjectSizeEstimator` walks object graphs recursively:

- Object headers: 16 bytes (64-bit HotSpot)
- Primitives: 1-8 bytes
- Strings: header + char array
- Collections/Maps: header + pointers + elements
- Cycle detection via WeakSet
- Platform-specific: JVM uses reflection, JS uses dynamic property enumeration

## Multiplatform

Works in `commonMain`, `jvmMain`, and `jsMain`. Platform differences are limited to the object size estimation
implementation.
