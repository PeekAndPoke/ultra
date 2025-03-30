package de.peekandpoke.ktorfx.cluster.storage

import de.peekandpoke.ultra.common.model.Paged
import de.peekandpoke.ultra.common.remote.ApiClient
import de.peekandpoke.ultra.common.remote.ApiResponse
import de.peekandpoke.ultra.common.remote.TypedApiEndpoint.Get
import de.peekandpoke.ultra.common.remote.api
import de.peekandpoke.ultra.common.remote.apiPaged
import de.peekandpoke.ultra.common.remote.call
import kotlinx.coroutines.flow.Flow

class RandomCacheStorageApiClient(config: Config) : ApiClient(config) {

    companion object {
        const val base = "/_/ktorfx/storage/random-cache"

        val List = Get(
            base,
            response = RawCacheDataModel.Head.serializer().apiPaged()
        )

        val Get = Get(
            "$base/{id}",
            response = RawCacheDataModel.serializer().api()
        )
    }

    fun list(
        search: String? = null, page: Int = 1, epp: Int = 50,
    ): Flow<ApiResponse<Paged<RawCacheDataModel.Head>>> = call(
        List(
            "search" to search,
            "page" to page.toString(),
            "epp" to epp.toString(),
        )
    )

    fun get(
        id: String,
    ): Flow<ApiResponse<RawCacheDataModel>> = call(
        Get("id" to id)
    )
}
