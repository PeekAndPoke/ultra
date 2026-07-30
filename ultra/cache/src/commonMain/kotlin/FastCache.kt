package io.peekandpoke.ultra.cache

import io.peekandpoke.ultra.common.RunSync
import io.peekandpoke.ultra.common.WeakReference
import io.peekandpoke.ultra.datetime.Kronos
import kotlinx.coroutines.CancellationException
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
    /** The coroutine scope used for the internal processing loop. */
    val scope: CoroutineScope = Cache.defaultCoroutineScope,
    /** The list of behaviours applied during each processing-loop iteration. */
    val behaviours: List<Behaviour<K, V>>,
    /** The delay between processing-loop iterations. */
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

        /**
         * Adds the [MaxMemoryUsageBehaviour] to the cache.
         *
         * [maxMemorySize] is a **budget in estimated bytes, not a real memory bound** — see
         * [ObjectSizeEstimator] for how far the estimate can be off, which differs per platform and
         * is worst on native, where every custom object counts as 32 bytes whatever it holds. Pass a
         * tuned [estimator], or a purpose-built one, where the number has to mean something.
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

        /** Adds the [RefreshAfterWriteBehaviour] to the cache. */
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
        // Deliberately holds no value: the behaviour never reads it, and keeping it would give every
        // cached value a second strong reference that outlives the cache entry itself.
        private data class Entry(
            val accessed: Long,
        )

        private val ttlMs = ttl.inWholeMilliseconds
        private val clock = Kronos.systemUtc
        private val data = ValueSortedMap<K, Entry, Long> { it.accessed }

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
                    data[action.key] = Entry(accessed = now)
                }

                is RemoveAction -> data.remove(key)
                is MissAction -> { /* no-op */
                }
            }
        }

        private fun evict(cache: FastCache<K, V>, key: K) {
            // Ask FIRST. removeSilently declines when the key was touched after the batch was
            // taken, and dropping our tracking for a declined eviction left the entry in the map
            // with nothing watching it. A declined key stays tracked; the pending read that spared
            // it re-stamps its access time next iteration, which is exactly this behaviour's rule.
            if (cache.removeSilently(key)) {
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
     * Expired entries are evicted during the next processing-loop iteration.
     */
    @Suppress("DuplicatedCode")
    class ExpireAfterWriteBehaviour<K, V>(ttl: Duration) : Behaviour<K, V> {
        // Deliberately holds no value - see ExpireAfterAccessBehaviour.Entry.
        private data class Entry(
            val written: Long,
        )

        private val ttlMs = ttl.inWholeMilliseconds
        private val clock = Kronos.systemUtc
        private val data = ValueSortedMap<K, Entry, Long> { it.written }

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
                    data[key] = Entry(written = now)
                }

                is RemoveAction -> data.remove(key)
                is ReadAction -> { /* reads do NOT reset write TTL */
                }

                is MissAction -> { /* no-op */
                }
            }
        }

        private fun evict(cache: FastCache<K, V>, key: K) {
            // Ask FIRST - the one behaviour where remove-before-ask was unrecoverable. Reads are
            // deliberately ignored here, so only a PutAction could re-track a dropped key; an entry
            // whose eviction was declined because of a pending READ therefore stayed in the map
            // forever. Kept tracked, it is simply retried once the pending action has drained.
            if (cache.removeSilently(key)) {
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
     * The least recently accessed entries are evicted first.
     */
    @Suppress("DuplicatedCode")
    class MaxEntriesBehaviour<K, V>(private val maxEntries: Int) : Behaviour<K, V> {
        // Holds the key (the eviction loop needs it) but never the value - see ExpireAfterAccess.
        private data class Entry<K>(
            val key: K,
            val accessed: Long,
        )

        private val clock = Kronos.systemUtc
        private val data = ValueSortedMap<K, Entry<K>, Long> { it.accessed }

        init {
            // Silently coercing to 1 turned `maxEntries(0)`, meant as "disable", into a 1-entry cache
            require(maxEntries > 0) { "maxEntries must be > 0, but was $maxEntries" }
        }

        override fun process(cache: FastCache<K, V>, updates: ActionUpdates<K, V>) {
            val now = clock.millisNow()

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

                is RemoveAction -> data.remove(key)
                is MissAction -> { /* no-op */
                }
            }
        }

        private fun evict(cache: FastCache<K, V>, key: K): Boolean {
            // Ask FIRST - see ExpireAfterAccessBehaviour.evict. Reports whether it really evicted,
            // because the loop below must not count a declined attempt as progress.
            return cache.removeSilently(key).also { removed ->
                if (removed) data.remove(key)
            }
        }

        private fun evictAllNecessary(cache: FastCache<K, V>) {
            var overshoot = data.size - maxEntries

            if (overshoot <= 0) return

            // A snapshot, NOT `while (data.size > maxEntries)`: a declined eviction keeps the key
            // tracked, so a size-based loop would pick the same key again and spin the processing
            // coroutine forever. Skipped keys stay over-capacity for one round and are retried
            // next iteration, when their pending action has drained.
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
     * The least recently accessed entries are evicted first.
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

        // TODO(scan): mutated on the loop thread and read by callers without any memory barrier, and
        //   it keeps counting entries that another behaviour evicted, because eviction records no action.
        /** The total estimated memory usage (in bytes) of all entries tracked by this behaviour. */
        var totalSize = 0L
            private set

        private val clock = Kronos.systemUtc
        private val data = ValueSortedMap<K, Entry<K>, Long> { it.accessed }

        init {
            require(maxMemorySize > 0) { "maxMemorySize must be > 0, but was $maxMemorySize" }
        }

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

                    // TODO(scan): re-walks the whole object graph of key and value on every read, not
                    //   just on writes.
                    val newKeySize = estimator.estimate(action.key)
                    val newValueSize = estimator.estimate(value)
                    val newSize = newKeySize + newValueSize

                    totalSize += newSize - currentSize

                    data[action.key] = Entry(key = action.key, accessed = now, size = newSize)
                }

                is RemoveAction -> {
                    current?.let { remove(current) }
                }

                is MissAction -> { /* no-op */
                }
            }
        }

        private fun remove(entry: Entry<K>) {
            totalSize -= entry.size
            data.remove(entry.key)
        }

        private fun evict(cache: FastCache<K, V>, entry: Entry<K>): Boolean {
            // Ask FIRST, and only release the bytes for an entry that was really removed:
            // decrementing totalSize for a declined eviction under-counted a cache that still
            // holds the entry.
            return cache.removeSilently(entry.key).also { removed ->
                if (removed) remove(entry)
            }
        }

        private fun evictAllNecessary(cache: FastCache<K, V>) {
            var overflow = totalSize - maxMemorySize

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
        // TODO(scan): the handler is skipped for entries whose value is null - see removeSilently.
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

        // TODO(scan): plain fields - written on the processing-loop thread, read by callers of snapshot()
        //   with no memory barrier, and any action dropped by clear() is never counted.
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

        // TODO(scan): only a RemoveAction or the hard TTL drops a key here - an entry evicted by any
        //   other behaviour stays and keeps being reloaded, putting the evicted key back into the cache.
        private val writeTimestamps = mutableMapOf<K, Long>()

        // TODO(scan): also mutated from the launched refresh coroutine, so it races with process().
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

            // Evict hard-expired entries. (An in-flight refresh cannot resurrect one: its result
            // goes through putIfPresent, which refuses once the entry is gone.)
            for (key in keysToEvict) {
                // Only forget a key that was really removed. removeSilently declines when the key
                // was touched after the batch was taken - forgetting it anyway meant that entry
                // was never refreshed and never hard-evicted again. Kept, it is retried next loop.
                if (cache.removeSilently(key)) {
                    writeTimestamps.remove(key)
                    refreshingKeys.remove(key)
                }
            }

            // Launch refresh coroutines
            for (key in keysToRefresh) {
                refreshingKeys.add(key)
                cache.scope.launch {
                    try {
                        val newValue = loader(key)

                        // Only refresh an entry that is still cached. A loader slower than the hard
                        // TTL used to have its result put back unconditionally, so the entry
                        // reappeared after being hard-evicted - i.e. the hard TTL was not hard. The
                        // same applies to an explicit remove() racing an in-flight refresh.
                        cache.putIfPresent(key, newValue)
                    } catch (e: CancellationException) {
                        // never swallow cancellation - it is how the scope is torn down
                        refreshingKeys.remove(key)
                        throw e
                    } catch (e: Exception) {
                        // Refresh failed: allow retry on next loop iteration. There is no logger
                        // here, so a repeatedly failing loader would otherwise be entirely silent.
                        refreshingKeys.remove(key)
                        println("FastCache refresh for key [$key] failed: $e")
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
                    try {
                        behaviour.process(cache, updates)
                    } catch (e: CancellationException) {
                        // never swallow cancellation - it is how the scope is torn down
                        throw e
                    } catch (e: Exception) {
                        // Without this, one throw ends the coroutine for good: no expiry, no
                        // eviction, no statistics, no refresh - while get/put keep working, so the
                        // cache silently grows without bound. A user onEviction handler throwing is
                        // enough, since removeSilently invokes those from inside process().
                        // There is no logger here, so it goes to the platform's default channel.
                        println("FastCache behaviour [${behaviour::class.simpleName}] failed: $e")
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
    // TODO(scan): unbounded - if `scope` is cancelled or the loop dies, this grows forever and pins
    //   every value that was ever read or written.
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
    // TODO(scan): behaviours are never told - their tracking maps, totalSize and refresh timestamps
    //   survive the clear, and the pending actions dropped here are lost to the statistics.
    override fun clear() = sync {
        map.clear()
        lastActions.clear()
        lastActionsKeys.clear()
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
     * value that lands in the map is returned.
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

    /**
     * Updates [key] to [value] only if it is still cached, and reports whether it did.
     *
     * Used by the refresh path: a value loaded for an entry that has since been evicted must not
     * resurrect it.
     */
    private fun putIfPresent(key: K, value: V): Boolean = sync {
        if (!map.containsKey(key)) {
            return@sync false
        }

        map[key] = value
        addAction(PutAction(key, value))

        true
    }

    /**
     * Evicts [key] on behalf of a behaviour, unless it was touched since the current batch was taken.
     *
     * Returns true when the entry was actually removed. Callers must not drop their own tracking for
     * a key this declined, or the entry stays in the map with no behaviour watching it — never
     * expired and never evicted again.
     *
     * Records a [RemoveAction] so that *every* behaviour learns about the eviction, not just the one
     * that asked for it. Without it, `refreshAfterWrite` kept reloading keys `maxEntries` had
     * evicted, and `MaxMemoryUsageBehaviour.totalSize` kept counting bytes that were already freed.
     */
    private fun removeSilently(key: K): Boolean = sync {
        // If there is a pending action for this key (e.g. it was just Put or Read),
        // it means the entry was recently updated or accessed, so we should NOT evict it.
        val hasPendingUpdates = lastActionsKeys.contains(key)

        if (hasPendingUpdates) {
            return@sync false
        }

        // containsKey, not `removed != null`: a null value is a real entry, and testing for null
        // meant evicting one fired no listener and bumped no eviction count.
        val wasPresent = map.containsKey(key)
        val removed = map.remove(key)

        if (wasPresent) {
            addAction(RemoveAction(key))

            // TODO(scan): user callbacks run while the lock is held - on native that is the single
            //   process-wide spin lock, and a throwing callback starves the listeners behind it.
            @Suppress("UNCHECKED_CAST")
            evictionListeners.forEach { listener -> listener(key, removed as V) }
        }

        wasPresent
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
