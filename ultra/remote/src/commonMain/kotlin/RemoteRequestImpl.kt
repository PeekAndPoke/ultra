package io.peekandpoke.ultra.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.sse.ClientSSESession
import io.ktor.client.plugins.sse.sseSession
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

/**
 * Single, platform-agnostic [RemoteResponse] backed by a Ktor [HttpResponse].
 */
class RemoteResponseImpl(
    override val body: String,
    override val status: Int,
    override val statusText: String,
) : RemoteResponse

/**
 * Single, platform-agnostic [RemoteRequest] backed by a Ktor [HttpClient].
 *
 * The [client] must be created with `expectSuccess = false` (the Ktor default) so that non-2xx
 * responses are returned instead of thrown — genuine transport failures still surface as
 * exceptions through the flow.
 */
class RemoteRequestImpl(
    private val baseUrl: String,
    private val client: HttpClient,
    private val onResponse: List<suspend (RemoteResponse) -> Unit> = emptyList(),
) : RemoteRequest {

    private fun execute(
        method: HttpMethod,
        uri: String,
        contentType: String? = null,
        body: String? = null,
    ): Flow<RemoteResponse> = flow {
        val url = "$baseUrl/${uri.trimStart('/')}"

        val response: HttpResponse = client.request(url) {
            this.method = method
            // Force emit-on-non-2xx regardless of the caller's client config: callers read the
            // decoded ApiResponse envelope on 4xx/5xx, so a non-2xx must never throw here (only
            // genuine transport failures should surface through the flow).
            expectSuccess = false
            contentType?.let { contentType(ContentType.parse(it)) }
            body?.let { setBody(it) }
        }

        emit(
            RemoteResponseImpl(
                body = response.bodyAsText(),
                status = response.status.value,
                statusText = response.status.description,
            )
        )
    }.onEach { response ->
        // Observers are side-effect-only and must never break request delivery.
        onResponse.forEach { observer ->
            try {
                observer(response)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Swallow — a misbehaving observer must not turn a delivered response into an error.
            }
        }
    }

    override fun get(uri: String): Flow<RemoteResponse> = execute(HttpMethod.Get, uri)

    override fun head(uri: String): Flow<RemoteResponse> = execute(HttpMethod.Head, uri)

    override suspend fun sse(uri: String): ClientSSESession =
        client.sseSession("$baseUrl/${uri.trimStart('/')}")

    override fun post(uri: String, contentType: String, body: String): Flow<RemoteResponse> =
        execute(HttpMethod.Post, uri, contentType, body)

    override fun put(uri: String, contentType: String, body: String): Flow<RemoteResponse> =
        execute(HttpMethod.Put, uri, contentType, body)

    override fun delete(uri: String, contentType: String, body: String?): Flow<RemoteResponse> =
        execute(HttpMethod.Delete, uri, contentType, body)

    override fun options(uri: String): Flow<RemoteResponse> = execute(HttpMethod.Options, uri)

    override fun patch(uri: String, contentType: String, body: String): Flow<RemoteResponse> =
        execute(HttpMethod.Patch, uri, contentType, body)
}
