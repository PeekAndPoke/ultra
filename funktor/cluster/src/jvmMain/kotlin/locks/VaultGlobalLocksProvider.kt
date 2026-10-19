package io.peekandpoke.funktor.cluster.locks

import io.peekandpoke.funktor.cluster.locks.domain.GlobalLockEntry
import io.peekandpoke.funktor.cluster.locks.domain.isLegacy
import io.peekandpoke.funktor.core.Retry.retry
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.log.Log
import io.peekandpoke.ultra.log.LogLevel
import io.peekandpoke.ultra.log.NullLog
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Global Lock Provider that stores its locks in a [Repo].
 *
 * While a handler runs, the holder refreshes [GlobalLockEntry.lastHeartbeat] every [heartbeatInterval]. A lock whose
 * last heartbeat is older than [ttl] is dead and may be taken over, however long it has been held. A lock written by
 * a version without heartbeats (empty [GlobalLockEntry.token]) is dead once its [GlobalLockEntry.expires] has passed.
 *
 * Operating constraints:
 * - [ttl] must exceed the worst database stall plus the clock skew between servers plus one [heartbeatInterval];
 *   otherwise a live lock can be taken over.
 * - [ttl] and [heartbeatInterval] must be the same on every server, so changing them takes two deploys: a server
 *   judges other servers' locks by its own [ttl].
 * - A lock makes concurrent work unlikely, not impossible: a write that protects an invariant also needs a
 *   conditional write.
 *
 * When acquiring the lock fails, this provider will wait until it retries.
 * The duration of this delay can be configured by [retryDelayMs].
 */
