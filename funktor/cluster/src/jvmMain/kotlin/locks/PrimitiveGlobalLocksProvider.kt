package de.peekandpoke.ktorfx.cluster.locks

import de.peekandpoke.ktorfx.cluster.locks.domain.GlobalLockEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlin.math.max
import kotlin.time.Duration

/**
 * This lock provider is very primitive and does NOT work in a multi-server setup.
 *
 * It will only acquire locks on a single server.
 */
class PrimitiveGlobalLocksProvider(private val retryDelayMs: Long = 1) : GlobalLocksProviderBase() {

    private object Storage {
        val lockMap = mutableMapOf<String, MutableList<QueueEntry>>()
    }

    private class QueueEntry(
        val handler: suspend () -> Any?,
    )

    override suspend fun <T> FlowCollector<T>.doAcquire(key: String, timeout: Duration, handler: suspend () -> T) {

        val timeoutMs = timeout.inWholeMilliseconds
        val launchedAt = System.currentTimeMillis()

        val entry = QueueEntry(handler = handler)

        val list = synchronized(Storage) {
            Storage.lockMap
                .getOrPut(key) { mutableListOf() }
                .apply { add(entry) }
        }

        var running = true

        while (running) {

            val isFirst = synchronized(list) {
                list.first() === entry
            }

            if (isFirst) {
                try {
                    val result = entry.handler()
                    @Suppress("UNCHECKED_CAST")
                    emit(result as T)
                } catch (e: Throwable) {
                    throw LocksException.Execution(cause = e)
                } finally {
                    synchronized(list) {
                        list.remove(entry)
                    }

                    running = false
                }

            } else {
                // Wait a bit until retry
                delay(max(1L, retryDelayMs))

                if (System.currentTimeMillis() - launchedAt > timeoutMs) {
                    synchronized(list) {
                        list.remove(entry)
                    }

                    throw LocksException.Timeout(key = key, duration = timeout)
                }
            }
        }
    }

    override suspend fun list(): List<GlobalLockEntry> {
        return emptyList()
    }

    override suspend fun releaseBy(filter: (GlobalLockEntry) -> Boolean): List<GlobalLockEntry> {
        return emptyList()
    }

    fun numEntriesFor(key: String): Int = synchronized(Storage) {
        Storage.lockMap[key]?.size ?: 0
    }
}
