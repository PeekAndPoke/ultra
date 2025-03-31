package de.peekandpoke.funktor.cluster.storage.api

import de.peekandpoke.funktor.cluster.cluster
import de.peekandpoke.funktor.cluster.storage.RandomCacheStorageApiClient
import de.peekandpoke.funktor.cluster.storage.RawCacheDataModel
import de.peekandpoke.funktor.cluster.storage.domain.RawCacheData
import de.peekandpoke.funktor.core.broker.OutgoingConverter
import de.peekandpoke.funktor.core.jsonPrinter
import de.peekandpoke.funktor.rest.ApiRoutes
import de.peekandpoke.funktor.rest.QueryParams
import de.peekandpoke.funktor.rest.docs.codeGen
import de.peekandpoke.funktor.rest.docs.docs
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.model.Paged
import de.peekandpoke.ultra.common.remote.ApiResponse
import de.peekandpoke.ultra.vault.Stored
import io.ktor.server.routing.*

class RandomCacheStorageApi(converter: OutgoingConverter) : ApiRoutes("random-cache", converter) {

    val list = RandomCacheStorageApiClient.List.mount(QueryParams.List::class) {
        docs {
            name = "List cache data"
        }.codeGen {
            funcName = "list"
        }.authorize {
            isSuperUser()
        }.handle { params ->
            val result = cluster.storage.randomCache
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

    val get = RandomCacheStorageApiClient.Get.mount(QueryParams.GetById::class) {
        docs {
            name = "Get cache data"
        }.codeGen {
            funcName = "get"
        }.authorize {
            isSuperUser()
        }.handle { params ->

            val result = cluster.storage.randomCache.get(id = params.id)

            ApiResponse.okOrNotFound(
                result?.let { asApiModel(it) }
            )
        }
    }

    private fun RoutingContext.asApiModel(raw: Stored<RawCacheData>) = with(raw.value) {
        RawCacheDataModel(
            id = raw._key,
            category = category,
            dataId = dataId,
            data = jsonPrinter.prettyPrint(data),
            policy = policy,
            expiresAt = MpInstant.fromEpochSeconds(expiresAt),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
