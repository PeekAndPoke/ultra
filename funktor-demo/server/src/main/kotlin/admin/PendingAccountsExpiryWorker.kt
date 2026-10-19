package io.peekandpoke.funktor.demo.server.admin

import io.peekandpoke.funktor.auth.AuthSystem
import io.peekandpoke.funktor.cluster.workers.StateProvider
import io.peekandpoke.funktor.cluster.workers.Worker
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.log.Log

/**
 * Retires the never-activated accounts of every realm once they outlive `RealmTokenConfig.pendingAccountLifetime`.
 *
 * Garbage collection, not the guard: expiry is enforced at use (activation, resend, sign-up) whether or not
 * this runs. Lives in the demo because `funktor/auth` cannot depend on `funktor/cluster` (that would be a
 * cycle through `funktor/inspect`); an app outside `funktor/all` registers the same few lines.
 */
class PendingAccountsExpiryWorker(
    authSystem: Lazy<AuthSystem>,
    private val log: Log,
) : Worker {

    private val authSystem by authSystem

    override val shouldRun: (lastRun: MpInstant, now: MpInstant) -> Boolean = Worker.Every.minutes(15)

    override suspend fun execute(state: StateProvider) {
        val retired = authSystem.retireExpiredPendingAccounts()

        if (retired > 0) {
            log.info("Retired $retired expired pending account(s)")
        }
    }
}
