package de.peekandpoke.funktor.cluster.locks.workers

import de.peekandpoke.funktor.cluster.locks.GlobalLocksProvider
import de.peekandpoke.funktor.cluster.locks.GlobalServerList
import de.peekandpoke.funktor.cluster.locks.domain.GlobalLockEntry
import de.peekandpoke.funktor.cluster.workers.StateProvider
import de.peekandpoke.funktor.cluster.workers.Worker
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.logging.Log
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

        val beacons = servers.getAll()

        val aliveServerIds: Set<String> = beacons
            .filter { it.lastPing > serverExpiresAt }
            .map { it.serverId }
            .toSet()

        val heldByDeadServers = globalLocks.releaseBy { lock -> lock.serverId !in aliveServerIds }

        if (heldByDeadServers.isNotEmpty()) {
            val mapped = heldByDeadServers.mapped()

            log.warning(
                """
                    Removed ${heldByDeadServers.size} Global Locks that where held by dead servers.
                    Server => Lock => Created => Expires:

                """.trimIndent() + mapped
            )
        }

        val expired = globalLocks.releaseBy { lock -> now > lock.expires }

        if (expired.isNotEmpty()) {
            val mapped = expired.mapped()

            log.warning(
                """
                    Removed ${expired.size} Global Locks that where expired.
                    Server => Lock => Created => Expires:

                """.trimIndent() + mapped
            )
        }
    }

    private fun List<GlobalLockEntry>.mapped() = joinToString("\n") {
        "${it.serverId} => ${it.key} => ${it.created.toIsoString()} => ${it.expires.toIsoString()}"
    }
}
