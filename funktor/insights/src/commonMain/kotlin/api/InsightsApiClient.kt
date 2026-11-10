package io.peekandpoke.funktor.insights.api

import io.peekandpoke.ultra.model.Paged
import io.peekandpoke.ultra.remote.ApiClient
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.TypedApiEndpoint.Get
import io.peekandpoke.ultra.remote.api
import io.peekandpoke.ultra.remote.apiPaged
import io.peekandpoke.ultra.remote.call
import kotlinx.coroutines.flow.Flow

/**
 * The Kotlin client for recorded request insights, superuser only (ULTRA-00198).
 *
 * The endpoint declarations are the single source: the server's `InsightsApi` mounts these, and the generated
 * TypeScript client is read off those mounts, so the Kraft and Vue clients cannot drift apart.
 */
class InsightsApiClient(config: Config) : ApiClient(config) {

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
    }

    /** One page of records; the server clamps [epp] to `InsightsApi.MAX_EPP`. */
    fun listRecords(page: Int, epp: Int): Flow<ApiResponse<Paged<InsightsRecordSummary>>> = call(
        ListRecords(
            "page" to page.toString(),
            "epp" to epp.toString(),
        )
    )

    /** One record; `data` is null when it no longer exists. */
    fun getRecord(ref: InsightsRecordRef): Flow<ApiResponse<InsightsRecord>> = call(
        GetRecord(
            "bucket" to ref.bucket,
            "file" to ref.file,
        )
    )

    fun getKontainerGraph(): Flow<ApiResponse<InsightsKontainerGraph>> = call(
        GetKontainerGraph()
    )

    fun getDatabaseGraph(): Flow<ApiResponse<InsightsDatabaseGraph>> = call(
        GetDatabaseGraph()
    )

    fun getServerInfo(): Flow<ApiResponse<InsightsServerInfo>> = call(
        GetServerInfo()
    )
}
