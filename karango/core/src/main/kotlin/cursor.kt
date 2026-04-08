package io.peekandpoke.karango

import com.arangodb.ArangoCursorAsync
import com.arangodb.entity.CursorStats
import io.peekandpoke.karango.slumber.KarangoCodec
import io.peekandpoke.karango.vault.AqlTypedQuery
import io.peekandpoke.ultra.vault.Cursor
import io.peekandpoke.ultra.vault.EntityCache
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.profiling.QueryProfiler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.await
import java.io.Closeable

/**
 * Streams query results from ArangoDB via [Flow] with non-blocking batch pagination.
 *
 * Results are deserialized via [KarangoCodec] as they are emitted. [Stored] entities are
 * automatically added to the [EntityCache] on deserialization.
 *
 * Chunk fetching is fully suspend-based — no runBlocking.
 */
class KarangoCursor<T>(
    private val itemFlow: Flow<T>,
    override val query: AqlTypedQuery<T>,
    override val entityCache: EntityCache,
    override val count: Long,
    override val fullCount: Long?,
    val stats: CursorStats?,
    private val profiler: QueryProfiler.Entry,
) : Cursor<T>, Closeable {

    companion object {
        fun <T> create(
            arangoCursor: ArangoCursorAsync<*>,
            query: AqlTypedQuery<T>,
            codec: KarangoCodec,
            profiler: QueryProfiler.Entry,
        ): KarangoCursor<T> {
            val type = query.root.innerType().type
            val count = arangoCursor.count?.toLong() ?: 0L
            val fullCount = arangoCursor.extra?.stats?.fullCount
            val stats = arangoCursor.extra?.stats

            val itemFlow = flow {
                var cursor = arangoCursor
                while (true) {
                    for (raw in cursor.result) {
                        val deserialized = profiler.measureDeserializer {
                            @Suppress("UNCHECKED_CAST")
                            codec.awake(type, raw) as T
                        }

                        if (deserialized is Stored<*>) {
                            codec.entityCache.put(deserialized._id, deserialized)
                        }

                        emit(deserialized)
                    }

                    if (!cursor.hasMore()) break
                    cursor = cursor.nextBatch().await()
                }
            }

            return KarangoCursor(
                itemFlow = itemFlow,
                query = query,
                entityCache = codec.entityCache,
                count = count,
                fullCount = fullCount,
                stats = stats,
                profiler = profiler,
            )
        }
    }

    override fun asFlow(): Flow<T> = itemFlow

    override val timeMs get() = profiler.totalNs / 1_000_000.0

    override fun close() {
        // No-op: the flow handles cursor lifecycle during collection
    }
}
