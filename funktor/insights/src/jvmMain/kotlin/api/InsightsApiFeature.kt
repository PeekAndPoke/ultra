package io.peekandpoke.funktor.insights.api

import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiRoutes

/**
 * The only way to reach insights data.
 *
 * Its endpoints are declared on [InsightsApiClient] in commonMain, in the pattern of the other funktor API
 * modules (maintainer, 2026-10-19): Kraft frontends call that client, and the generated TypeScript client is
 * read off the same declarations through [InsightsApi]'s mounts, so the two cannot drift apart.
 */
class InsightsApiFeature : ApiFeature {

    /** Depot paths are `records-<date>/<file>.json`, so the record id arrives as two segments. */
    data class RecordParam(
        val bucket: String,
        val file: String,
    )

    /**
     * Paging for the record list, matching `BackgroundJobsApi.PagingParam`.
     *
     * Declared as a PARAMS class, not read off `queryParameters`: codegen derives query parameters
     * exclusively from the route's params type, so a value read off the raw request is invisible to the
     * generated TypeScript client, which the Vue GUI uses (the Kraft GUI uses `InsightsApiClient`).
     */
    data class PagingParam(
        val page: Int = 1,
        val epp: Int = 20,
    )

    override val name = "Funktor Insights"

    override val description = """
        Superuser-only access to recorded request insights.
    """.trimIndent()

    val insights = InsightsApi()

    override fun getRouteGroups(): List<ApiRoutes> = listOf(
        insights,
    )
}
