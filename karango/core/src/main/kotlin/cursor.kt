package de.peekandpoke.karango

import com.arangodb.ArangoCursorAsync
import de.peekandpoke.karango.slumber.KarangoCodec
import de.peekandpoke.ultra.vault.Cursor
import de.peekandpoke.ultra.vault.EntityCache
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.TypedQuery
import de.peekandpoke.ultra.vault.profiling.QueryProfiler
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import java.io.Closeable

// TODO: [KARANGO] make the cursor fully async by getting rid of the internal [Iterator]
//                 make use of Kotlin's Sequence api
class KarangoCursor<T>(
    arangoCursor: ArangoCursorAsync<*>,
    override val query: TypedQuery<T>,
    val codec: KarangoCodec,
    private val profiler: QueryProfiler.Entry,
) : Cursor<T>, Closeable {

    private val lock = Any()
    private var inner = arangoCursor

    private val type = query.root.innerType().type

    /** List of pairs, where first is the raw data and second the deserialized data */
    private var data = emptyList<T>()

    private inner class It : Iterator<T> {

        private var idx = 0

        override fun hasNext(): Boolean {
            return idx < count
        }

        override fun next(): T {

            // Make a copy of local state to avoid error, when multiple threads call next() in parallel
            val currentIdx = idx
            val currentData = data

            if (currentIdx >= currentData.size) {
                data = currentData.plus(
                    inner.result.map {
                        deserialize(it)
                    }
                )

                // Is there another chunk available?
                if (inner.hasMore()) {
                    // TODO: [Karango] get rid of this blocking call
                    runBlocking {
                        inner = inner.nextBatch().await()
                    }
                }
            }

            // Get the next entry from the low level iterator
            val entry = data[currentIdx]
            // increase the idx
            idx = currentIdx + 1

            return entry
        }

        private fun deserialize(raw: Any?): T {
            val deserialized = profiler.measureDeserializer {
                // Try to map the entry onto the target type
                @Suppress("UNCHECKED_CAST")
                codec.awake(type, raw) as T
            }

            // If the result is a Stored we put it into the EntityCache
            if (deserialized is Stored<*>) {
                codec.entityCache.put(deserialized._id, deserialized)
            }

            return deserialized
        }
    }

    override val entityCache: EntityCache = codec.entityCache

    override val count: Long = inner.count?.toLong() ?: 0L

    override val fullCount: Long? get() = stats?.fullCount

    val stats get() = inner.extra?.stats

    override fun iterator(): Iterator<T> = It()

    override val timeMs get() = profiler.totalNs / 1_000_000.0

    override fun close() {
        try {
            inner.close()
        } finally {
        }
    }
}
