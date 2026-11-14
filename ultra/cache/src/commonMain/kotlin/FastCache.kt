package io.peekandpoke.ultra.cache

import io.peekandpoke.ultra.common.RunSync
import io.peekandpoke.ultra.common.WeakReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * Creates a [FastCache] using the DSL-style [configure] block.
 *
 * @param scope       the [CoroutineScope] for the internal processing loop
 * @param loopDelay   the delay between processing-loop iterations; below 1 ms it runs as 1 ms
 * @param configure   builder DSL to add behaviours such as TTL, max-entries or memory limits
 * @return a configured [FastCache] instance
 */
fun <K, V> fastCache(
    scope: CoroutineScope = Cache.defaultCoroutineScope,
    loopDelay: Duration = FastCache.defaultLoopDelay,
    configure: FastCache.Builder<K, V>.() -> Unit = {},
): FastCache<K, V> {
    val builder = FastCache.Builder<K, V>(scope = scope, loopDelay = loopDelay)

    return builder.apply(configure).build()
}

/**
 * This TTL in whole milliseconds, the resolution of the cache's clock.
 *
 * Never throws; each odd value has a fallback. Zero or negative is 0: "do not keep", the entry expires
 * at the next processing pass. A positive TTL below 1 ms is 1 ms: truncated to 0 it would mean
 * "do not keep" although a positive TTL was asked for. [Duration.INFINITE] never expires.
 */
private fun Duration.ttlMillis(): Long = when {
    isPositive() -> inWholeMilliseconds.coerceAtLeast(1L)
    else -> 0L
}

/**
 * Cache implementation that focuses on performance.
 *
 * The cache can use a number of [behaviours].
 * There is an internal processing loop that periodically invokes the behaviours.
 *
 * This means f.e. that the [Builder.maxMemoryUsage] behaviour will not check after every insert whether the cache is
 * over the limit and something needs to be evicted. Instead, the loop will check the memory usage periodically and
 * evict entries until the memory usage is below the limit.
 */
