package de.peekandpoke.kraft.utils

import de.peekandpoke.kraft.streams.Stream
import de.peekandpoke.kraft.streams.StreamSource
import de.peekandpoke.ultra.common.TypedKey
import kotlinx.coroutines.flow.Flow

class LazyLoader {

    private val keys: MutableMap<TypedKey<*>, Stream<*>> = mutableMapOf()

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

