package de.peekandpoke.funktor.cluster.storage

import de.peekandpoke.ultra.common.model.Paged
import de.peekandpoke.ultra.common.remote.ApiClient
import de.peekandpoke.ultra.common.remote.ApiResponse
import de.peekandpoke.ultra.common.remote.TypedApiEndpoint.Get
import de.peekandpoke.ultra.common.remote.api
import de.peekandpoke.ultra.common.remote.apiPaged
import de.peekandpoke.ultra.common.remote.call
import kotlinx.coroutines.flow.Flow

class RandomDataStorageApiClient(config: Config) : ApiClient(config) {

    companion object {
        const val base = "/_/funktor/storage/random-data"

        val List = Get(
            base,
            response = RawRandomDataModel.Head.serializer().apiPaged()
        )

        val Get = Get(
            "$base/{id}",
            response = RawRandomDataModel.serializer().api()
        )
    }

    fun list(
        search: String? = null, page: Int = 1, epp: Int = 50,
    ): Flow<ApiResponse<Paged<RawRandomDataModel.Head>>> = call(
        List(
            "search" to search,
            "page" to page.toString(),
            "epp" to epp.toString(),
        )
    )

    fun get(
        id: String,
    ): Flow<ApiResponse<RawRandomDataModel>> = call(
        Get("id" to id)
    )
}