class FastCache<K, V> internal constructor(
    /** The coroutine scope used for the internal processing loop. */
    val scope: CoroutineScope,
    /** The list of behaviours applied during each processing-loop iteration. */
    val behaviours: List<Behaviour<K, V>>,
    /**
     * The delay between processing-loop iterations, as given.
     *
     * Below 1 ms, zero and negative included, the loop runs with 1 ms: without a pause it would
     * never suspend and hold a thread for good.
     */
    val loopDelay: Duration,
    /** The clock of every TTL: monotonic, so a step of the wall clock neither expires nor keeps an entry. */
    timeSource: TimeSource,
) : Cache<K, V> {

    /** A cache on the monotonic clock - see [Builder.timeSource] for another. */
    constructor(
        scope: CoroutineScope = Cache.defaultCoroutineScope,
        behaviours: List<Behaviour<K, V>>,
        loopDelay: Duration = defaultLoopDelay,
    ) : this(scope, behaviours, loopDelay, TimeSource.Monotonic)

    /** Where [millisNow] counts from. */
    private val clockOrigin = timeSource.markNow()

    /** Milliseconds on this cache's clock: the time base of every TTL stamp and comparison. */
    internal fun millisNow(): Long = clockOrigin.elapsedNow().inWholeMilliseconds

    companion object {
        /** Default delay between processing-loop iterations. */
        val defaultLoopDelay = 50.milliseconds
    }

    /**
     * Builder for [FastCache].
     */
    class Builder<K, V>(
        private val scope: CoroutineScope = Cache.defaultCoroutineScope,
        private var loopDelay: Duration = defaultLoopDelay,
    ) {
        private val behaviours = mutableListOf<Behaviour<K, V>>()

        private var timeSource: TimeSource = TimeSource.Monotonic

        /**
         * Sets the clock of every TTL; the default is [TimeSource.Monotonic].
         *
         * For tests, e.g. a `TestTimeSource`: TTLs then pass only when the test moves its time on.
         */
        fun timeSource(source: TimeSource) = apply { timeSource = source }

        /** Sets the loop delay; below 1 ms it runs as 1 ms - see [FastCache.loopDelay]. */
        fun loopDelay(delay: Duration) = apply { loopDelay = delay }

        /** Adds a behaviour to the cache. */
        fun addBehaviour(behaviour: Behaviour<K, V>) = apply { behaviours.add(behaviour) }

        /**
         * Adds the [ExpireAfterAccessBehaviour] to the cache.
         *
         * Never throws: a zero or negative [ttl] expires entries at the next pass, one below 1 ms
         * counts as 1 ms, and [Duration.INFINITE] never expires.
         */
        fun expireAfterAccess(ttl: Duration) = addBehaviour(ExpireAfterAccessBehaviour(ttl))

        /**
         * Adds the [ExpireAfterWriteBehaviour] to the cache.
         *
         * Never throws: a zero or negative [ttl] expires entries at the next pass, one below 1 ms
         * counts as 1 ms, and [Duration.INFINITE] never expires.
         */
        fun expireAfterWrite(ttl: Duration) = addBehaviour(ExpireAfterWriteBehaviour(ttl))

        /**
         * Adds the [MaxEntriesBehaviour] to the cache.
         *
         * A soft bound: an entry read or written in every maintenance pass is not evicted by it, so
         * under such load the cache holds up to the number of distinct keys touched within one pass.
         */
        fun maxEntries(maxEntries: Int) = addBehaviour(MaxEntriesBehaviour(maxEntries))

        /**
         * Adds the [MaxMemoryUsageBehaviour] to the cache.
         *
         * [maxMemorySize] is a **budget in estimated bytes, not a real memory bound** — see
         * [ObjectSizeEstimator] for how far the estimate can be off, which differs per platform and
         * is worst on native, where every custom object counts as 32 bytes whatever it holds. Pass a
         * tuned [estimator], or a purpose-built one, where the number has to mean something.
         *
         * A soft bound, like [maxEntries]: an entry read or written in every maintenance pass is not
         * evicted by it, so under such load the cache holds every entry touched within one pass.
         */
        fun maxMemoryUsage(maxMemorySize: Long, estimator: ObjectSizeEstimator = ObjectSizeEstimator()) =
            addBehaviour(MaxMemoryUsageBehaviour(maxMemorySize, estimator))

        /** Adds the [OnEvictionBehaviour] that fires [handler] when entries are evicted by other behaviours. */
        fun onEviction(handler: (K, V) -> Unit) = addBehaviour(OnEvictionBehaviour(handler))

        /** Adds the [StatisticsBehaviour] and returns it for later [StatisticsBehaviour.snapshot] access. */
        fun statistics(): StatisticsBehaviour<K, V> {
            val behaviour = StatisticsBehaviour<K, V>()
            addBehaviour(behaviour)
            return behaviour
        }

        /**
         * Adds the [RefreshAfterWriteBehaviour] to the cache, refreshing entries older than [ttl].
         *
         * Never throws. A zero or negative [ttl] or [hardTtl] expires entries at the next pass,
         * unrefreshed; below 1 ms counts as 1 ms; [Duration.INFINITE] never expires; a [hardTtl] not
         * above [ttl] expires entries at [hardTtl], unrefreshed.
         */
        fun refreshAfterWrite(
            ttl: Duration,
            hardTtl: Duration? = null,
            loader: suspend (K) -> V,
        ) = addBehaviour(RefreshAfterWriteBehaviour(refreshAfter = ttl, hardTtl = hardTtl, loader = loader))

        /** Builds the [FastCache] instance. */
        fun build() = FastCache(
            scope = scope,
            // Copied: handing the builder's own list over means reusing the builder afterwards
            // mutates an already-built cache, and a behaviour added that way silently never gets
            // its eviction listener registered - that happens once, in init.
            behaviours = behaviours.toList(),
            loopDelay = loopDelay,
            timeSource = timeSource,
        )
    }

    // Actions ////////////////////////////////////////////////////////////////////////////////////////

    /** Represents a cache action recorded for deferred processing by behaviours. */
    sealed interface Action<K, V> {
        /** The key involved in this action. */
        val key: K
    }

    /** Records that a value was read from the cache. */
    class ReadAction<K, V>(
        override val key: K,
        /** The value that was read. */
        val value: V,
    ) : Action<K, V>

    /** Records that a value was inserted or updated in the cache. */
    class PutAction<K, V>(
        override val key: K,
        /** The value that was written. */
        val value: V,
    ) : Action<K, V>

    /** Records that a key was removed from the cache. */
    class RemoveAction<K, V>(override val key: K) : Action<K, V>

    /** Records that a key lookup was attempted but the key was not present. */
    class MissAction<K, V>(override val key: K) : Action<K, V>

    /**
     * A batch of [actions] collected between processing-loop iterations.
     *
     * Provides lazy groupings so behaviours can efficiently inspect the most
     * recent action per key without re-scanning the list.
     */
    data class ActionUpdates<K, V>(
        /** The recorded actions, in the order they happened. */
        val actions: List<Action<K, V>>,
    ) {
        /** Actions grouped by their key. */
        val byKey: Map<K, List<Action<K, V>>> by lazy {
            actions.groupBy { it.key }
        }

        /** The most recent action for each key. */
        val lastByKey: Map<K, Action<K, V>> by lazy {
            byKey.mapValues { (_, actions) -> actions.last() }
        }

        /**
         * The most recent action for each key that is not a read: what a write-based behaviour acts on.
         *
         * With [lastByKey], a read right after a put in the same batch hid the put from them.
         */
        internal val lastWriteByKey: Map<K, Action<K, V>> by lazy {
            val result = LinkedHashMap<K, Action<K, V>>()

            actions.forEach { action ->
                if (action !is ReadAction) {
                    result[action.key] = action
                }
            }

            result
        }
    }

    // Behaviour interface ////////////////////////////////////////////////////////////////////////////

    /**
     * A pluggable behaviour that is invoked by the processing loop to inspect
     * recent cache actions and perform eviction or bookkeeping.
     */
    interface Behaviour<K, V> {
        /** Processes one batch of [updates] collected since the last loop iteration. */
        fun process(cache: FastCache<K, V>, updates: ActionUpdates<K, V>)
    }

    // Behaviours /////////////////////////////////////////////////////////////////////////////////////

    /**
     * Behaviour that expires entries after a fixed [ttl] since their last access.
     *
     * Each read or put resets the TTL clock for the affected entry.
     * Expired entries are evicted during the next processing-loop iteration. The clock starts at the
     * pass that sees the access, so an entry can be served up to [ttl] plus two loop periods after it.
     * A zero or negative [ttl] expires entries at the next pass, one below 1 ms counts as 1 ms, and
     * [Duration.INFINITE] never expires.
     */
    @Suppress("DuplicatedCode")
    class ExpireAfterAccessBehaviour<K, V>(ttl: Duration) : Behaviour<K, V> {
        // Deliberately holds no value: the behaviour never reads it, and keeping it would give every
        // cached value a second strong reference that outlives the cache entry itself.
        private data class Entry(
            val accessed: Long,
        )

        private val ttlMs = ttl.ttlMillis()
        private val data = ValueSortedMap<K, Entry, Long> { it.accessed }

        override fun process(cache: FastCache<K, V>, updates: ActionUpdates<K, V>) {
            val now = cache.millisNow()

            updates.lastByKey.forEach { (key, action) ->
                handle(now, key, action)
            }

            evictAllNecessary(now, cache)
        }

        private fun handle(now: Long, key: K, action: Action<K, V>) {
            when (action) {
                is ReadAction, is PutAction -> {
                    data[action.key] = Entry(accessed = now)
                }

                // A miss as the key's last action means it is gone: a remove followed by a miss
                // in the same batch left a tracked key that no longer exists.
                is RemoveAction, is MissAction -> data.remove(key)
            }
        }

        private fun evict(cache: FastCache<K, V>, key: K) {
            // Ask FIRST. removeSilently declines when the key was touched after the batch was
            // taken, and dropping our tracking for a declined eviction left the entry in the map
            // with nothing watching it. A declined key stays tracked; the pending read that spared
            // it re-stamps its access time next iteration, which is exactly this behaviour's rule.
            if (cache.removeSilently(key, SparedBy.ANY_PENDING_ACTION) != Eviction.SPARED) {
                data.remove(key)
            }
        }

        private fun evictAllNecessary(now: Long, cache: FastCache<K, V>) {
            val toEvict = data.ascending()
                .takeWhile { (_, entry) -> now - entry.accessed >= ttlMs }

            toEvict.forEach { (key, _) -> evict(cache, key) }
        }
    }

    /**
     * Behaviour that expires entries after a fixed [ttl] since they were last written.
     *
     * Only put operations reset the TTL clock. Reads do NOT extend the entry's lifetime.
     * Expired entries are evicted during the next processing-loop iteration. The clock starts at the
     * pass that sees the write, so an entry can be served up to [ttl] plus two loop periods after it.
     * A zero or negative [ttl] expires entries at the next pass, one below 1 ms counts as 1 ms, and
     * [Duration.INFINITE] never expires.
     */
    @Suppress("DuplicatedCode")
    class ExpireAfterWriteBehaviour<K, V>(ttl: Duration) : Behaviour<K, V> {
        // Deliberately holds no value - see ExpireAfterAccessBehaviour.Entry.
        private data class Entry(
            val written: Long,
        )

        private val ttlMs = ttl.ttlMillis()
        private val data = ValueSortedMap<K, Entry, Long> { it.written }

        override fun process(cache: FastCache<K, V>, updates: ActionUpdates<K, V>) {
            val now = cache.millisNow()

            updates.lastWriteByKey.forEach { (key, action) ->
                handle(now, key, action)
            }

            evictAllNecessary(now, cache)
        }

        private fun handle(now: Long, key: K, action: Action<K, V>) {
            when (action) {
                is PutAction -> {
                    data[key] = Entry(written = now)
                }

                // A miss as the key's last action means it is gone - see ExpireAfterAccessBehaviour
                is RemoveAction, is MissAction -> data.remove(key)
                is ReadAction -> { /* reads do NOT reset write TTL, and lastWriteByKey holds none */
                }
            }
        }

        private fun evict(cache: FastCache<K, V>, key: K) {
            // Ask FIRST, and only a pending PUT spares the entry: a read does not reset a write TTL,
            // and sparing reads let a key read in every pass outlive its TTL for good. A spared key
            // stays tracked; its pending put re-stamps it next pass.
            if (cache.removeSilently(key, SparedBy.PENDING_PUT) != Eviction.SPARED) {
                data.remove(key)
            }
        }

        private fun evictAllNecessary(now: Long, cache: FastCache<K, V>) {
            val toEvict = data.ascending()
                .takeWhile { (_, entry) -> now - entry.written >= ttlMs }

            toEvict.forEach { (key, _) -> evict(cache, key) }
        }
    }

    /**
     * Behaviour that keeps a maximum number of entries in the cache.
     *
     * The least recently accessed entries are evicted first. A soft bound: an entry
     * read or written in every maintenance pass is not evicted by it, so under such load the cache
     * holds up to the number of distinct keys touched within one pass.
     */
    @Suppress("DuplicatedCode")
    class MaxEntriesBehaviour<K, V>(private val maxEntries: Int) : Behaviour<K, V> {
        // Holds the key (the eviction loop needs it) but never the value - see ExpireAfterAccess.
        private data class Entry<K>(
            val key: K,
            val accessed: Long,
        )

        private val data = ValueSortedMap<K, Entry<K>, Long> { it.accessed }

        init {
            // Silently coercing to 1 turned `maxEntries(0)`, meant as "disable", into a 1-entry cache
            require(maxEntries > 0) { "maxEntries must be > 0, but was $maxEntries" }
        }

        override fun process(cache: FastCache<K, V>, updates: ActionUpdates<K, V>) {
            val now = cache.millisNow()

            updates.lastByKey.forEach { (key, action) ->
                handle(now, key, action)
            }

            evictAllNecessary(cache)
        }

        // TODO(scan): every action in one batch is stamped with the same `now`, so LRU order is only
        //   resolved down to one loop iteration - within a batch it falls back to arrival order.
        private fun handle(now: Long, key: K, action: Action<K, V>) {
            when (action) {
                is ReadAction, is PutAction -> {
                    data[action.key] = Entry(key = action.key, accessed = now)
                }

                // A miss as the key's last action means it is gone - see ExpireAfterAccessBehaviour
                is RemoveAction, is MissAction -> data.remove(key)
            }
        }

        private fun evict(cache: FastCache<K, V>, key: K): Boolean {
            // Ask FIRST - see ExpireAfterAccessBehaviour.evict. Reports whether this evicted an entry:
            // a spared one is no progress, and neither is one already gone - the overshoot below is
            // taken from what the cache really holds, which no longer counts it. Its tracking goes.
            return when (cache.removeSilently(key, SparedBy.ANY_PENDING_ACTION)) {
                Eviction.REMOVED -> true.also { data.remove(key) }
                Eviction.ABSENT -> false.also { data.remove(key) }
                Eviction.SPARED -> false
            }
        }

        private fun evictAllNecessary(cache: FastCache<K, V>) {
            // From what the cache really holds: tracked entries already gone (removed earlier in this
            // pass, their removal still pending) made it evict live ones in their place.
            var overshoot = minOf(data.size, cache.size) - maxEntries

            if (overshoot <= 0) return

            // A snapshot, NOT `while (data.size > maxEntries)`: a declined eviction keeps the key
            // tracked, so a size-based loop would pick the same key again and spin the processing
            // coroutine forever. A skipped key is retried next pass. A key accessed in EVERY pass is
            // skipped in every pass, so the cache stays over its bound for as long as that lasts: a
            // soft bound by decision (FastCacheSoftBoundSpec).
            for ((key, _) in data.ascending().toList()) {
                if (overshoot <= 0) break

                if (evict(cache, key)) {
                    overshoot--
                }
            }
        }
    }

    /**
     * Behaviour that keeps a maximum memory size in the cache.
     *
     * The least recently accessed entries are evicted first. A soft bound, like
     * [MaxEntriesBehaviour]: an entry read or written in every maintenance pass is not evicted by it.
     */
    @Suppress("DuplicatedCode")
    class MaxMemoryUsageBehaviour<K, V>(
        /** The maximum allowed total estimated memory size in bytes. */
        val maxMemorySize: Long,
        /** The estimator used to calculate the in-memory size of keys and values. */
        val estimator: ObjectSizeEstimator = ObjectSizeEstimator(),
    ) : Behaviour<K, V> {

        // Holds the key (the eviction loop needs it) but never the value - see ExpireAfterAccess.
        private data class Entry<K>(
            val key: K,
            val accessed: Long,
            val size: Long,
        )

        // Volatile: written by the processing loop, read by any thread.
        /** The total estimated memory usage (in bytes) of all entries tracked by this behaviour. */
        @Volatile
        var totalSize = 0L
            private set

        private val data = ValueSortedMap<K, Entry<K>, Long> { it.accessed }

        init {
            require(maxMemorySize > 0) { "maxMemorySize must be > 0, but was $maxMemorySize" }
        }

        override fun process(cache: FastCache<K, V>, updates: ActionUpdates<K, V>) {
            val now = cache.millisNow()

            updates.lastByKey.forEach { (key, action) ->
                handle(now, key, action)
            }

            evictAllNecessary(cache)
        }

        private fun handle(now: Long, key: K, action: Action<K, V>) {
            val current = data[key]
            val currentSize = current?.size ?: 0

            when (action) {
                is ReadAction, is PutAction -> {
                    val value = when (action) {
                        is ReadAction -> action.value
                        is PutAction -> action.value
                    }

                    // TODO(scan): re-walks the whole object graph of key and value on every read, not
                    //   just on writes.
                    // A failing estimate must not end the batch: the entries after it went uncounted
                    // and their removals left ghost bytes. The entry keeps its previous size (0 when
                    // new), so it stays tracked and evictable, and its removal stays consistent.
                    val newSize = try {
                        estimator.estimate(action.key) + estimator.estimate(value)
                    } catch (e: Throwable) {
                        // The exception's class only: its message can carry cached data.
                        println("FastCache size estimate failed: ${e::class.simpleName}")
                        currentSize
                    }

                    totalSize += newSize - currentSize

                    data[action.key] = Entry(key = action.key, accessed = now, size = newSize)
                }

                // A miss as the key's last action means it is gone - see ExpireAfterAccessBehaviour
                is RemoveAction, is MissAction -> {
                    current?.let { remove(current) }
                }
            }
        }

        private fun remove(entry: Entry<K>) {
            totalSize -= entry.size
            data.remove(entry.key)
        }

        private fun evict(cache: FastCache<K, V>, entry: Entry<K>): Boolean {
            // Ask FIRST, and only release the bytes of an entry that is gone: decrementing totalSize
            // for a spared one under-counted a cache that still holds the entry.
            return (cache.removeSilently(entry.key, SparedBy.ANY_PENDING_ACTION) != Eviction.SPARED).also { gone ->
                if (gone) remove(entry)
            }
        }

        private fun evictAllNecessary(cache: FastCache<K, V>) {
            var overflow = totalSize - maxMemorySize

            if (overflow <= 0) return

            // Entries already gone (removed earlier in this pass, their removal still pending) hold
            // no bytes: release them first, so no live entry is evicted in place of one.
            data.ascending().toList().forEach { (_, entry) ->
                if (!cache.isCached(entry.key)) remove(entry)
            }

            overflow = totalSize - maxMemorySize

            if (overflow <= 0) return

            // Snapshot for the same reason as MaxEntriesBehaviour: a declined eviction stays
            // tracked, and a `while (overflow > 0)` over live state would retry it forever.
            for ((_, entry) in data.ascending().toList()) {
                if (overflow <= 0) break

                if (evict(cache, entry)) {
                    overflow -= entry.size
                }
            }
        }
    }

    /**
     * Behaviour that fires a [handler] callback when entries are evicted by other behaviours.
     *
     * Does NOT fire on explicit [FastCache.remove] calls — only on automatic eviction via the
     * processing loop.
     */
    class OnEvictionBehaviour<K, V>(
        /** The callback invoked with the key and value of each evicted entry. */
        internal val handler: (K, V) -> Unit,
    ) : Behaviour<K, V> {
        /** No-op — [handler] is wired up as an eviction listener when the [FastCache] is constructed. */
        override fun process(cache: FastCache<K, V>, updates: ActionUpdates<K, V>) {
            // No-op: eviction notifications are handled via the listener registered in FastCache.init
        }
    }

    /**
     * Behaviour that tracks cache statistics: hits, misses, puts, and evictions.
     *
     * Retrieve a snapshot via [snapshot].
     */
    class StatisticsBehaviour<K, V> : Behaviour<K, V> {

        /** Immutable snapshot of cache statistics. */
        data class CacheStats(
            /** Number of cache hits (successful lookups). */
            val hitCount: Long,
            /** Number of cache misses (lookups for absent keys). */
            val missCount: Long,
            /** Number of put operations (inserts and updates). */
            val putCount: Long,
            /** Number of entries evicted by behaviours. */
            val evictionCount: Long,
        ) {
            /** Total number of get/has requests (hits + misses). */
            val requestCount: Long get() = hitCount + missCount

            /** Hit rate as a ratio between 0.0 and 1.0 (NaN if no requests). */
            val hitRate: Double get() = if (requestCount == 0L) Double.NaN else hitCount.toDouble() / requestCount
        }

        // Written only by the processing loop, read by any thread through snapshot(). One volatile
        // reference to an immutable value gives readers visibility AND counters from the same moment.
        @Volatile
        private var stats = CacheStats(hitCount = 0, missCount = 0, putCount = 0, evictionCount = 0)

        /** Returns an immutable snapshot of the current statistics. */
        fun snapshot(): CacheStats = stats

        override fun process(cache: FastCache<K, V>, updates: ActionUpdates<K, V>) {
            var hits = 0L
            var misses = 0L
            var puts = 0L

            for (action in updates.actions) {
                when (action) {
                    is ReadAction -> hits++
                    is MissAction -> misses++
                    is PutAction -> puts++
                    is RemoveAction -> { /* explicit removes are not counted */
                    }
                }
            }

            if (hits + misses + puts > 0) {
                val current = stats

                stats = current.copy(
                    hitCount = current.hitCount + hits,
                    missCount = current.missCount + misses,
                    putCount = current.putCount + puts,
                )
            }
        }

        /** Called when an eviction occurs. */
        internal fun recordEviction() {
            val current = stats

            stats = current.copy(evictionCount = current.evictionCount + 1)
        }
    }

    /**
     * Behaviour that refreshes stale entries in the background instead of evicting them.
     *
     * When an entry's write age exceeds [refreshAfter], the [loader] is called asynchronously
     * to compute a new value. The stale value continues to be served during the refresh.
     * One refresh per key runs at a time, unless the key is written meanwhile: the write frees the
     * key, so a later refresh can run beside the older one.
     *
     * A refresh replaces only the value it started from. If the entry was written, removed or evicted
     * while the loader ran, the loaded value is dropped, without any notice. The comparison is by
     * identity: when the application writes back the very instance the refresh started from, the
     * refresh still replaces it, and readers get the loaded value until the next write or refresh -
     * what the refresh would have installed without that write. For booleans, enum constants, small
     * boxed numbers, interned strings and JS primitives "the very instance" is any equal value.
     *
     * The refresh's result is recorded like a put: it counts as an access for
     * [ExpireAfterAccessBehaviour] and for the least-recently-used order of [MaxEntriesBehaviour] and
     * [MaxMemoryUsageBehaviour], and as a put in [StatisticsBehaviour]. Whether a refresh should count
     * as an access is open in ULTRA-00321.
     *
     * A failed refresh keeps serving the stale value and retries the load in every pass, until it
     * succeeds or [hardTtl] evicts the entry.
     *
     * An optional [hardTtl] forces eviction when the write age exceeds it, even during refresh.
     *
     * Odd values fall back instead of throwing. Below 1 ms counts as 1 ms; [Duration.INFINITE] never
     * expires. A zero or negative [hardTtl] expires entries at the next pass. A zero or negative
     * [refreshAfter] means "do not keep" as well: entries expire at the next pass and are never
     * refreshed - not reloaded on every pass, which would hammer the [loader]'s source. A [hardTtl]
     * not above [refreshAfter] wins: entries expire at [hardTtl] and are never refreshed.
     */
    class RefreshAfterWriteBehaviour<K, V>(
        refreshAfter: Duration,
        hardTtl: Duration? = null,
        private val loader: suspend (K) -> V,
    ) : Behaviour<K, V> {

        private val refreshAfterMs = refreshAfter.ttlMillis()

        // The hard branch is checked first, so 0 here expires every entry before any refresh
        private val hardTtlMs = if (refreshAfter.isPositive()) hardTtl?.ttlMillis() else 0L

        private val writeTimestamps = mutableMapOf<K, Long>()

        /**
         * Keys currently being refreshed, each with the token of the refresh that claimed it. Guarded
         * by [refreshLock].
         *
         * A token, not just the key: a write releases a refresh's claim, so a newer refresh can claim
         * the key while the older one still runs - and the older one, failing, released the newer claim.
         */
        private val refreshClaims = mutableMapOf<K, Any>()

        /** The refresh coroutine releases a key on its own thread, while [process] claims keys on the loop. */
        private val refreshLock = Any()

        override fun process(cache: FastCache<K, V>, updates: ActionUpdates<K, V>) {
            val now = cache.millisNow()

            val keysToRefresh = mutableListOf<Pair<K, Any>>()
            val keysToEvict = mutableListOf<K>()

            RunSync(refreshLock) {
                // Track write timestamps. lastWriteByKey: a read right after a refresh's put used to
                // hide that put, so the key stayed claimed and was never refreshed again.
                updates.lastWriteByKey.forEach { (key, action) ->
                    when (action) {
                        is PutAction -> {
                            writeTimestamps[key] = now
                            refreshClaims.remove(key)
                        }

                        // A miss as the key's last action means it is gone - see ExpireAfterAccessBehaviour
                        is RemoveAction, is MissAction -> {
                            writeTimestamps.remove(key)
                            refreshClaims.remove(key)
                        }

                        is ReadAction -> { /* lastWriteByKey holds none */
                        }
                    }
                }

                // Check for stale entries and schedule refreshes
                for ((key, writtenAt) in writeTimestamps) {
                    val age = now - writtenAt

                    // Hard TTL: evict if exceeded
                    if (hardTtlMs != null && age >= hardTtlMs) {
                        keysToEvict.add(key)
                        continue
                    }

                    // Soft TTL: claim a stale key unless a refresh already holds it
                    if (age >= refreshAfterMs && key !in refreshClaims) {
                        val token = Any()
                        refreshClaims[key] = token
                        keysToRefresh.add(key to token)
                    }
                }
            }

            // Launch refresh coroutines - BEFORE the evictions below: those run user eviction
            // listeners, and one that throws ended process() with these keys claimed but never loaded.
            for ((key, token) in keysToRefresh) {
                // The value this refresh replaces. Gone already: nothing to refresh. Written after the
                // batch (a put pending): that value is not stale, and its put re-stamps the key.
                val startedFrom = cache.refreshableValue(key)

                if (startedFrom == null) {
                    release(key, token)
                    continue
                }

                cache.scope.launch {
                    // Set once the result reached the cache. Until then, every way out - a failure,
                    // cancellation, or an Error such as TODO() - must free the key for a retry.
                    var settled = false

                    try {
                        val newValue = loader(key)

                        // Only replace the value the refresh started from. Unconditionally, a loader
                        // slower than the hard TTL put an evicted entry back, and a value the
                        // application wrote while the loader ran was overwritten with an older one.
                        // A refused result needs no release: the write or removal that changed the
                        // entry recorded an action, and that releases the claim.
                        cache.replaceIfUnchanged(key, startedFrom.value, newValue)
                        settled = true
                    } catch (e: CancellationException) {
                        // The loader suspends, so this is cancellation: of the scope, or one the loader
                        // threw. Either way it ends this refresh quietly; finally frees the key.
                        throw e
                    } catch (e: Throwable) {
                        // Refresh failed: allow retry on next loop iteration. Every Throwable, not only
                        // Exceptions: one that is not (an Error, or e.g. VaultException) escaped to the
                        // scope's handler, which on native ends the process. There is no logger here,
                        // so a repeatedly failing loader would otherwise be entirely silent.
                        // The exception's class only: a key or a message can carry a session id or a token.
                        println("FastCache refresh failed: ${e::class.simpleName}")
                    } finally {
                        if (!settled) {
                            release(key, token)
                        }
                    }
                }
            }

            // Evict hard-expired entries outside refreshLock: removeSilently takes the cache's lock.
            // (An in-flight refresh cannot resurrect one: its result goes through replaceIfUnchanged,
            // which refuses once the entry is gone.)
            //
            // Only forget a key that is gone. removeSilently spares a key that was rewritten after the
            // batch was taken (a read spares nothing here, as in ExpireAfterWriteBehaviour) -
            // forgetting it anyway meant that entry was never refreshed and never hard-evicted again.
            // Kept, it is retried next loop.
            val evicted = keysToEvict.filter { key -> cache.removeSilently(key, SparedBy.PENDING_PUT) != Eviction.SPARED }

            if (evicted.isNotEmpty()) {
                RunSync(refreshLock) {
                    evicted.forEach { key ->
                        writeTimestamps.remove(key)
                        refreshClaims.remove(key)
                    }
                }
            }
        }

        /** Frees [key] for the next refresh, unless a newer refresh holds it. Runs on the refresh's thread. */
        private fun release(key: K, token: Any) {
            RunSync(refreshLock) {
                if (refreshClaims[key] === token) {
                    refreshClaims.remove(key)
                }
            }
        }
    }

    // Processing loop ////////////////////////////////////////////////////////////////////////////////

    private class ActionProcessingLoop<K, V>(
        private val ref: WeakReference<FastCache<K, V>>,
        private val loopDelay: Duration,
    ) {
        suspend fun run() {
            while (true) {
                // Check if the cache is still alive, otherwise break the loop and finish the coroutine
                val cache = ref.get() ?: break

                val actions = cache.sync {
                    cache.lastActions.also { currentActions ->
                        cache.lastActions = ArrayList(currentActions.size)
                        cache.pendingKeys = HashMap(cache.pendingKeys.size)
                    }
                }

                val updates = ActionUpdates(actions)

                cache.behaviours.forEach { behaviour ->
                    try {
                        behaviour.process(cache, updates)
                    } catch (e: Throwable) {
                        // process() does not suspend, so a CancellationException from it was thrown by
                        // user code, not by this loop being cancelled: rethrow only when it is.
                        if (e is CancellationException && !currentCoroutineContext().isActive) {
                            throw e
                        }

                        // Without this, one throw ends the coroutine for good: no expiry, no
                        // eviction, no statistics, no refresh - while get/put keep working, so the
                        // cache silently grows without bound. Every Throwable, Errors included: a
                        // StackOverflowError from estimating a deep value must not end the loop, and
                        // a dead loop turns any failure into unbounded growth.
                        // There is no logger here, so it goes to the platform's default channel.
                        // The exception's class only: its message can carry cached data.
                        println("FastCache behaviour [${behaviour::class.simpleName}] failed: ${e::class.simpleName}")
                    }
                }

                // Wait for the next cycle
                delay(loopDelay)
            }
        }
    }

    // Cache implementation ///////////////////////////////////////////////////////////////////////////

    /** Internal lock */
    private val lock = Any()

    /** Registered eviction listeners, invoked when removeSilently actually removes an entry. */
    private val evictionListeners = mutableListOf<(K, V) -> Unit>()

    /** List with last taken actions, to be processed by the behaviours */
    // TODO(scan): unbounded - if `scope` is cancelled, the loop stops and this grows forever, pinning
    //   every value that was ever read or written. (A throwing behaviour no longer stops the loop.)
    private var lastActions = ArrayList<Action<K, V>>(1_000)

    /**
     * The keys with an action in [lastActions], each mapped to whether a put is among them.
     *
     * One map, not two sets, so the two views are swapped together and cannot drift apart.
     */
    private var pendingKeys = HashMap<K, Boolean>()

    /** What [removeSilently] did with a key. */
    private enum class Eviction {
        /** Removed now. */
        REMOVED,

        /** Kept: touched after the caller's batch was taken. */
        SPARED,

        /** Not cached any more: removed earlier, for example in the same pass. */
        ABSENT,
    }

    /** What spares a key from [removeSilently]: any pending action, or only a pending put. */
    private enum class SparedBy {
        /** For the access-ordered behaviours: a pending read is an access, the entry is in use. */
        ANY_PENDING_ACTION,

        /**
         * For write-based expiry: a read does not reset a write TTL, so only a pending put spares the
         * entry. Sparing reads let a key read in every pass outlive its TTL for good.
         */
        PENDING_PUT,
    }

    /** Internal data map */
    private val map = mutableMapOf<K, V>()

    init {
        // Important:
        //   Capture NOTHING from the Cache object inside scope.launch
        //   Otherwise the GC will not be able to collect the FastCache instance.
        val ref = WeakReference(this)
        val delay = loopDelay.coerceAtLeast(1.milliseconds)

        // Run the processing loop
        scope.launch {
            ActionProcessingLoop(ref = ref, loopDelay = delay).run()
        }

        // Register eviction listeners from behaviours
        behaviours.filterIsInstance<OnEvictionBehaviour<K, V>>().forEach { behaviour ->
            evictionListeners.add(behaviour.handler)
        }
        behaviours.filterIsInstance<StatisticsBehaviour<K, V>>().forEach { behaviour ->
            evictionListeners.add { _, _ -> behaviour.recordEviction() }
        }
    }

    /** All current keys */
    override val keys: Set<K> get() = sync { map.keys.toSet() }

    /** All current values */
    override val values: List<V> get() = sync { map.values.toList() }

    /** All current entries */
    override val entries: Map<K, V> get() = sync { map.toMap() }

    /** Number of entries in the cache */
    override val size: Int get() = sync { map.size }

    /**
     * Clears the cache.
     *
     * Records a [RemoveAction] per entry, so every behaviour drops its tracking of the cleared entries
     * on its next iteration. Pending actions are kept, so the statistics still count them.
     */
    override fun clear() = sync {
        map.keys.forEach { key -> addAction(RemoveAction(key)) }
        map.clear()
    }

    /**
     * Whether [key] is cached — including when it is cached with a `null` value.
     *
     * A `null` value is a legitimate cache entry: `V` is unconstrained and `FastCache<Any?, Any?>`
     * is a real use (slumber caches serialized results, which can be null). Testing presence with
     * `map[key] != null` made such an entry occupy a slot while every accessor denied it existed.
     */
    override fun has(key: K): Boolean = sync { readAndRecord(key) != null }

    /**
     * Returns the value for [key], or `null` if absent. Records a [ReadAction] or [MissAction].
     *
     * A cached `null` and an absent key both return `null` — use [has] to tell them apart. The
     * recorded action distinguishes them correctly either way.
     */
    override fun get(key: K): V? = sync { readAndRecord(key)?.value }

    /** Carries a cached value so that "absent" stays distinguishable from "present and null". */
    private class Holder<V>(val value: V)

    /** [lookup], plus the matching [ReadAction] / [MissAction]. Caller must hold the lock. */
    private fun readAndRecord(key: K): Holder<V>? {
        val holder = lookup(key)

        when (holder) {
            null -> addAction(MissAction(key))
            else -> addAction(ReadAction(key, holder.value))
        }

        return holder
    }

    /**
     * Presence-tolerant read: `null` means absent, a [Holder] means present, possibly holding `null`.
     *
     * A non-null value settles presence without a second probe, so a cache that never stores null
     * pays nothing; only the null case costs the extra `containsKey`. Caller must hold the lock.
     */
    @Suppress("UNCHECKED_CAST")
    private fun lookup(key: K): Holder<V>? {
        val value = map[key]

        return when {
            value != null || map.containsKey(key) -> Holder(value as V)
            else -> null
        }
    }

    /** Inserts or updates the [value] for [key] and records a [PutAction]. */
    override fun put(key: K, value: V) = sync {
        map[key] = value

        addAction(PutAction(key, value))
    }

    /**
     * Returns the cached value for [key] if present.
     *
     * Otherwise invokes [producer] outside the lock, stores the result, and returns it.
     * Under contention the producer may be called more than once, but only the first
     * value that lands in the map is returned. A producer running across a [remove] or [clear]
     * still stores the value it computed before them.
     */
    override fun getOrPut(key: K, producer: () -> V): V {
        // Fast path. Tests presence rather than nullness, so a key cached as null is served from the
        // cache instead of re-running the producer on every call.
        sync { readAndRecord(key) }?.let { return it.value }

        // Slow path: compute the value outside the lock
        // Tradeoff: the producer might be called multiple times when a race-condition occurs
        val newValue = producer()

        // Insert if it hasn't been put by another thread in the meantime
        return sync {
            val existing = lookup(key)

            if (existing != null) {
                // Another thread won. Record the access: without it this hit is invisible to the
                // statistics AND to the access-ordered behaviours, so a key served only through
                // this branch ages as if it were never touched and gets evicted early.
                addAction(ReadAction(key, existing.value))

                return@sync existing.value
            }

            newValue.also {
                map[key] = it
                addAction(PutAction(key, it))
            }
        }
    }

    /** Removes and returns the value for [key], or `null` if absent. Records a [RemoveAction]. */
    override fun remove(key: K): V? = sync {
        map.remove(key).also {
            addAction(RemoveAction(key))
        }
    }

    /** Whether [key] is cached, its value null included. Records no action. */
    private fun isCached(key: K): Boolean = sync { map.containsKey(key) }

    /**
     * The value cached for [key], or `null` when absent or written after the current batch was taken
     * (a put pending) - see [lookup]. Records no action.
     */
    private fun refreshableValue(key: K): Holder<V>? = sync {
        if (pendingKeys[key] == true) null else lookup(key)
    }

    /**
     * Replaces the value of [key] with [value] only while it still holds [expected], and reports
     * whether it did. Compares by identity, so writing back the very same instance counts as unchanged.
     *
     * Used by the refresh path: a refresh must neither resurrect an entry removed while its loader
     * ran, nor overwrite a value written meanwhile.
     */
    private fun replaceIfUnchanged(key: K, expected: V, value: V): Boolean = sync {
        val current = lookup(key)

        if (current == null || current.value !== expected) {
            return@sync false
        }

        map[key] = value
        addAction(PutAction(key, value))

        true
    }

    /**
     * Evicts [key] on behalf of a behaviour, unless it was touched since the current batch was taken
     * in a way that [sparedBy] counts: any action for the access-ordered behaviours, only a put for
     * write-based expiry.
     *
     * Reports what happened: [Eviction.REMOVED], [Eviction.SPARED], or [Eviction.ABSENT] when the key
     * is not cached any more. Callers must not drop their own tracking for a SPARED key, or the entry
     * stays in the map with no behaviour watching it — never expired and never evicted again.
     *
     * Records a [RemoveAction] so that *every* behaviour learns about the eviction, not just the one
     * that asked for it. Without it, `refreshAfterWrite` kept reloading keys `maxEntries` had
     * evicted, and `MaxMemoryUsageBehaviour.totalSize` kept counting bytes that were already freed.
     */
    private fun removeSilently(key: K, sparedBy: SparedBy): Eviction = sync {
        // Presence first: a key removed earlier in the same pass has a pending RemoveAction, and
        // reporting it as spared made a size bound evict a live entry in its place. containsKey, not
        // `removed != null`: a null value is a real entry.
        if (!map.containsKey(key)) {
            return@sync Eviction.ABSENT
        }

        // A pending action means the entry was used or rewritten after the batch the caller decided
        // on was taken - unless the caller expires by write time, where only a rewrite counts.
        val spared = when (sparedBy) {
            SparedBy.ANY_PENDING_ACTION -> pendingKeys.containsKey(key)
            SparedBy.PENDING_PUT -> pendingKeys[key] == true
        }

        if (spared) {
            return@sync Eviction.SPARED
        }

        val removed = map.remove(key)

        addAction(RemoveAction(key))

        // TODO(scan): user callbacks run while the lock is held - on native that is the single
        //   process-wide spin lock.
        evictionListeners.forEach { listener ->
            // One at a time: a throwing listener skipped the ones behind it, and ended the
            // calling behaviour's process() - one eviction per behaviour and pass.
            try {
                @Suppress("UNCHECKED_CAST")
                listener(key, removed as V)
            } catch (e: Throwable) {
                // The exception's class only: its message can carry cached data.
                println("FastCache eviction listener failed: ${e::class.simpleName}")
            }
        }

        Eviction.REMOVED
    }

    private fun addAction(action: Action<K, V>) {
        lastActions.add(action)
        // One lookup for a key already pending: a put marks it, anything else only adds it unmarked
        if (action is PutAction) {
            pendingKeys[action.key] = true
        } else {
            pendingKeys.getOrPut(action.key) { false }
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun <T> sync(noinline block: () -> T): T {
        return RunSync(lock, block)
    }
}
