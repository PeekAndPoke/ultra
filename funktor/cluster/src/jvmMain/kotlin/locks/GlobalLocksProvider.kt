package de.peekandpoke.ktorfx.cluster.locks

import de.peekandpoke.ktorfx.cluster.locks.domain.GlobalLockEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlin.time.Duration

/**
 * Defines a global lock provider
 */
interface GlobalLocksProvider {

    /**
     * Acquires a lock for the given [key] waiting for max [timeout].
     *
     * When [timeout] is null the provider will call the [handler] eventually, when the lock is released.
     *
     * When the lock is acquired the [handler] is invoked.
     *
     * When the timeout is hit a [LocksException.Timeout] is raised.
     * When the handler throws an exception it will be wrapped in a [LocksException.Execution].
     */
    @Throws(LocksException.Timeout::class, LocksException.Execution::class)
    fun <T> acquire(
        key: String,
        timeout: Duration = Duration.ZERO,
        handler: suspend () -> T,
    ): Flow<T>

    /**
     * Acquires a lock and immediately executes the [handler]
     *
     * See [acquire]
     *
     * When the timeout is hit a [LocksException.Timeout] is raised.
     * When the handler throws an exception it will be wrapped in a [LocksException.Execution].
     */
    @Throws(LocksException.Timeout::class, LocksException.Execution::class)
    suspend fun <T> lock(
        key: String,
        timeout: Duration = Duration.ZERO,
        handler: suspend () -> T,
    ): T {
        return supervisorScope {
            withContext(Dispatchers.IO) {
                acquire(
                    key = key,
                    timeout = timeout,
                    handler = handler
                ).single()
            }
        }
    }

    /**
     * Tries to acquire a lock and immediately execute the [handler]
     *
     * See [acquire]
     *
     * When the timeout is null will be returned.
     * When the handler throws an exception it will be wrapped in a [LocksException.Execution].
     */
    @Throws(LocksException.Execution::class)
    suspend fun <T> tryToLock(
        key: String,
        timeout: Duration = Duration.ZERO,
        handler: suspend () -> T,
    ): T? {
        return try {
            lock(
                key = key,
                timeout = timeout,
                handler = handler
            )
        } catch (e: LocksException.Timeout) {
            null
        }
    }

    /**
     * Lists all currently active locks.
     */
    suspend fun list(): List<GlobalLockEntry>

    /**
     * Releases all Locks the match the [filter].
     *
     * Returns a list of locks that where released.
     */
    suspend fun releaseBy(filter: (GlobalLockEntry) -> Boolean): List<GlobalLockEntry>

    /**
     * Releases all Locks that are held by the given [serverId].
     */
    suspend fun releaseByServerId(serverId: GlobalServerId) = releaseBy { it.serverId == serverId.getId() }
}
