package de.peekandpoke.ktorfx.cluster.storage.api

import de.peekandpoke.ktorfx.cluster.cluster
import de.peekandpoke.ktorfx.cluster.storage.RandomDataStorageApiClient
import de.peekandpoke.ktorfx.cluster.storage.RawRandomDataModel
import de.peekandpoke.ktorfx.cluster.storage.domain.RawRandomData
import de.peekandpoke.ktorfx.core.broker.OutgoingConverter
import de.peekandpoke.ktorfx.core.jsonPrinter
import de.peekandpoke.ktorfx.rest.ApiRoutes
import de.peekandpoke.ktorfx.rest.QueryParams
import de.peekandpoke.ktorfx.rest.docs.codeGen
import de.peekandpoke.ktorfx.rest.docs.docs
import de.peekandpoke.ultra.common.model.Paged
import de.peekandpoke.ultra.common.remote.ApiResponse
import de.peekandpoke.ultra.vault.Stored
import io.ktor.server.routing.*

class RandomDataStorageApi(converter: OutgoingConverter) : ApiRoutes("random-data", converter) {

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
