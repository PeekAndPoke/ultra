package io.peekandpoke.kraft.utils

import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import kotlinx.coroutines.flow.Flow

/**
 * Lazily loads and caches async data streams, identified by [TypedKey].
 *
 * Each key is loaded at most once; subsequent calls return the cached stream.
 */
class LazyLoader {

    private val keys: MutableMap<TypedKey<*>, Stream<*>> = mutableMapOf()

    /**
     * Returns a [Stream] for the given [key], loading it from [flow] on first access.
     *
     * @param key unique identifier for the data
     * @param default initial value emitted before the flow produces data
     * @param flow factory for the data flow, called only on first access
     */
    fun <T> add(key: TypedKey<T>, default: T, flow: suspend () -> Flow<T>): Stream<T> {
        @Suppress("UNCHECKED_CAST")
        return keys.getOrPut(key) {
            StreamSource(default).also { stream ->
                launch {
                    flow().collect { stream(it) }
                }
            }
        } as Stream<T>
    }
}
