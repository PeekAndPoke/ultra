package de.peekandpoke.ultra.common.cache

import de.peekandpoke.ultra.common.RunSync
import de.peekandpoke.ultra.common.WeakReference
import de.peekandpoke.ultra.common.datetime.Kronos
import korlibs.time.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

// TODO: test me
class FastCache<K, V>(
    private val behaviours: List<Behaviour<K, V>>,
    scope: CoroutineScope = Cache.defaultCoroutineScope,
    private val loopDelay: Duration = 100.milliseconds,
) : Cache<K, V> {

    class Builder<K, V>(private val scope: CoroutineScope = Cache.defaultCoroutineScope) {
        private val behaviours = mutableListOf<Behaviour<K, V>>()

        fun addBehaviour(behaviour: Behaviour<K, V>) = apply { behaviours.add(behaviour) }

        fun expireAfterAccess(ttl: Duration) = addBehaviour(ExpireAfterAccessBehaviour(ttl))

        fun maxEntries(maxEntries: Int) = addBehaviour(MaxEntriesBehaviour(maxEntries))

        fun maxMemoryUsage(maxMemorySize: Long, estimator: ObjectSizeEstimator = ObjectSizeEstimator()) =
            addBehaviour(MaxMemoryUsageBehaviour(maxMemorySize, estimator))

        fun build() = FastCache(behaviours, scope)
    }

    sealed interface Action<K, V> {
        val key: K
    }

    class ReadAction<K, V>(override val key: K) : Action<K, V>
    class PutAction<K, V>(override val key: K, val value: V) : Action<K, V>
    class RemoveAction<K, V>(override val key: K) : Action<K, V>

    interface Behaviour<K, V> {
        fun process(cache: FastCache<K, V>, actions: List<Action<K, V>>)
    }

    class ExpireAfterAccessBehaviour<K, V>(ttl: Duration) : Behaviour<K, V> {
        private val durationMs = ttl.inWholeMilliseconds
        private val clock = Kronos.systemUtc
        private val lastAccess = mutableMapOf<K, Long>()

        override fun process(cache: FastCache<K, V>, actions: List<Action<K, V>>) {
            val now = clock.millisNow()
            // process all actions
            actions.forEach { action ->
                when (action) {
                    is ReadAction, is PutAction -> {
                        lastAccess[action.key] = now
                    }

                    is RemoveAction -> {
                        lastAccess.remove(action.key)
                    }
                }
            }

            // check for expired entries
            lastAccess.filter { it.value + durationMs < now }.forEach { (key, _) ->
                expire(cache, key)
            }
        }

        private fun expire(cache: FastCache<K, V>, key: K) {
            lastAccess.remove(key)
            cache.removeSilently(key)
        }
    }

    class MaxEntriesBehaviour<K, V>(val maxEntries: Int) : Behaviour<K, V> {
        override fun process(cache: FastCache<K, V>, actions: List<Action<K, V>>) {
            cache.entries.keys
                .take(cache.entries.size - maxEntries)
                .forEach { key ->
                    cache.removeSilently(key)
                }
        }
    }

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
        private val data = ValueSortedMap<K, Entry<K, V>, Long> {
            it.accessed
        }

        private var totalSize = 0L

        override fun process(cache: FastCache<K, V>, actions: List<Action<K, V>>) {
            val now = clock.millisNow()

            actions.forEach { action ->
                val current = data[action.key]
                val currentSize = current?.size ?: 0

                when (action) {
                    is ReadAction -> {
                        // Update the last access time in data map
                        current?.let {
                            data[action.key] = current.copy(accessed = now)
                        }
                    }

                    is PutAction -> {
                        // Update the total tracked size
                        val newKeySize = estimator.estimate(action.key)
                        val newValueSize = estimator.estimate(action.value)
                        val newSize = newKeySize + newValueSize

                        totalSize += newSize - currentSize

                        // Update the data map
                        data[action.key] = Entry(
                            key = action.key,
                            value = action.value,
                            accessed = now,
                            size = newSize,
                        )
                    }

                    is RemoveAction -> {
                        current?.let {
                            remove(current)
                        }
                        // Update the total tracked size
                        totalSize -= currentSize
                        // Remove from data map
                        data.remove(action.key)
                    }
                }
            }

            // Evict entries until we are below the max memory size
            evictAllNecessary(cache)
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
            do {
                val cache = ref.get() ?: return

                // println("Loop.run")

                val actions = cache.sync {
                    // Get the currently stored actions
                    cache.lastActions.also {
                        // Create a new instance
                        cache.lastActions = ArrayList(it.size)
                    }
                }

                if (actions.isNotEmpty()) {
                    cache.behaviours.forEach { behaviour ->
                        behaviour.process(cache, actions)
                    }
                }

                delay(loopDelay)
            } while (true)
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
        map.containsKey(key).also {
            addAction(ReadAction(key))
        }
    }

    override fun get(key: K): V? = sync {
        map[key].also {
            addAction(ReadAction(key))
        }
    }

    override fun put(key: K, value: V) = sync {
        map[key] = value

        addAction(PutAction(key, value))
    }

    override fun remove(key: K): V? = sync {
        map.remove(key).also {
            addAction(RemoveAction(key))
        }
    }

    override fun getOrPut(key: K, producer: () -> V): V = sync {
        map[key] ?: producer()
            .also { map[key] = it }
            .also { addAction(PutAction(key, it)) }
    }

    private fun removeSilently(key: K) = sync {
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
