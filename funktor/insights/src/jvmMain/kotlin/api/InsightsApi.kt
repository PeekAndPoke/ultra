package io.peekandpoke.funktor.insights.api

import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.funktor.insights.InsightsDataLoader
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.funktor.rest.noInsights
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.TypedApiEndpoint.Get
import io.peekandpoke.ultra.remote.api
import io.peekandpoke.ultra.remote.apiList

/**
 * Recorded request insights, superuser only.
 *
 * The `authFloor` is the whole point of this class. The data includes request and response headers,
 * user records and application config, so there is no view of it that is safe for anyone else — which
 * is why the floor sits on the group rather than on individual routes.
 */
class InsightsApi : ApiRoutes("insights", authFloor = { isSuperUser() }) {

    companion object {
        const val base = "/_/funktor/insights"

        /** The most recent records, newest first, paged. */
        val ListRecords = Get(
            uri = "$base/records",
            response = InsightsRecordSummary.serializer().apiList(),
        )

        /** One full record, with its neighbours for prev/next navigation. */
        val GetRecord = Get(
            uri = "$base/records/{bucket}/{file}",
            response = InsightsRecord.serializer().api(),
        )

        /** Upper bound on `epp`, so one request cannot walk the entire depot. */
        const val MAX_EPP = 200
    }

    val listRecords = ListRecords.mount(InsightsApiFeature.PagingParam::class) {
        docs {
            name = "List insights records"
        }.codeGen {
            funcName = "listRecords"
        }.noInsights().handle { params ->
            ApiResponse.ok(
                call.kontainer.get(InsightsDataLoader::class).list(
                    page = params.page.coerceAtLeast(1),
                    epp = params.epp.coerceIn(1, MAX_EPP),
                )
            )
        }
    }

    val getRecord = GetRecord.mount(InsightsApiFeature.RecordParam::class) {
        docs {
            name = "Get one insights record"
        }.codeGen {
            funcName = "getRecord"
        }.noInsights().handle { params ->
            ApiResponse.okOrNotFound(
                call.kontainer.get(InsightsDataLoader::class).load(
                    InsightsRecordRef(bucket = params.bucket, file = params.file)
                )
            )
        }
    }
}
