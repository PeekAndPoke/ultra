package io.peekandpoke.ultra.remote

import io.ktor.client.plugins.sse.*
import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.model.EmptyObject
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer

/**
 * Executes a bound GET endpoint, returning a [Flow] of the deserialized response.
 */
fun <RESPONSE> ApiClient.call(bound: TypedApiEndpoint.Get.Bound<RESPONSE>): Flow<RESPONSE> {
    return remote.get(
        uri = buildUri(pattern = bound.uri, params = bound.params)
    ).body { it decodedBy bound.responseSerializer }
}

/**
 * Executes a bound HEAD endpoint, returning a [Flow] of the deserialized response.
 */
fun <RESPONSE> ApiClient.call(bound: TypedApiEndpoint.Head.Bound<RESPONSE>): Flow<RESPONSE> {
    return remote.head(
        uri = buildUri(pattern = bound.endpoint.uri, params = bound.params)
    ).body { it decodedBy bound.endpoint.response }
}

/**
 * Opens a Server-Sent Events session for a bound SSE endpoint.
 */
suspend fun ApiClient.call(bound: TypedApiEndpoint.Sse.Bound): ClientSSESession {
    return remote.sse(
        uri = buildUri(pattern = bound.uri, params = bound.params)
    )
}

/**
 * Executes a bound POST endpoint, serializing the body and returning a [Flow] of the deserialized response.
 */
fun <BODY, RESPONSE> ApiClient.call(bound: TypedApiEndpoint.Post.Bound<BODY, RESPONSE>): Flow<RESPONSE> {
    @Suppress("UNCHECKED_CAST")
    return remote.post(
        uri = buildUri(pattern = bound.uri, params = bound.params),
        contentType = "application/json",
        body = bound.body encodedBy bound.bodySerializer as KSerializer<BODY>
    ).body { it decodedBy bound.responseSerializer }
}

/**
 * Executes a bound PUT endpoint, serializing the body and returning a [Flow] of the deserialized response.
 */
fun <BODY, RESPONSE> ApiClient.call(bound: TypedApiEndpoint.Put.Bound<BODY, RESPONSE>): Flow<RESPONSE> {
    @Suppress("UNCHECKED_CAST")
    return remote.put(
        uri = buildUri(pattern = bound.uri, params = bound.params),
        contentType = "application/json",
        body = bound.body encodedBy bound.bodySerializer as KSerializer<BODY>
    ).body { it decodedBy bound.responseSerializer }
}

/**
 * Executes a bound DELETE endpoint, returning a [Flow] of the deserialized response.
 */
fun <RESPONSE> ApiClient.call(bound: TypedApiEndpoint.Delete.Bound<RESPONSE>): Flow<RESPONSE> {
    return remote.delete(
        uri = buildUri(pattern = bound.endpoint.uri, params = bound.params)
    ).body { it decodedBy bound.endpoint.response }
}

/**
 * Sealed hierarchy of type-safe API endpoint definitions.
 *
 * Unlike [ApiEndpoint], each subclass carries [KSerializer] references for the
 * request body and/or response type so that serialization can be performed
 * automatically by [ApiClient.call].
 *
 * Every endpoint also holds [TypedAttributes] for attaching arbitrary metadata
 * (e.g., required permissions, feature flags).
 */
sealed class TypedApiEndpoint {

    /** The URI pattern for the endpoint, may contain `{placeholder}` segments. */
    abstract val uri: String

    /** The HTTP method as an uppercase string (e.g., "GET", "POST"). */
    abstract val httpMethod: String

    /** Arbitrary typed attributes attached to this endpoint definition. */
    abstract val attributes: TypedAttributes

    /** Retrieves a typed attribute by its [key], or `null` if not present. */
    fun <T : Any> getAttribute(key: TypedKey<T>): T? = attributes[key]

