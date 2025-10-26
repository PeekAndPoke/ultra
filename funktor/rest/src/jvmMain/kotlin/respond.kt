package de.peekandpoke.funktor.rest

import de.peekandpoke.funktor.core.appConfig
import de.peekandpoke.funktor.core.kontainerOrNull
import de.peekandpoke.funktor.core.metrics.RequestMetricsProvider
import de.peekandpoke.funktor.rest.auth.AuthRule
import de.peekandpoke.ultra.common.encodeUriComponent
import de.peekandpoke.ultra.common.network.NetworkUtils
import de.peekandpoke.ultra.common.remote.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import java.time.Instant

/**
 * The name of the server
 */
private val server = NetworkUtils.getHostNameOrDefault()

/**
 * Sends an [ApiResponse]
 */
suspend fun <T : Any?> ApplicationCall.apiRespond(response: T) {

    when (response) {
        is ApiResponse<*> -> {
            // Base content
            val enriched = enrichApiResponseWithInsights(response)

            val serialized = restCodec.serialize(enriched) ?: ""

            respondBytes(
                contentType = ContentType.Application.Json.withCharset(Charsets.UTF_8),
                status = HttpStatusCode.fromValue(response.status.value),
                bytes = serialized.encodeToByteArray(),
            )
        }

        else -> respond(response ?: Unit)
    }
}

/**
 * Private helper for creating a not found ApiResponse
 */
suspend fun <T : Any?> ApplicationCall.apiRespondUnauthorized(
    method: HttpMethod,
    uri: String,
    failedRules: List<AuthRule<*, *>>,
) {

    val response = ApiResponse.unauthorized<T>()
        .withError("Access to the resource '${method.value} $uri' is not authorized.")
        .let {
            when (appConfig.ktor.isNotProduction) {
                true -> {
                    it.withInfo(
                        "Failed auth rules: " + failedRules.joinToString(" AND ") { failed -> failed.description }
                    )
                }

                else -> it
            }
        }

    apiRespond(response)
}

fun <T : Any?> ApplicationCall.enrichApiResponseWithInsights(response: ApiResponse<T>): ApiResponse<T> {

    val metrics = kontainerOrNull?.getOrNull(RequestMetricsProvider::class)

    return response.withInsights(
        ApiResponse.Insights(
            ts = Instant.now().epochSecond,
            method = request.httpMethod.value,
            url = "${request.origin.scheme}://${request.origin.serverHost}:${request.origin.serverPort}${request.origin.uri}",
            server = server,
            status = response.status,
            durationMs = metrics?.getRequestDurationInMs(),
            detailsUri = metrics?.getRequestDetailsUri()?.encodeUriComponent(),
            detailsUrl = metrics?.getRequestDetailsUrl(),
        )
    )
}
