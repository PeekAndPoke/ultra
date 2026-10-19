package io.peekandpoke.funktor.cluster.locks.workers

import io.peekandpoke.funktor.cluster.locks.GlobalLocksProvider
import io.peekandpoke.funktor.cluster.locks.GlobalServerList
import io.peekandpoke.funktor.cluster.locks.domain.GlobalLockEntry
import io.peekandpoke.funktor.cluster.locks.domain.isLegacy
import io.peekandpoke.funktor.cluster.workers.StateProvider
import io.peekandpoke.funktor.cluster.workers.Worker
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.log.Log
import kotlin.time.Duration.Companion.minutes

class GlobalLocksCleanupWorker(
    private val servers: GlobalServerList,
    private val globalLocks: GlobalLocksProvider,
    private val log: Log,
) : Worker {

    override val shouldRun: (lastRun: MpInstant, now: MpInstant) -> Boolean = Worker.Every.seconds(10)

    override suspend fun execute(state: StateProvider) {

//        log.debug("=== Running GlobalLocksCleanupWorker")

        val now = MpInstant.now()
        val serverExpiresAt = now.minus(3.minutes)

        // Right after a server starts, the beacons have not been read yet: an empty list would make every server dead.
        if (servers.isPopulated) {
            val aliveServerIds: Set<String> = servers.getAll()
                .filter { it.lastPing > serverExpiresAt }
                .map { it.serverId }
                .toSet()

            releaseHeldByDeadServers(aliveServerIds)
        }

        val expired = globalLocks.releaseExpired()

        if (expired.isNotEmpty()) {
            val mapped = expired.mapped()

            log.warning(
                """
                    Removed ${expired.size} Global Locks that where expired.
                    Server => Lock => Created => Last heartbeat, or expiry:

                """.trimIndent() + mapped
            )
        }
    }

    private suspend fun releaseHeldByDeadServers(aliveServerIds: Set<String>) {
        // Only locks without a token: a lock with one is dead once its heartbeat stops, sooner than its server's beacon.
        val heldByDeadServers = globalLocks.releaseBy { lock -> lock.isLegacy && lock.serverId !in aliveServerIds }

        if (heldByDeadServers.isNotEmpty()) {
            val mapped = heldByDeadServers.mapped()

            log.warning(
                """
                    Removed ${heldByDeadServers.size} Global Locks that where held by dead servers.
                    Server => Lock => Created => Last heartbeat, or expiry:

                """.trimIndent() + mapped
            )
        }
    }

    private fun List<GlobalLockEntry>.mapped() = joinToString("\n") {
        // The field a lock is judged by: `expires` without heartbeats, else the last heartbeat.
        val decisive = when (it.isLegacy) {
            true -> "expires ${it.expires.toIsoString()}"
            false -> "last heartbeat ${it.lastHeartbeat.toIsoString()}"
        }

        "${it.serverId} => ${it.key} => ${it.created.toIsoString()} => $decisive"
    }
}