    /** A typed HTTP DELETE endpoint with a deserializer for [RESPONSE]. */
    data class Delete<RESPONSE>(
        override val uri: String,
        val response: KSerializer<RESPONSE>,
        override val attributes: TypedAttributes = TypedAttributes.empty,
    ) : TypedApiEndpoint() {
        override val httpMethod: String get() = "DELETE"
        companion object {
            operator fun invoke(uri: String) = Delete(uri = uri, response = EmptyObject.serializer())
        }

        class Bound<RESPONSE>(
            val endpoint: Delete<RESPONSE>,
            val params: Map<String, String?>,
        )

        fun bind(params: Map<String, String?> = emptyMap()) =
            Bound(endpoint = this, params = params)

        operator fun invoke(params: Map<String, String?> = emptyMap()) =
            bind(params = params)

        operator fun invoke(vararg params: Pair<String, String>) =
            bind(params = params.toMap())

        fun withAttributes(builder: TypedAttributes.Builder.() -> Unit): Delete<RESPONSE> = copy(
            attributes = attributes.plus(builder)
        )
    }

    /** A typed HTTP GET endpoint with a deserializer for [RESPONSE]. */
    data class Get<out RESPONSE>(
        override val uri: String,
        val response: KSerializer<out RESPONSE>,
        override val attributes: TypedAttributes = TypedAttributes.empty,
    ) : TypedApiEndpoint() {
        override val httpMethod: String get() = "GET"
        class Bound<out RESPONSE>(
            val uri: String,
            val params: Map<String, String?>,
            val responseSerializer: KSerializer<out RESPONSE>,
        )

        private fun bind(
            params: Map<String, String?>,
            responseSerializer: KSerializer<out RESPONSE>,
        ): Bound<RESPONSE> = Bound(
            uri = uri,
            params = params,
            responseSerializer = responseSerializer,
        )

        operator fun invoke(
            params: Map<String, String?> = emptyMap(),
            responseSerializer: KSerializer<out @UnsafeVariance RESPONSE> = this.response,
        ): Bound<RESPONSE> =
            bind(params = params, responseSerializer = responseSerializer)

        operator fun invoke(
            vararg params: Pair<String, String?>,
            responseSerializer: KSerializer<out @UnsafeVariance RESPONSE> = this.response,
        ): Bound<RESPONSE> =
            invoke(params = params.toMap(), responseSerializer = responseSerializer)

        fun withAttributes(builder: TypedAttributes.Builder.() -> Unit): Get<RESPONSE> = copy(
            attributes = attributes.plus(builder)
        )
    }

    /** A typed Server-Sent Events endpoint. */
    data class Sse(
        override val uri: String,
        override val attributes: TypedAttributes = TypedAttributes.empty,
    ) : TypedApiEndpoint() {
        override val httpMethod: String get() = "GET"
        class Bound(
            val uri: String,
            val params: Map<String, String?>,
        )

        private fun bind(
            params: Map<String, String?>,
        ): Bound = Bound(
            uri = uri,
            params = params,
        )

        operator fun invoke(
            params: Map<String, String?> = emptyMap(),
        ): Bound =
            bind(params = params)

        operator fun invoke(
            vararg params: Pair<String, String?>,
        ): Bound =
            invoke(params = params.toMap())

        fun withAttributes(builder: TypedAttributes.Builder.() -> Unit): Sse = copy(
            attributes = attributes.plus(builder)
        )
    }

    /** A typed HTTP HEAD endpoint with a deserializer for [RESPONSE]. */
    data class Head<RESPONSE>(
        override val uri: String,
        val response: KSerializer<RESPONSE>,
        override val attributes: TypedAttributes = TypedAttributes.empty,
    ) : TypedApiEndpoint() {
        override val httpMethod: String get() = "HEAD"
        companion object {
            operator fun invoke(uri: String) = Head(uri = uri, response = EmptyObject.serializer())
        }

        class Bound<RESPONSE>(
            val endpoint: Head<RESPONSE>,
            val params: Map<String, String?>,
        )

        fun bind(params: Map<String, String?> = emptyMap()) =
            Bound(endpoint = this, params = params)

        operator fun invoke(params: Map<String, String?> = emptyMap()) =
            bind(params = params)

        operator fun invoke(vararg params: Pair<String, String>) =
            bind(params = params.toMap())

        fun withAttributes(builder: TypedAttributes.Builder.() -> Unit): Head<RESPONSE> = copy(
            attributes = attributes.plus(builder)
        )
    }

