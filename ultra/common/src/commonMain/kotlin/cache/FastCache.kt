package de.peekandpoke.ultra.common.cache

import de.peekandpoke.ultra.common.RunSync
import de.peekandpoke.ultra.common.WeakReference
import de.peekandpoke.ultra.common.datetime.Kronos
import korlibs.time.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

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
    scope: CoroutineScope = Cache.defaultCoroutineScope,
    private val behaviours: List<Behaviour<K, V>>,
    private val loopDelay: Duration = defaultLoopDelay,
) : Cache<K, V> {

    companion object {
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

        /** Adds the [MaxEntriesBehaviour] to the cache. */
        fun maxEntries(maxEntries: Int) = addBehaviour(MaxEntriesBehaviour(maxEntries))

        /** Adds the [MaxMemoryUsageBehaviour] to the cache. */
        fun maxMemoryUsage(maxMemorySize: Long, estimator: ObjectSizeEstimator = ObjectSizeEstimator()) =
            addBehaviour(MaxMemoryUsageBehaviour(maxMemorySize, estimator))

        /** Builds the [FastCache] instance. */
        fun build() = FastCache(
            scope = scope,
            behaviours = behaviours,
            loopDelay = loopDelay,
        )
    }

    sealed interface Action<K, V> {
        val key: K
    }

    class ReadAction<K, V>(override val key: K, val value: V) : Action<K, V>
    class PutAction<K, V>(override val key: K, val value: V) : Action<K, V>
    class RemoveAction<K, V>(override val key: K) : Action<K, V>

    data class ActionUpdates<K, V>(
        val actions: List<Action<K, V>>,
    ) {
        val byKey: Map<K, List<Action<K, V>>> by lazy {
            actions.groupBy { it.key }
        }

        val lastByKey: Map<K, Action<K, V>> by lazy {
            byKey.mapValues { (_, actions) -> actions.last() }
        }
    }

    interface Behaviour<K, V> {
        fun process(cache: FastCache<K, V>, updates: ActionUpdates<K, V>)
    }

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

        /**
         * Processes the cache actions
         *
         * The following steps are taken:
         * - Group actions by key
         * - take the last action per key, as it is the current state
         * - update the total tracked size
         * - evict oldest as long as the capacity limit is violated
         */
        override fun process(cache: FastCache<K, V>, updates: ActionUpdates<K, V>) {
            val now = clock.millisNow()

            updates.lastByKey.forEach { (key, action) ->
                handle(now, key, action)
            }

            // Evict entries until we are below the max memory size
            evictAllNecessary(now, cache)
        }

        private fun handle(now: Long, key: K, action: Action<K, V>) {
            when (action) {
                is ReadAction, is PutAction -> {
                    val value = when (action) {
                        is ReadAction -> action.value
                        is PutAction -> action.value
                        else -> error("Unexpected action: $action")
                    }

                    // Update the data map
                    data[action.key] = Entry(key = action.key, value = value, accessed = now)
                }

                is RemoveAction -> {
                    data.remove(key)
                }
            }
        }

        private fun evict(cache: FastCache<K, V>, key: K) {
            // Remove entry locally
            data.remove(key)
            // Remove entry from the cache
            cache.removeSilently(key)
        }

        private fun evictAllNecessary(now: Long, cache: FastCache<K, V>) {

            val toEvict = data.ascending()
                .takeWhile { (_, entry) -> now - entry.accessed >= ttlMs }

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

        /**
         * Processes the cache actions
         *
         * The following steps are taken:
         * - Group actions by key
         * - take the last action per key, as it is the current state
         * - update the total tracked size
         * - evict oldest as long as the capacity limit is violated
         */
        override fun process(cache: FastCache<K, V>, updates: ActionUpdates<K, V>) {
            val now = clock.millisNow()

            updates.lastByKey.forEach { (key, action) ->
                handle(now, key, action)
            }

            // Evict entries until we are below the max memory size
            evictAllNecessary(cache)
        }

        private fun handle(now: Long, key: K, action: Action<K, V>) {
            when (action) {
                is ReadAction, is PutAction -> {
                    val value = when (action) {
                        is ReadAction -> action.value
                        is PutAction -> action.value
                        else -> error("Unexpected action: $action")
                    }

                    // Update the data map
                    data[action.key] = Entry(key = action.key, value = value, accessed = now)
                }

                is RemoveAction -> {
                    data.remove(key)
                }
            }
        }

        private fun evict(cache: FastCache<K, V>, key: K) {
            // Remove entry locally
            data.remove(key)
            // Remove entry from the cache
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

        private val clock = Kronos.systemUtc
        private val data = ValueSortedMap<K, Entry<K, V>, Long> { it.accessed }
        private var totalSize = 0L

        /**
         * Processes the cache actions
         *
         * The following steps are taken:
         * - Group actions by key
         * - take the last action per key, as it is the current state
         * - update the total tracked size
         * - evict oldest as long as the memory limit is violated
         */
        override fun process(cache: FastCache<K, V>, updates: ActionUpdates<K, V>) {
            val now = clock.millisNow()

            updates.lastByKey.forEach { (key, action) ->
                handle(now, key, action)
            }

            // Evict entries until we are below the max memory size
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
                        else -> error("Unexpected action: $action")
                    }

                    // Update the total tracked size
                    val newKeySize = estimator.estimate(action.key)
                    val newValueSize = estimator.estimate(value)
                    val newSize = newKeySize + newValueSize

                    totalSize += newSize - currentSize

                    // Update the data map
                    data[action.key] = Entry(key = action.key, value = value, accessed = now, size = newSize)
                }

                is RemoveAction -> {
                    current?.let { remove(current) }
                }
            }
        }

        private fun remove(entry: Entry<K, V>) {
            // Update the total tracked size
            totalSize -= entry.size
            // Remove from data map
            data.remove(entry.key)
        }

        private fun evict(cache: FastCache<K, V>, entry: Entry<K, V>) {
            // Remove entry locally
            remove(entry)
            // Remove entry from the cache
            cache.removeSilently(entry.key)
        }

        private fun evictAllNecessary(cache: FastCache<K, V>) {
            var overflow = totalSize - maxMemorySize

            // println("Total size: $totalSize, max size: $maxMemorySize, overflow: $overflow")

            if (overflow <= 0) return

            while (overflow > 0 && data.isNotEmpty()) {
                val next = data.ascending().first().second

                overflow -= next.size

                evict(cache, next)
            }
        }
    }

    private class ActionProcessingLoop<K, V>(
        private val ref: WeakReference<FastCache<K, V>>,
        private val loopDelay: Duration,
    ) {
        suspend fun run() {
            ref.get()?.let { doRun(it) }
        }

        private suspend fun doRun(cache: FastCache<K, V>) {
            // println("LOOP RUN  |  ")

            val actions = cache.sync {
                // Get the currently stored actions
                cache.lastActions.also {
                    // Create a new instance
                    cache.lastActions = ArrayList(it.size)
                }
            }

            val updates = ActionUpdates(actions)

            cache.behaviours.forEach { behaviour ->
                behaviour.process(cache, updates)
            }

            triggerNextRun()
        }

        private suspend fun triggerNextRun() {
            delay(loopDelay)

            ref.get()?.let { doRun(it) }
        }
    }

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
    }

    private val lock = Any()

    private var lastActions = ArrayList<Action<K, V>>(1_000)

    private val map = mutableMapOf<K, V>()

    val keys: Set<K> get() = map.keys.toSet()

    val values: List<V> get() = map.values.toList()

    val entries: Map<K, V> get() = map.toMap()

    override fun has(key: K): Boolean = sync {
        map[key]?.also {
            addAction(ReadAction(key, it))
        } != null
    }

    override fun get(key: K): V? = sync {
        map[key]?.also {
            addAction(ReadAction(key, it))
        }
    }

    override fun put(key: K, value: V) = sync {
        map[key] = value

        addAction(PutAction(key, value))
    }

    override fun getOrPut(key: K, producer: () -> V): V = sync {
        map[key] ?: producer()
            .also { map[key] = it }
            .also { addAction(PutAction(key, it)) }
    }

    override fun remove(key: K): V? = sync {
        map.remove(key).also {
            addAction(RemoveAction(key))
        }
    }

    private fun removeSilently(key: K) = sync {
        // println("FastCache.removeSilently: $key  |  ")
        map.remove(key)
    }

    private fun addAction(action: Action<K, V>) {
        lastActions.add(action)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun <T> sync(noinline block: () -> T): T {
        return RunSync(lock, block)
    }
}
