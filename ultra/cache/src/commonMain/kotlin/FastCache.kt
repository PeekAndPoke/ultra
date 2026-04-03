package io.peekandpoke.ultra.cache

import io.peekandpoke.ultra.common.RunSync
import io.peekandpoke.ultra.common.WeakReference
import io.peekandpoke.ultra.datetime.Kronos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Creates a [FastCache] using the DSL-style [configure] block.
 *
 * @param scope       the [CoroutineScope] for the internal processing loop
 * @param loopDelay   the delay between processing-loop iterations
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
 * Cache implementation that focuses on performance.
 *
 * The cache can use a number of [behaviours].
 * There is an internal processing loop that periodically invokes the behaviours.
 *
 * This means f.e. that the [Builder.maxMemoryUsage] behaviour will not check after every insert whether the cache is
 * over the limit and something needs to be evicted. Instead, the loop will check the memory usage periodically and
 * evict entries until the memory usage is below the limit.
 */
class FastCache<K, V>(
    val scope: CoroutineScope = Cache.defaultCoroutineScope,
    val behaviours: List<Behaviour<K, V>>,
    val loopDelay: Duration = defaultLoopDelay,
) : Cache<K, V> {

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

        /** Sets the loop delay. */
        fun loopDelay(delay: Duration) = apply { loopDelay = delay }

        /** Adds a behaviour to the cache. */
        fun addBehaviour(behaviour: Behaviour<K, V>) = apply { behaviours.add(behaviour) }

        /** Adds the [ExpireAfterAccessBehaviour] to the cache. */
        fun expireAfterAccess(ttl: Duration) = addBehaviour(ExpireAfterAccessBehaviour(ttl))

        /** Adds the [ExpireAfterWriteBehaviour] to the cache. */
        fun expireAfterWrite(ttl: Duration) = addBehaviour(ExpireAfterWriteBehaviour(ttl))

        /** Adds the [MaxEntriesBehaviour] to the cache. */
        fun maxEntries(maxEntries: Int) = addBehaviour(MaxEntriesBehaviour(maxEntries))

        /** Adds the [MaxMemoryUsageBehaviour] to the cache. */
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

        /** Adds the [RefreshAfterWriteBehaviour] to the cache. */
        fun refreshAfterWrite(
            ttl: Duration,
            hardTtl: Duration? = null,
            loader: suspend (K) -> V,
        ) = addBehaviour(RefreshAfterWriteBehaviour(refreshAfter = ttl, hardTtl = hardTtl, loader = loader))

        /** Builds the [FastCache] instance. */
        fun build() = FastCache(
            scope = scope,
            behaviours = behaviours,
            loopDelay = loopDelay,
        )
    }

    // Actions ////////////////////////////////////////////////////////////////////////////////////////

    /** Represents a cache action recorded for deferred processing by behaviours. */
    sealed interface Action<K, V> {
        /** The key involved in this action. */
        val key: K
    }

    /** Records that a value was read from the cache. */
    class ReadAction<K, V>(override val key: K, val value: V) : Action<K, V>

    /** Records that a value was inserted or updated in the cache. */
    class PutAction<K, V>(override val key: K, val value: V) : Action<K, V>

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
     * Expired entries are evicted during the next processing-loop iteration.
     */
    @Suppress("DuplicatedCode")
    class ExpireAfterAccessBehaviour<K, V>(ttl: Duration) : Behaviour<K, V> {
        private data class Entry<K, V>(
            val key: K,
            val value: V,
            val accessed: Long,
        )

        private val ttlMs = ttl.inWholeMilliseconds
        private val clock = Kronos.systemUtc
        private val data = ValueSortedMap<K, Entry<K, V>, Long> { it.accessed }

        override fun process(cache: FastCache<K, V>, updates: ActionUpdates<K, V>) {
            val now = clock.millisNow()

            updates.lastByKey.forEach { (key, action) ->
                handle(now, key, action)
            }

            evictAllNecessary(now, cache)
        }

        private fun handle(now: Long, key: K, action: Action<K, V>) {
            when (action) {
                is ReadAction, is PutAction -> {
                    val value = when (action) {
                        is ReadAction -> action.value
                        is PutAction -> action.value
                    }

                    data[action.key] = Entry(key = action.key, value = value, accessed = now)
                }

                is RemoveAction -> data.remove(key)
                is MissAction -> { /* no-op */
                }
            }
        }

        private fun evict(cache: FastCache<K, V>, key: K) {
            data.remove(key)
            cache.removeSilently(key)
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
     * Expired entries are evicted during the next processing-loop iteration.
     */
    @Suppress("DuplicatedCode")
    class ExpireAfterWriteBehaviour<K, V>(ttl: Duration) : Behaviour<K, V> {
        private data class Entry<K, V>(
            val key: K,
            val value: V,
            val written: Long,
        )

        private val ttlMs = ttl.inWholeMilliseconds
        private val clock = Kronos.systemUtc
        private val data = ValueSortedMap<K, Entry<K, V>, Long> { it.written }

        override fun process(cache: FastCache<K, V>, updates: ActionUpdates<K, V>) {
            val now = clock.millisNow()

            updates.lastByKey.forEach { (key, action) ->
                handle(now, key, action)
            }

            evictAllNecessary(now, cache)
        }

        private fun handle(now: Long, key: K, action: Action<K, V>) {
            when (action) {
                is PutAction -> {
                    data[key] = Entry(key = key, value = action.value, written = now)
                }

                is RemoveAction -> data.remove(key)
                is ReadAction -> { /* reads do NOT reset write TTL */
                }

                is MissAction -> { /* no-op */
                }
            }
        }

        private fun evict(cache: FastCache<K, V>, key: K) {
            data.remove(key)
            cache.removeSilently(key)
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
     * The least recently accessed entries are evicted first.
     */
    @Suppress("DuplicatedCode")
    class MaxEntriesBehaviour<K, V>(maxEntries: Int) : Behaviour<K, V> {
        private data class Entry<K, V>(
            val key: K,
            val value: V,
            val accessed: Long,
        )

        private val clock = Kronos.systemUtc
        private val data = ValueSortedMap<K, Entry<K, V>, Long> { it.accessed }
        private val maxEntries = maxEntries.coerceAtLeast(1)

        override fun process(cache: FastCache<K, V>, updates: ActionUpdates<K, V>) {
            val now = clock.millisNow()

            updates.lastByKey.forEach { (key, action) ->
                handle(now, key, action)
            }

            evictAllNecessary(cache)
        }

        private fun handle(now: Long, key: K, action: Action<K, V>) {
            when (action) {
                is ReadAction, is PutAction -> {
                    val value = when (action) {
                        is ReadAction -> action.value
                        is PutAction -> action.value
                    }

                    data[action.key] = Entry(key = action.key, value = value, accessed = now)
                }

                is RemoveAction -> data.remove(key)
                is MissAction -> { /* no-op */
                }
            }
        }

        private fun evict(cache: FastCache<K, V>, key: K) {
            data.remove(key)
            cache.removeSilently(key)
        }

        private fun evictAllNecessary(cache: FastCache<K, V>) {
            while (data.size > maxEntries) {
                val next = data.ascending().first().second

                evict(cache, next.key)
            }
        }
    }

    /**
     * Behaviour that keeps a maximum memory size in the cache.
     *
     * The least recently accessed entries are evicted first.
     */
    @Suppress("DuplicatedCode")
    class MaxMemoryUsageBehaviour<K, V>(
        val maxMemorySize: Long,
        val estimator: ObjectSizeEstimator = ObjectSizeEstimator(),
    ) : Behaviour<K, V> {

        private data class Entry<K, V>(
            val key: K,
            val value: V,
            val accessed: Long,
            val size: Long,
        )

        /** The total estimated memory usage (in bytes) of all entries tracked by this behaviour. */
        var totalSize = 0L
            private set

        private val clock = Kronos.systemUtc
        private val data = ValueSortedMap<K, Entry<K, V>, Long> { it.accessed }

        override fun process(cache: FastCache<K, V>, updates: ActionUpdates<K, V>) {
            val now = clock.millisNow()

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

                    val newKeySize = estimator.estimate(action.key)
                    val newValueSize = estimator.estimate(value)
                    val newSize = newKeySize + newValueSize

                    totalSize += newSize - currentSize

                    data[action.key] = Entry(key = action.key, value = value, accessed = now, size = newSize)
                }

                is RemoveAction -> {
                    current?.let { remove(current) }
                }

                is MissAction -> { /* no-op */
                }
            }
        }

        private fun remove(entry: Entry<K, V>) {
            totalSize -= entry.size
            data.remove(entry.key)
        }

        private fun evict(cache: FastCache<K, V>, entry: Entry<K, V>) {
            remove(entry)
            cache.removeSilently(entry.key)
        }

        private fun evictAllNecessary(cache: FastCache<K, V>) {
            var overflow = totalSize - maxMemorySize

            if (overflow <= 0) return

            while (overflow > 0 && data.isNotEmpty()) {
                val next = data.ascending().first().second

                overflow -= next.size

                evict(cache, next)
            }
        }
    }

    /**
     * Behaviour that fires a [handler] callback when entries are evicted by other behaviours.
     *
     * Does NOT fire on explicit [remove] calls — only on automatic eviction via the processing loop.
     */
    class OnEvictionBehaviour<K, V>(
        internal val handler: (K, V) -> Unit,
    ) : Behaviour<K, V> {
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
            val hitCount: Long,
            val missCount: Long,
            val putCount: Long,
            val evictionCount: Long,
        ) {
            /** Total number of get/has requests (hits + misses). */
            val requestCount: Long get() = hitCount + missCount

            /** Hit rate as a ratio between 0.0 and 1.0 (NaN if no requests). */
            val hitRate: Double get() = if (requestCount == 0L) Double.NaN else hitCount.toDouble() / requestCount
        }

        private var _hitCount = 0L
        private var _missCount = 0L
        private var _putCount = 0L
        private var _evictionCount = 0L

        /** Returns an immutable snapshot of the current statistics. */
        fun snapshot(): CacheStats = CacheStats(
            hitCount = _hitCount,
            missCount = _missCount,
            putCount = _putCount,
            evictionCount = _evictionCount,
        )

        override fun process(cache: FastCache<K, V>, updates: ActionUpdates<K, V>) {
            for (action in updates.actions) {
                when (action) {
                    is ReadAction -> _hitCount++
                    is MissAction -> _missCount++
                    is PutAction -> _putCount++
                    is RemoveAction -> { /* explicit removes are not counted */
                    }
                }
            }
        }

        /** Called when an eviction occurs. */
        internal fun recordEviction() {
            _evictionCount++
        }
    }

    /**
     * Behaviour that refreshes stale entries in the background instead of evicting them.
     *
     * When an entry's write age exceeds [refreshAfter], the [loader] is called asynchronously
     * to compute a new value. The stale value continues to be served during the refresh.
     * Only one refresh per key runs at a time.
     *
     * An optional [hardTtl] forces eviction when the write age exceeds it, even during refresh.
     */
    class RefreshAfterWriteBehaviour<K, V>(
        refreshAfter: Duration,
        private val hardTtl: Duration? = null,
        private val loader: suspend (K) -> V,
    ) : Behaviour<K, V> {

        private val refreshAfterMs = refreshAfter.inWholeMilliseconds
        private val hardTtlMs = hardTtl?.inWholeMilliseconds
        private val clock = Kronos.systemUtc
        private val writeTimestamps = mutableMapOf<K, Long>()

        /** Keys currently being refreshed — used for deduplication. */
        private val refreshingKeys = mutableSetOf<K>()

        override fun process(cache: FastCache<K, V>, updates: ActionUpdates<K, V>) {
            val now = clock.millisNow()

            // Track write timestamps
            updates.lastByKey.forEach { (key, action) ->
                when (action) {
                    is PutAction -> {
                        writeTimestamps[key] = now
                        refreshingKeys.remove(key)
                    }

                    is RemoveAction -> {
                        writeTimestamps.remove(key)
                        refreshingKeys.remove(key)
                    }

                    is ReadAction, is MissAction -> { /* no-op */
                    }
                }
            }

            // Check for stale entries and schedule refreshes
            val keysToRefresh = mutableListOf<K>()
            val keysToEvict = mutableListOf<K>()

            for ((key, writtenAt) in writeTimestamps) {
                val age = now - writtenAt

                // Hard TTL: evict if exceeded
                if (hardTtlMs != null && age >= hardTtlMs) {
                    keysToEvict.add(key)
                    continue
                }

                // Soft TTL: schedule refresh if stale and not already refreshing
                if (age >= refreshAfterMs && key !in refreshingKeys) {
                    keysToRefresh.add(key)
                }
            }

            // Evict hard-expired entries
            for (key in keysToEvict) {
                writeTimestamps.remove(key)
                refreshingKeys.remove(key)
                cache.removeSilently(key)
            }

            // Launch refresh coroutines
            for (key in keysToRefresh) {
                refreshingKeys.add(key)
                cache.scope.launch {
                    try {
                        val newValue = loader(key)
                        cache.put(key, newValue)
                    } catch (_: Exception) {
                        // Refresh failed: allow retry on next loop iteration
                        refreshingKeys.remove(key)
                    }
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
                        cache.lastActionsKeys = HashSet(cache.lastActionsKeys.size)
                    }
                }

                val updates = ActionUpdates(actions)

                cache.behaviours.forEach { behaviour ->
                    behaviour.process(cache, updates)
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
    private var lastActions = ArrayList<Action<K, V>>(1_000)

    /** Set of keys that are included in [lastActions] */
    private var lastActionsKeys = HashSet<K>()

    /** Internal data map */
    private val map = mutableMapOf<K, V>()

    init {
        // Important:
        //   Capture NOTHING from the Cache object inside scope.launch
        //   Otherwise the GC will not be able to collect the FastCache instance.
        val ref = WeakReference(this)
        val delay = loopDelay

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

    /** Clears the cache */
    override fun clear() = sync {
        map.clear()
        lastActions.clear()
        lastActionsKeys.clear()
    }

    /** Check if the cache contains the given key */
    override fun has(key: K): Boolean = sync {
        val value = map[key]
        if (value != null) {
            addAction(ReadAction(key, value))
            true
        } else {
            addAction(MissAction(key))
            false
        }
    }

    /** Returns the value for [key], or `null` if absent. Records a [ReadAction] or [MissAction]. */
    override fun get(key: K): V? = sync {
        val value = map[key]
        if (value != null) {
            addAction(ReadAction(key, value))
        } else {
            addAction(MissAction(key))
        }
        value
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
     * value that lands in the map is returned.
     */
    override fun getOrPut(key: K, producer: () -> V): V {
        // Fast path: try to get the existing value
        get(key)?.let { return it }

        // Slow path: compute the value outside the lock
        // Tradeoff: the producer might be called multiple times when a race-condition occurs
        val newValue = producer()

        // Insert if it hasn't been put by another thread in the meantime
        return sync {
            map[key] ?: newValue.also {
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

    private fun removeSilently(key: K) = sync {
        // If there is a pending action for this key (e.g. it was just Put or Read),
        // it means the entry was recently updated or accessed, so we should NOT evict it.
        val hasPendingUpdates = lastActionsKeys.contains(key)

        if (!hasPendingUpdates) {
            val removed = map.remove(key)
            if (removed != null) {
                evictionListeners.forEach { listener -> listener(key, removed) }
            }
        }
    }

    private fun addAction(action: Action<K, V>) {
        lastActions.add(action)
        lastActionsKeys.add(action.key)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun <T> sync(noinline block: () -> T): T {
        return RunSync(lock, block)
    }
}