    /** A typed HTTP POST endpoint with serializers for [BODY] and [RESPONSE]. */
    data class Post<out BODY, out RESPONSE>(
        override val uri: String,
        val body: KSerializer<out BODY>,
        val response: KSerializer<out RESPONSE>,
        override val attributes: TypedAttributes = TypedAttributes.empty,
    ) : TypedApiEndpoint() {
        override val httpMethod: String get() = "POST"
        class Bound<out BODY, out RESPONSE>(
            val uri: String,
            val params: Map<String, String?>,
            val body: BODY,
            val bodySerializer: KSerializer<out BODY>,
            val responseSerializer: KSerializer<out RESPONSE>,
        )

        private fun bind(
            params: Map<String, String?>,
            body: BODY,
            bodySerializer: KSerializer<out BODY>,
            responseSerializer: KSerializer<out RESPONSE>,
        ): Bound<BODY, RESPONSE> = Bound(
            uri = uri,
            params = params,
            body = body,
            bodySerializer = bodySerializer,
            responseSerializer = responseSerializer,
        )

        operator fun invoke(
            params: Map<String, String?> = emptyMap(),
            body: @UnsafeVariance BODY,
            bodySerializer: KSerializer<out @UnsafeVariance BODY> = this.body,
            responseSerializer: KSerializer<out @UnsafeVariance RESPONSE> = this.response,
        ): Bound<BODY, RESPONSE> = bind(
            params = params,
            body = body,
            bodySerializer = bodySerializer,
            responseSerializer = responseSerializer,
        )

        operator fun invoke(
            vararg params: Pair<String, String>,
            body: @UnsafeVariance BODY,
            bodySerializer: KSerializer<out @UnsafeVariance BODY> = this.body,
            responseSerializer: KSerializer<out @UnsafeVariance RESPONSE> = this.response,
        ) = invoke(
            params = params.toMap(),
            body = body,
            bodySerializer = bodySerializer,
            responseSerializer = responseSerializer,
        )

        fun withAttributes(builder: TypedAttributes.Builder.() -> Unit): Post<BODY, RESPONSE> = copy(
            attributes = attributes.plus(builder)
        )
    }

    /** A typed HTTP PUT endpoint with serializers for [BODY] and [RESPONSE]. */
    data class Put<out BODY, out RESPONSE>(
        override val uri: String,
        val body: KSerializer<out BODY>,
        val response: KSerializer<out RESPONSE>,
        override val attributes: TypedAttributes = TypedAttributes.empty,
    ) : TypedApiEndpoint() {
        override val httpMethod: String get() = "PUT"
        class Bound<out BODY, out RESPONSE>(
            val uri: String,
            val params: Map<String, String?>,
            val body: BODY,
            val bodySerializer: KSerializer<out BODY>,
            val responseSerializer: KSerializer<out RESPONSE>,
        )

        private fun bind(
            params: Map<String, String?>,
            body: BODY,
            bodySerializer: KSerializer<out BODY>,
            responseSerializer: KSerializer<out RESPONSE>,
        ): Bound<BODY, RESPONSE> {
            return Bound(
                uri = uri,
                params = params,
                body = body,
                bodySerializer = bodySerializer,
                responseSerializer = responseSerializer,
            )
        }

        operator fun invoke(
            params: Map<String, String?> = emptyMap(),
            body: @UnsafeVariance BODY,
            bodySerializer: KSerializer<out @UnsafeVariance BODY> = this.body,
            responseSerializer: KSerializer<out @UnsafeVariance RESPONSE> = this.response,
        ) = bind(
            params = params,
            body = body,
            bodySerializer = bodySerializer,
            responseSerializer = responseSerializer,
        )

        operator fun invoke(
            vararg params: Pair<String, String>,
            body: @UnsafeVariance BODY,
            bodySerializer: KSerializer<out @UnsafeVariance BODY> = this.body,
            responseSerializer: KSerializer<out @UnsafeVariance RESPONSE> = this.response,
        ) = bind(
            params = params.toMap(),
            body = body,
            bodySerializer = bodySerializer,
            responseSerializer = responseSerializer,
        )

        fun withAttributes(builder: TypedAttributes.Builder.() -> Unit): Put<BODY, RESPONSE> = copy(
            attributes = attributes.plus(builder)
        )
    }
}
