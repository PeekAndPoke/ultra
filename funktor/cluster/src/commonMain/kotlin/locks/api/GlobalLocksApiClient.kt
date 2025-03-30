package de.peekandpoke.ktorfx.cluster.locks.api

import de.peekandpoke.ultra.common.remote.ApiClient
import de.peekandpoke.ultra.common.remote.ApiResponse
import de.peekandpoke.ultra.common.remote.TypedApiEndpoint.Get
import de.peekandpoke.ultra.common.remote.apiList
import de.peekandpoke.ultra.common.remote.call
import kotlinx.coroutines.flow.Flow

class GlobalLocksApiClient(config: Config) : ApiClient(config) {

    companion object {
        const val base = "/_/ktorfx/cluster"

        val ListGlobalLocks = Get(
            "$base/global-locks",
            response = GlobalLockEntryModel.serializer().apiList()
        )

        val ListServerBeacons = Get(
            "$base/server-beacons",
            response = ServerBeaconModel.serializer().apiList()
        )
    }

    fun listGlobalLocks(): Flow<ApiResponse<List<GlobalLockEntryModel>>> =
        call(ListGlobalLocks())

    fun listServerBeacons(): Flow<ApiResponse<List<ServerBeaconModel>>> =
        call(ListServerBeacons())
}
