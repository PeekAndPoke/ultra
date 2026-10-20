package io.peekandpoke.funktor

import io.peekandpoke.funktor.auth.AuthSystem
import io.peekandpoke.funktor.cluster.workers.StateProvider
import io.peekandpoke.funktor.cluster.workers.Worker
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.log.Log

/**
 * Retires the never-activated accounts of every realm once they outlive `RealmTokenConfig.pendingAccountLifetime`.
 *
 * Registered by the [Funktor] module. It lives here, not in `funktor/auth`, because only `funktor/all` sees both
 * auth and the cluster's workers (auth → cluster would be a cycle through `funktor/inspect`). Expiry is also
 * enforced at every use of an account, so this frees what nobody touches again.
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
