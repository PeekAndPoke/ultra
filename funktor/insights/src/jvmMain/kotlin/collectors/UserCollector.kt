package io.peekandpoke.funktor.insights.collectors

import io.ktor.server.application.ApplicationCall
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.security.user.UserProvider
import io.peekandpoke.ultra.security.user.UserRecord

class UserCollector(
    private val user: UserProvider,
) : InsightsCollector {

    override val key = KEY

    companion object {
        const val KEY = "user"
    }

    /** VUE-REF: `reference/collectors/UserCollector.kt` */
    data class Data(
        val user: UserRecord,
        val permissions: UserPermissions,
    ) : InsightsCollectorData

    override fun finish(call: ApplicationCall): Data = Data(
        user = user().record,
        permissions = user().permissions,
    )
}
