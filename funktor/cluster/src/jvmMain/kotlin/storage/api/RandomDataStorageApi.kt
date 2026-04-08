package io.peekandpoke.funktor.cluster.storage.api

import io.ktor.server.routing.*
import io.peekandpoke.funktor.cluster.cluster
import io.peekandpoke.funktor.cluster.storage.domain.RawRandomData
import io.peekandpoke.funktor.core.jsonPrinter
import io.peekandpoke.funktor.inspect.cluster.storage.RandomDataStorageApiClient
import io.peekandpoke.funktor.inspect.cluster.storage.RawRandomDataModel
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.QueryParams
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.model.Paged
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.map

class RandomDataStorageApi : ApiRoutes("random-data") {

    val list = RandomDataStorageApiClient.List.mount(QueryParams.List::class) {
        docs {
            name = "List random data"
        }.codeGen {
            funcName = "list"
        }.authorize {
            isSuperUser()
        }.handle { params ->

            val result = cluster.storage.randomData
                .list(search = params.search, page = params.page, epp = params.epp)

            ApiResponse.ok(
                Paged(
                    items = result.map { asApiModel(it).asHead },
                    page = params.page,
                    epp = params.epp,
                    fullItemCount = result.fullCount,
                )
            )
        }
    }

    val get = RandomDataStorageApiClient.Get.mount(QueryParams.GetById::class) {
        docs {
            name = "Get random data"
        }.codeGen {
            funcName = "get"
        }.authorize {
            isSuperUser()
        }.handle { params ->

            val result = cluster.storage.randomData.get(id = params.id)

            ApiResponse.okOrNotFound(
                result?.let { asApiModel(it) }
            )
        }
    }

    private fun RoutingContext.asApiModel(raw: Stored<RawRandomData>) = with(raw.value) {
        RawRandomDataModel(
            id = raw._key,
            category = category,
            dataId = dataId,
            data = jsonPrinter.prettyPrint(data),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
