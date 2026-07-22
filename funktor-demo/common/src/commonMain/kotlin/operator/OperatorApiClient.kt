package io.peekandpoke.funktor.demo.common.operator

import io.peekandpoke.ultra.remote.ApiClient
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.TypedApiEndpoint
import io.peekandpoke.ultra.remote.api
import io.peekandpoke.ultra.remote.call
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** Aggregate stats shown on the operator console dashboard. */
@Serializable
data class OperatorDashboardStats(
    val orgs: Int = 0,
    val branches: Int = 0,
    /**
     * Org count per lifecycle status, keyed by the server-side `OrgStatus` name. A map (rather
     * than one field per status) stays additive-safe when the status enum grows, and string keys
     * keep an older client deserializing a newer server's payload.
     */
    val orgsByStatus: Map<String, Int> = emptyMap(),
    val operators: Int = 0,
)

/** Typed client for the operator (platform super-user) console API. */
class OperatorApiClient(config: Config) : ApiClient(config) {

    companion object {
        private const val BASE = "/api/operator"

        val GetDashboardStats = TypedApiEndpoint.Get(
            uri = "$BASE/dashboard/stats",
            response = OperatorDashboardStats.serializer().api(),
        )
    }

    fun getDashboardStats(): Flow<ApiResponse<OperatorDashboardStats>> = call(
        GetDashboardStats()
    )
}
