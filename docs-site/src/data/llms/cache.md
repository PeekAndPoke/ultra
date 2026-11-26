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

A TTL counts from the loop pass that sees the access (for `expireAfterWrite` and the refresh's `hardTtl`: the write),
so an entry can be served up to its TTL plus two loop periods.
TTLs run on a monotonic clock, not the system time (suspended time may not count); `Builder.timeSource(...)` swaps it
for tests. Odd values never throw: a zero or negative TTL expires at the next pass, below 1 ms counts as 1 ms,
`Duration.INFINITE` never expires; a loop delay below 1 ms runs as 1 ms. `maxEntries` or `maxMemoryUsage` below 1
throws `IllegalArgumentException` when the builder adds the behaviour.

```kotlin
val cache = fastCache<String, String>(loopDelay = 10.milliseconds) {  // default 50 ms
    expireAfterAccess(1.seconds)
}
```

```kotlin
val cache = fastCache<String, String> {
    expireAfterAccess(5.minutes)
}
```

### Expire After Write (TTL)

Evicts entries not written within a TTL window. Reads do NOT reset the timer; a write (`put`, `getOrPut` on a miss, a stored
refresh result) does. A key read in every pass still expires on time: only a pending write spares it.

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

Caps the sum of the estimated sizes of keys and values; over budget, least recently accessed entries go first.

```kotlin
val cache = fastCache<String, MyData> {
    maxMemoryUsage(50L * 1024 * 1024)  // default estimator: ObjectSizeEstimator()
}
```

- Sized on writes only: the estimator runs once for the key and once for the value, on the maintenance loop, outside
  the cache's lock, once per written key and pass. Reads reuse the size; a value changed in place keeps its old size.
- An entry whose own size exceeds `maxMemorySize` is evicted alone at the next pass and never counted; the others
  stay. A read does not spare it, only a newer pending write (sized anew). Such a value is never cached, also a small
  one whose estimate is inflated.
- Soft bound like `maxEntries`; sizes saturate at `Long.MAX_VALUE`, never negative.

#### The default estimator

Counts primitives by size; a string as header + reference + array header + 2 B per char; arrays as header +
elements; a collection as header + one reference per element (a map two) + the elements; any other object as header +
one reference per field + the fields (JVM: reflection, JS: own properties, native: a flat 32 B). An object reached
twice in one estimate counts once, except (errs high): past the cap an object not walked before is charged flat each
time, and strings and primitive arrays after the first `maxObjects` skip the identity check. Bounded cost, configured
by `EstimatorConfig`:

- Breadth first, at most `maxObjects` (10 000) objects visited; strings and primitive arrays are sized from their
  length and do not count. Past the cap an object is charged 32 B (header + two references), unvisited.
- More than `sampleSize` (64) elements or fields: up to 64 fixed positions (`SamplePick.GoldenRatio` by default,
  `EvenlySpaced` the alternative), the rest charged by their average. Without index access (set, linked list, map):
  over the first `sampleWindow` (1 024) elements only. Deterministic.
- Never throws: a part whose walk fails is charged flat and counted in the loop's failure line
  `FastCache size estimate (part charged flat) failed N time(s): <class>`.
- Work: at most 10 000 visits, each handing on at most 64 elements (a map 64 keys and 64 values), at most 1 024
  iterator steps per collection. JVM: 10 000 strings 0.03 ms, a million strings in nested lists 15 to 35 ms. Not in
  that bound: listing a JS object's own properties, a linear `size` (concurrent queue), hash tables iterated over
  their empty slots (a once huge `HashMap`), user code in `size`/`get`/`iterator`/getters (lazy ORM collections).

It is an estimate. Which way it errs:

| Where          | Errs       | Why                                                                                          |
|----------------|------------|----------------------------------------------------------------------------------------------|
| Sampling       | either way | a sample counts about n/64 times; nested sampled levels multiply (3 levels of 1 000: up to about 3 800x); sets, maps, linked lists unseen past 1 024 |
| Shared objects | high       | reached via sampled elements, counted with their weight (7.6x measured); may count again past the cap or after the first 10 000 strings and primitive arrays; key, value and other entries sized separately |
| Cap            | low        | objects past it are 32 B whatever they hold (150 000 objects: 0.63); a cut empty collection up to 16 B high |
| Reachability   | high       | everything reachable counts, up to the cap (a service reference, a coroutine `Job`)          |
| JVM            | low        | map entry about 48 B real, 16 charged; boxed elements raw size; JPMS-closed types (`Instant`, `UUID`, `BigDecimal`) an empty object |
| JVM            | high       | assumes 8 B references, 16 B headers (HotSpot compressed: 4 and 12), 2 B per char (Latin-1: 1) |
| JS             | mixed      | every number 8 B; equal strings once; a JS `Map`/`Set` an empty object; `ArrayBuffer`, typed array, `DataView`: array header + `byteLength`; `Blob`: object header + `size`; getters and `Proxy` traps run |
| Native         | either way | any object other than string, array, collection, map: a flat 32 B                           |
| Writes only    | either way | a value changed in place keeps its old size                                                  |

**With the default estimator `maxMemoryUsage` is not a memory bound for data an attacker shapes**: fixed sample
positions can be steered far too high (above the whole budget never cached; below it cached, crowding out other
entries) or far too low (large elements off the samples, or past a set's first 1 024).

#### Your own estimator

`fun interface ObjectSizeEstimator { fun estimate(obj: Any?): Long }`. For untrusted data: a size the app already
knows (payload length, serialized byte count) plus `maxEntries`.

```kotlin
class Rendered(val html: String, val byteCount: Int)

val cache = fastCache<String, Rendered> {
    maxEntries(10_000)
    maxMemoryUsage(64L * 1024 * 1024) { obj ->
        when (obj) {
            is Rendered -> 64L + obj.byteCount   // value: a known size
            is String -> 40L + 2L * obj.length   // key
            else -> 0L                           // null
        }
    }
}
```

Contract: called separately for key and value, each `Any?` (handle `null`); on writes only (and a read of an entry not
sized yet); on the maintenance loop, so keep it cheap; thread-safe if shared by several caches. It may throw: counted
in the failure line, a new entry is charged `fallbackSize` (default 1 024; zero or below counts as the default), an
existing one keeps its size. A negative result counts as 0.

Also: `maxMemoryUsage(bytes, estimator, fallbackSize)`, `addBehaviour(FastCache.MaxMemoryUsageBehaviour(bytes,
estimator, fallbackSize))`. No (key, value) weigher; `ObjectSizeEstimatorImpl` is final; a lambda delegating to it
loses the per-part failure count.

Tune the default (more samples, a larger cap: more accuracy, more time; below 1 counts as 1, a window below the
sample size as the sample size):

```kotlin
val estimator = ObjectSizeEstimator(
    ObjectSizeEstimatorImpl.EstimatorConfig(
        objectHeader = 16L, arrayHeader = 24L, pointerSize = 8L,  // defaults
        sampleSize = 64, sampleWindow = 1_024, maxObjects = 10_000,
        samplePick = ObjectSizeEstimatorImpl.SamplePick.GoldenRatio,
    )
)
```

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
  dropped without notice; the compare is by identity, so writing back the same instance still lets the refresh replace
  it (readers get the loaded value until the next write or refresh); for booleans, enums, small boxed numbers,
  interned strings and JS primitives any equal value counts as the same instance
- **Counts as a put**: the result is an access for `expireAfterAccess` and the LRU order, and a put in the statistics
  (an open question, may change)
- **Hard TTL**: optional `hardTtl` evicts entries past a hard deadline, also a key read in every pass; a zero or
  negative `ttl`, or a `hardTtl` not above `ttl`, expires entries unrefreshed
- **Error retry**: a failed load keeps the stale value and retries every loop pass (~50ms) until it succeeds or the
  entry is gone (hard TTL, another behaviour's eviction, `remove()`); failures print as
  `FastCache refresh failed N time(s): <class>`; a `CancellationException` (also `withTimeout`'s) is retried but not
  counted
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
