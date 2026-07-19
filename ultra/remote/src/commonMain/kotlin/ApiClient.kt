package io.peekandpoke.ultra.remote

import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Base class for type-safe API clients.
 *
 * Subclasses define domain-specific endpoint methods that delegate to the
 * operator overloads provided here for [ApiEndpoint.Get], [ApiEndpoint.Post],
 * [ApiEndpoint.Put], and [ApiEndpoint.Delete].
 *
 * Serialization / deserialization is driven by the [Json] codec in [Config].
 */
@Suppress("Detekt.TooManyFunctions")
abstract class ApiClient(val config: Config) {

    /**
     * Configuration for the api client.
     *
     * The [client] is the Ktor client used for all requests. Configure request headers / bearer
     * tokens on it via `defaultRequest { }`. Non-2xx responses are surfaced as a decoded
     * [ApiResponse] envelope, not thrown — the transport forces `expectSuccess = false` per request,
     * so a client-level `expectSuccess = true` / `HttpResponseValidator` cannot silently reintroduce
     * throw-on-error.
     *
     * [onResponse] observers are invoked (via `onEach`) for every [RemoteResponse] — used e.g. by
     * the dev-tools request list. They must not throw.
     */
    data class Config(
        val baseUrl: String,
        val codec: Json,
        val client: HttpClient,
        val onResponse: List<suspend (RemoteResponse) -> Unit> = emptyList(),
    )

    /** Gets a net instance of a [RemoteRequest] */
    val remote get(): RemoteRequest = createRequest(config)

    //  CALLING ENDPOINTS  /////////////////////////////////////////////////////////////////////////////////////////////

    //  GET  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    /** Invokes a GET endpoint with vararg [params] and a [decode] function. */
    operator fun <RESPONSE> ApiEndpoint.Get.invoke(
        vararg params: Pair<String, String?>,
        decode: Json.(String) -> RESPONSE,
    ): Flow<RESPONSE> =
        invoke(params = params.toMap(), decode = decode)

    /** Invokes a GET endpoint with a [params] map and a [decode] function. */
    operator fun <RESPONSE> ApiEndpoint.Get.invoke(
        params: Map<String, String?>,
        decode: Json.(String) -> RESPONSE,
    ): Flow<RESPONSE> =
        remote
            .get(uri = buildUri(uri, params))
            .body { config.codec.decode(it) }

    //  POST  //////////////////////////////////////////////////////////////////////////////////////////////////////////

    /** Invokes a POST endpoint with vararg [params], an optional [body], and a [decode] function. */
    operator fun <RESPONSE> ApiEndpoint.Post.invoke(
        vararg params: Pair<String, String?>,
        body: String? = null,
        decode: Json.(String) -> RESPONSE,
    ): Flow<RESPONSE> =
        invoke(params = params.toMap(), body = body, decode = decode)

    /** Invokes a POST endpoint with a [params] map, an optional [body], and a [decode] function. */
    operator fun <RESPONSE> ApiEndpoint.Post.invoke(
        params: Map<String, String?>,
        body: String?,
        decode: Json.(String) -> RESPONSE,
    ): Flow<RESPONSE> =
        remote
            .post(uri = buildUri(uri, params), body = body ?: "{}")
            .body { config.codec.decode(it) }

    //  PUT  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    /** Invokes a PUT endpoint with vararg [params], an optional [body], and a [decode] function. */
    operator fun <RESPONSE> ApiEndpoint.Put.invoke(
        vararg params: Pair<String, String?>,
        body: String? = null,
        decode: Json.(String) -> RESPONSE,
    ): Flow<RESPONSE> =
        invoke(params = params.toMap(), body = body, decode = decode)

    /** Invokes a PUT endpoint with a [params] map, an optional [body], and a [decode] function. */
    operator fun <RESPONSE> ApiEndpoint.Put.invoke(
        params: Map<String, String?>,
        body: String?,
        decode: Json.(String) -> RESPONSE,
    ): Flow<RESPONSE> =
        remote
            .put(uri = buildUri(uri, params), body = body ?: "{}")
            .body { config.codec.decode(it) }

    //  DELETE  ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /** Invokes a DELETE endpoint with vararg [params], an optional [body], and a [decode] function. */
    operator fun <RESPONSE> ApiEndpoint.Delete.invoke(
        vararg params: Pair<String, String?>,
        body: String? = null,
        decode: Json.(String) -> RESPONSE,
    ): Flow<RESPONSE> =
        invoke(params = params.toMap(), body = body, decode = decode)

    /** Invokes a DELETE endpoint with a [params] map, an optional [body], and a [decode] function. */
    operator fun <RESPONSE> ApiEndpoint.Delete.invoke(
        params: Map<String, String?>,
        body: String?,
        decode: Json.(String) -> RESPONSE,
    ): Flow<RESPONSE> =
        remote
            .delete(uri = buildUri(uri, params), body = body ?: "{}")
            .body { config.codec.decode(it) }

    //  HELPERS  ///////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Helper for encoding an object
     */
    infix fun <T> T.encodedBy(serializer: KSerializer<T>): String {
        return serializer.encode(this)
    }

    /**
     * Helper for encoding an object
     */
    fun <T> KSerializer<T>.encode(body: T): String {
        return config.codec.encodeToString(this, body)
    }

    /**
     * Helper for decoding a string into an object
     */
    infix fun <T> String.decodedBy(serializer: KSerializer<T>): T {
        return serializer.decode(this)
    }

    /**
     * Helper for decoding a string into an object
     */
    fun <T> KSerializer<T>.decode(body: String): T {
        return config.codec.decodeFromString(this, body)
    }

    /**
     * extracts the body from the given [RemoteResponse]
     */
    fun Flow<RemoteResponse>.body(): Flow<String> = this.map { it.body }

    /**
     * Helper function for receiving the response body and mapping it
     */
    inline fun <R> Flow<RemoteResponse>.body(crossinline transform: suspend (String) -> R): Flow<R> {
        return body().map { transform(it) }
    }
}