open class VaultGlobalLocksProvider(
    val repository: Repo,
    val serverId: GlobalServerId,
    val retryDelayMs: Long = 100,
    val ttl: Duration = 60.seconds,
    val heartbeatInterval: Duration = ttl / 3,
    private val log: Log = NullLog,
) : GlobalLocksProviderBase() {

    /**
     * The storage of the provider.
     *
     * [insert] must fail when the key is already taken. The operations below must be atomic.
     */
    interface Repo : Repository<GlobalLockEntry> {

        /** Sets heartbeat and expiry of the lock [key] iff it is held with [token]; returns whether it was. */
        suspend fun heartbeat(key: String, token: String, heartbeatAt: MpInstant, expiresAt: MpInstant): Boolean

        /** Removes the lock [key] iff it is held with [token]; returns whether it was. */
        suspend fun removeIfHeld(key: String, token: String): Boolean

        /** Removes the lock [entry] iff it is unchanged since it was read; returns whether it was. */
        suspend fun removeIfUnchanged(entry: GlobalLockEntry): Boolean
    }

    companion object {
        private val cleanRegex = "[^a-zA-Z0-9]".toRegex()

        /** How far each write moves [GlobalLockEntry.expires], which versions without heartbeats judge a lock by. */
        internal val legacyExpiry: Duration = 15.minutes

        // Its own threads, beyond the limit of Dispatchers.IO, which blocking handlers may saturate. Few suffice:
        // a beat suspends while the database works.
        private val heartbeatDispatcher = Dispatchers.IO.limitedParallelism(4, "global-locks-heartbeat")
    }

    init {
        require(heartbeatInterval.isPositive() && heartbeatInterval < ttl) {
            "heartbeatInterval ($heartbeatInterval) must be positive and shorter than ttl ($ttl)"
        }
        // Otherwise a server without heartbeats, judging by `expires`, takes over a lock this one still counts as alive.
        require(ttl <= legacyExpiry) {
            "ttl ($ttl) must not exceed $legacyExpiry"
        }
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
        // Identifies this acquisition: the heartbeat and the release only touch a lock that still carries it.
        val token = UUID.randomUUID().toString()
        var running = true

        while (running) {

            val acquired = try {
                val now = MpInstant.now()

                repository.insert(
                    key = key,
                    GlobalLockEntry(
                        key = key,
                        serverId = serverId.getId(),
                        created = now,
                        expires = now.plus(legacyExpiry),
                        token = token,
                        lastHeartbeat = now,
                    )
                )

                true
            } catch (_: Throwable) {
                // We could not save, which means that the lock is already present in the database.
                // If its holder has gone silent, take it over so the next retry can acquire a fresh lock.
                // Without this, a dead holder blocks the cluster until GlobalLocksCleanupWorker runs (every 10s).
                tryTakeOverDeadLock(key)
                false
            }

            if (acquired) {
                try {
                    coroutineScope {
                        // Off the handler's dispatcher: a handler that blocks its thread must not starve the heartbeat.
                        val heartbeat = launch(heartbeatDispatcher) { keepAlive(key = key, token = token) }

                        try {
                            val result = handler()
                            emit(result)
                        } finally {
                            heartbeat.cancel()
                        }
                    }
                } catch (e: Throwable) {
                    throw LocksException.Execution(cause = e)
                } finally {
                    running = false

                    // NonCancellable: a cancelled handler must still release its lock, not leave it for the ttl.
                    // Only our own lock is removed: one taken over meanwhile belongs to its new holder.
                    // We will retry to delete the lock if deletion fails
                    withContext(NonCancellable) {
                        retry(attempts = 10, delays = 10.milliseconds..1000.milliseconds) {
                            repository.removeIfHeld(key = key, token = token)
                        }
                    }
                }
            } else {
                delay(max(10L, retryDelayMs))

                val timePassedMs = System.currentTimeMillis() - launchedAt

                if (timePassedMs > timeoutMs) {
                    throw LocksException.Timeout(key = key, duration = timeout)
                }
            }
        }
    }

    /** Beats until cancelled. Gives up, with a warning, once the lock turns out to be no longer ours. */
    private suspend fun keepAlive(key: String, token: String) {
        while (true) {
            delay(heartbeatInterval)

            val now = MpInstant.now()

            val held = try {
                // A hanging call counts as a failed beat, rather than silently stopping the beating.
                withTimeout(heartbeatInterval) {
                    repository.heartbeat(key = key, token = token, heartbeatAt = now, expiresAt = now.plus(legacyExpiry))
                }
            } catch (e: Throwable) {
                // Only our own cancellation ends the beating; any other failure is retried with the next beat.
                currentCoroutineContext().ensureActive()
                log.log(LogLevel.WARNING, "Global lock '$key': heartbeat failed, retrying in $heartbeatInterval", e)
                continue
            }

            if (!held) {
                log.warning(
                    "Global lock '$key' was released or taken over while its handler was still running; " +
                            "the guarded work may run twice"
                )
                return
            }
        }
    }

    private suspend fun tryTakeOverDeadLock(key: String) {
        val existing = repository.findById(key)?.resolve() ?: return

        if (isDead(existing, MpInstant.now())) {
            // Only what we read is removed: a lock refreshed or re-acquired meanwhile stays.
            repository.removeIfUnchanged(existing)
        }
    }

    private fun isDead(entry: GlobalLockEntry, now: MpInstant): Boolean = when (entry.isLegacy) {
        // Written by a version without heartbeats, which keeps `expires` as its only rule.
        true -> now > entry.expires
        false -> now - entry.lastHeartbeat > ttl
    }

    override suspend fun list(): List<GlobalLockEntry> {
        return repository.findAll().map { it.resolve() }
    }

    override suspend fun releaseBy(filter: (GlobalLockEntry) -> Boolean): List<GlobalLockEntry> {
        // Only locks unchanged since they were read are removed: one refreshed or re-acquired meanwhile stays.
        return list().filter(filter).filter { repository.removeIfUnchanged(it) }
    }

    override suspend fun releaseExpired(): List<GlobalLockEntry> {
        val now = MpInstant.now()

        return releaseBy { isDead(it, now) }
    }
}
