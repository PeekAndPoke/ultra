package io.peekandpoke.ultra.remote

import io.ktor.client.plugins.sse.ClientSSESession
import kotlinx.coroutines.flow.Flow

/**
 * Issues HTTP requests against a base URL and returns a [Flow] of [RemoteResponse].
 *
 * Backed by a single Ktor `HttpClient`. Non-2xx responses are **emitted** (not thrown) so callers
 * can inspect the decoded `ApiResponse` envelope; only genuine transport failures propagate as
 * exceptions through the flow.
 *
 * Cross-cutting concerns are configured on the Ktor client itself:
 * - request headers / bearer tokens via `defaultRequest { }`
 * - response observation (e.g. dev-tools) via [ApiClient.Config.onResponse], applied with `onEach`.
 */
interface RemoteRequest {

    /** issues a GET request returning a flow of its response */
    fun get(uri: String = ""): Flow<RemoteResponse>

    /** issues a HEAD request returning a flow of its response */
    fun head(uri: String = ""): Flow<RemoteResponse>

    /** starts an SSE session */
    suspend fun sse(uri: String = ""): ClientSSESession

    /** issues a POST request returning a flow of its response */
    fun post(uri: String = "", contentType: String = "application/json", body: String): Flow<RemoteResponse>

    /** issues a PUT request returning a flow of its response */
    fun put(uri: String = "", contentType: String = "application/json", body: String): Flow<RemoteResponse>

    /** issues a DELETE request returning a flow of its response */
    fun delete(uri: String = "", contentType: String = "application/json", body: String? = null): Flow<RemoteResponse>

    /** issues an OPTIONS request returning a flow of its response */
    fun options(uri: String = ""): Flow<RemoteResponse>

    /** issues a PATCH request returning a flow of its response */
    fun patch(uri: String = "", contentType: String = "application/json", body: String): Flow<RemoteResponse>
}
