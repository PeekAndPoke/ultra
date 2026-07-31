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

        /**
         * The most recent records, newest first.
         *
         * `limit` rather than a cursor for now — building a summary means opening each record. See
         * blocker B1 in `.claude/tasks/20260730-insights-rest-api.md`.
         */
        val ListRecords = Get(
            uri = "$base/records",
            response = InsightsRecordSummary.serializer().apiList(),
        )

        /** One full record, with its neighbours for prev/next navigation. */
        val GetRecord = Get(
            uri = "$base/records/{bucket}/{file}",
            response = InsightsRecord.serializer().api(),
        )

        /** How many records the list returns when the caller does not say. */
        const val DEFAULT_LIMIT = 50

        /** Upper bound on `limit`, so one request cannot walk the entire depot. */
        const val MAX_LIMIT = 500
    }

    val listRecords = ListRecords.mount {
        docs {
            name = "List insights records"
        }.codeGen {
            funcName = "listRecords"
        }.noInsights().handle {
            val limit = call.request.queryParameters["limit"]
                ?.toIntOrNull()
                ?.coerceIn(1, MAX_LIMIT)
                ?: DEFAULT_LIMIT

            ApiResponse.ok(
                call.kontainer.get(InsightsDataLoader::class).list(limit = limit)
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
                call.kontainer.get(InsightsDataLoader::class)
                    .load(path = "${params.bucket}/${params.file}")
            )
        }
    }
}
