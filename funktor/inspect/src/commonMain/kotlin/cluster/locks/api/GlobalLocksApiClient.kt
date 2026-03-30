package io.peekandpoke.funktor.inspect.cluster.locks.api

import io.peekandpoke.ultra.remote.ApiClient
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.TypedApiEndpoint.Get
import io.peekandpoke.ultra.remote.apiList
import io.peekandpoke.ultra.remote.call
import kotlinx.coroutines.flow.Flow

class GlobalLocksApiClient(config: Config) : ApiClient(config) {

    companion object {
        const val base = "/_/funktor/cluster"

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
