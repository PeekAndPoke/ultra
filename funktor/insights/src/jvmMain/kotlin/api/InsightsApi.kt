package io.peekandpoke.funktor.insights.api

import io.ktor.server.plugins.NotFoundException
import io.ktor.server.routing.RoutingContext
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.funktor.core.model.AppInfo
import io.peekandpoke.funktor.insights.InsightsDataLoader
import io.peekandpoke.funktor.insights.InsightsGraphs
import io.peekandpoke.funktor.insights.Insights
import io.peekandpoke.funktor.insights.funktorInsights
import io.peekandpoke.funktor.insights.toInsightsDatabaseGraph
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.funktor.rest.noInsights
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.TypedApiEndpoint.Get
import io.peekandpoke.ultra.remote.api
import io.peekandpoke.ultra.remote.apiPaged
import io.peekandpoke.ultra.vault.tools.DatabaseGraphBuilder

/**
 * Recorded request insights, superuser only.
 *
 * The `authFloor` is the whole point of this class. The data includes request and response headers,
 * user records and application config, so there is no view of it that is safe for anyone else — which
 * is why the floor sits on the group rather than on individual routes. The two graphs sit under it for
 * the same reason: one names every class in the app, the other every repository and reference.
 *
 * The server info names the build and environment; under the floor because a git describe can name a branch or
 * ticket, and an id the deployment.
 *
 * Every route also answers 404 unless insights is on for the mount and the environment allows it
 * ([requireActiveInsights]).
 */
class InsightsApi : ApiRoutes("insights", authFloor = { isSuperUser() }) {

    companion object {
        const val base = "/_/funktor/insights"

        /** The most recent records, newest first, paged. */
        val ListRecords = Get(
            uri = "$base/records",
            response = InsightsRecordSummary.serializer().apiPaged(),
        )

        /** One full record, with its neighbours for prev/next navigation. */
        val GetRecord = Get(
            uri = "$base/records/{bucket}/{file}",
            response = InsightsRecord.serializer().api(),
        )

        /** The kontainer's service definition graph. Static per blueprint, so a client may cache it by fingerprint. */
        val GetKontainerGraph = Get(
            uri = "$base/graphs/kontainer",
            response = InsightsKontainerGraph.serializer().api(),
        )

        /** The database's repositories and references, built per request; `repos` is empty for an app without any. */
        val GetDatabaseGraph = Get(
            uri = "$base/graphs/database",
            response = InsightsDatabaseGraph.serializer().api(),
        )

        /** The running server's git version and environment, for the list page header. */
        val GetServerInfo = Get(
            uri = "$base/server",
            response = InsightsServerInfo.serializer().api(),
        )

        /** Upper bound on `epp`, so one request cannot walk the entire depot. */
        const val MAX_EPP = 200

        /**
         * Upper bound on `page`.
         *
         * Not cosmetic: the loader computes `(page - 1) * epp`, which used to overflow `Int` and wrap
         * NEGATIVE, so `?page=2147483647` answered with page 1 rather than an empty page. The skip is a
         * `Long` now; this keeps a nonsense page from being accepted at all.
         */
        const val MAX_PAGE = 1_000_000
    }

    val listRecords = ListRecords.mount(InsightsApiFeature.PagingParam::class) {
        docs {
            name = "List insights records"
        }.codeGen {
            funcName = "listRecords"
        }.noInsights().handle { params ->
            requireActiveInsights()

            ApiResponse.ok(
                call.kontainer.get(InsightsDataLoader::class).list(
                    page = params.page.coerceIn(1, MAX_PAGE),
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
            requireActiveInsights()

            ApiResponse.okOrNotFound(
                call.kontainer.get(InsightsDataLoader::class).load(
                    InsightsRecordRef(bucket = params.bucket, file = params.file)
                )
            )
        }
    }

    val getKontainerGraph = GetKontainerGraph.mount {
        docs {
            name = "Get the kontainer graph"
        }.codeGen {
            funcName = "getKontainerGraph"
        }.noInsights().handle {
            requireActiveInsights()

            ApiResponse.ok(call.kontainer.get(InsightsGraphs::class).kontainerGraph)
        }
    }

    val getDatabaseGraph = GetDatabaseGraph.mount {
        docs {
            name = "Get the database graph"
        }.codeGen {
            funcName = "getDatabaseGraph"
        }.noInsights().handle {
            requireActiveInsights()

            // Always registered: funktor core installs `ultraVault` (`funktor/core/.../core_module.kt`)
            ApiResponse.ok(call.kontainer.get(DatabaseGraphBuilder::class).getGraph().toInsightsDatabaseGraph())
        }
    }

    val getServerInfo = GetServerInfo.mount {
        docs {
            name = "Get the server's version and environment"
        }.codeGen {
            funcName = "getServerInfo"
        }.noInsights().handle {
            requireActiveInsights()

            ApiResponse.ok(
                InsightsServerInfo.of(
                    version = call.kontainer.get(AppInfo::class).version,
                    ktor = call.kontainer.get(AppConfig::class).ktor,
                )
            )
        }
    }

    /**
     * 404 unless this mount records insights ([Insights.isActive]: enabled, and an environment that allows it;
     * ULTRA-00225). The graphs need no record, so without this a superuser of an app that records nothing still got
     * every service definition and the repository graph. Checked after the floor, so an unauthorized caller gets the
     * floor's 401 as before.
     */
    private fun RoutingContext.requireActiveInsights() {
        if (call.funktorInsights?.isActive != true) throw NotFoundException()
    }
}
