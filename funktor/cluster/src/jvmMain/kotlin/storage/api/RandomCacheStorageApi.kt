package io.peekandpoke.funktor.cluster.storage.api

import io.ktor.server.routing.*
import io.peekandpoke.funktor.cluster.cluster
import io.peekandpoke.funktor.cluster.storage.domain.RawCacheData
import io.peekandpoke.funktor.core.broker.OutgoingConverter
import io.peekandpoke.funktor.core.jsonPrinter
import io.peekandpoke.funktor.inspect.cluster.storage.RandomCacheStorageApiClient
import io.peekandpoke.funktor.inspect.cluster.storage.RawCacheDataModel
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.QueryParams
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.model.Paged
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.vault.Stored

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
