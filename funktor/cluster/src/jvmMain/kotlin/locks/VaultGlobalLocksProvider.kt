package de.peekandpoke.ktorfx.cluster.locks

import de.peekandpoke.ktorfx.cluster.locks.domain.GlobalLockEntry
import de.peekandpoke.ktorfx.core.Retry.retry
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.vault.RemoveResult
import de.peekandpoke.ultra.vault.Repository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Global Lock Provider that uses a Vault [Repository].
 *
 * The [repository] must ensure that these operations are atomic:
 * - save
 * - remove
 *
 * The 'Save' operation must:
 * 1. Return a Stored<LockEntry> iff a new entry could be created in the database. This means that the lock is acquired.
 * 2. Throw an exception iff a new entry could NOT be created in the database. This means that the lock is already taken.
 *
 * The 'Remove' operation must always succeed:
 * 1. If there is an entry in the database it must be removed.
 * 2. If there is NO entry in the database it must return a [RemoveResult] with count = 0.
 *
 * When acquiring the lock fails, this provider will wait until it retries.
 * The duration of this delay can be configured by [retryDelayMs].
 */
open class VaultGlobalLocksProvider(
    val repository: Repository<GlobalLockEntry>,
    val serverId: GlobalServerId,
    val retryDelayMs: Long = 100,
) : GlobalLocksProviderBase() {

    companion object {
        private val cleanRegex = "[^a-zA-Z0-9]".toRegex()
    }

    /**
     * @see GlobalLocksProvider.acquire
     */
    override suspend fun <T> FlowCollector<T>.doAcquire(key: String, timeout: Duration, handler: suspend () -> T) {
        return doAcquireInternal(
            key = key.replace(cleanRegex, "-"),
            timeout = timeout,
            handler = handler,
        )
    }

    private suspend fun <T> FlowCollector<T>.doAcquireInternal(
        key: String,
        timeout: Duration,
        handler: suspend () -> T,
    ) {
        val timeoutMs = timeout.inWholeMilliseconds
        val launchedAt = System.currentTimeMillis()
        var running = true

        while (running) {

            val lock = try {
                val now = MpInstant.now()

                repository.insert(
                    key = key,
                    GlobalLockEntry(
                        key = key,
                        serverId = serverId.getId(),
                        created = now,
                        expires = now.plus(15.minutes),
                    )
                )
            } catch (_: Throwable) {
                // We could not save, which means that the lock is already present in the database
                null
            }

            if (lock != null) {
                try {
                    val result = handler()
                    emit(result)
                } catch (e: Throwable) {
                    throw LocksException.Execution(cause = e)
                } finally {
                    running = false

                    // We will retry to delete the lock if deletion fails
                    retry(attempts = 10, delays = 10.milliseconds..1000.milliseconds) {
                        repository.remove(lock)
                    }
                }
            } else {
                delay(max(10L, retryDelayMs))

                val timePassedMs = System.currentTimeMillis() - launchedAt

//                println("Could not get lock for '$key' after $timePassedMs")

                if (timePassedMs > timeoutMs) {
                    throw LocksException.Timeout(key = key, duration = timeout)
                }
            }
        }
    }

    override suspend fun list(): List<GlobalLockEntry> {
        return repository.findAll().map { it.value }
    }

    override suspend fun releaseBy(filter: (GlobalLockEntry) -> Boolean): List<GlobalLockEntry> {
        val toBeReleased = repository.findAll().filter { filter(it.value) }

        return toBeReleased
            .onEach { repository.remove(it) }
            .map { it.value }
    }
}
