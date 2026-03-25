package de.peekandpoke.funktor.cluster.storage

import de.peekandpoke.ultra.model.Paged
import de.peekandpoke.ultra.remote.ApiClient
import de.peekandpoke.ultra.remote.ApiResponse
import de.peekandpoke.ultra.remote.TypedApiEndpoint.Get
import de.peekandpoke.ultra.remote.api
import de.peekandpoke.ultra.remote.apiPaged
import de.peekandpoke.ultra.remote.call
import kotlinx.coroutines.flow.Flow

class RandomCacheStorageApiClient(config: Config) : ApiClient(config) {

    companion object {
        const val base = "/_/funktor/storage/random-cache"

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
