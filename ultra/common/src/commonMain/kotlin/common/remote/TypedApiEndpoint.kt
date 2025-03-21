package de.peekandpoke.ultra.common.remote

import de.peekandpoke.ultra.common.TypedAttributes
import de.peekandpoke.ultra.common.TypedKey
import de.peekandpoke.ultra.common.model.EmptyObject
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer

fun <RESPONSE> ApiClient.call(bound: TypedApiEndpoint.Delete.Bound<RESPONSE>): Flow<RESPONSE> {
    return remote
        .delete(
            uri = buildUri(pattern = bound.endpoint.uri, params = bound.params)
        )
        .body { it decodedBy bound.endpoint.response }
}

fun <RESPONSE> ApiClient.call(bound: TypedApiEndpoint.Get.Bound<RESPONSE>): Flow<RESPONSE> {
    return remote
        .get(
            uri = buildUri(pattern = bound.uri, params = bound.params)
        )
        .body { it decodedBy bound.responseSerializer }
}

fun <RESPONSE> ApiClient.call(bound: TypedApiEndpoint.Head.Bound<RESPONSE>): Flow<RESPONSE> {
    return remote
        .head(
            uri = buildUri(pattern = bound.endpoint.uri, params = bound.params)
        )
        .body { it decodedBy bound.endpoint.response }
}

fun <BODY, RESPONSE> ApiClient.call(bound: TypedApiEndpoint.Post.Bound<BODY, RESPONSE>): Flow<RESPONSE> {
    @Suppress("UNCHECKED_CAST")
    return remote
        .post(
            uri = buildUri(pattern = bound.uri, params = bound.params),
            contentType = "application/json",
            body = bound.body encodedBy bound.bodySerializer as KSerializer<BODY>
        )
        .body { it decodedBy bound.responseSerializer }
}

fun <BODY, RESPONSE> ApiClient.call(bound: TypedApiEndpoint.Put.Bound<BODY, RESPONSE>): Flow<RESPONSE> {
    @Suppress("UNCHECKED_CAST")
    return remote
        .put(
            uri = buildUri(pattern = bound.uri, params = bound.params),
            contentType = "application/json",
            body = bound.body encodedBy bound.bodySerializer as KSerializer<BODY>
        )
        .body { it decodedBy bound.responseSerializer }
}

sealed class TypedApiEndpoint {

    abstract val uri: String
    abstract val attributes: TypedAttributes

    fun <T : Any> getAttribute(key: TypedKey<T>): T? = attributes[key]

    data class Delete<RESPONSE>(
        override val uri: String,
        val response: KSerializer<RESPONSE>,
        override val attributes: TypedAttributes = TypedAttributes.empty,
    ) : TypedApiEndpoint() {
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

    data class Get<out RESPONSE>(
        override val uri: String,
        val response: KSerializer<out RESPONSE>,
        override val attributes: TypedAttributes = TypedAttributes.empty,
    ) : TypedApiEndpoint() {
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

    data class Sse(
        override val uri: String,
        override val attributes: TypedAttributes = TypedAttributes.empty,
    ) : TypedApiEndpoint() {
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

    data class Head<RESPONSE>(
        override val uri: String,
        val response: KSerializer<RESPONSE>,
        override val attributes: TypedAttributes = TypedAttributes.empty,
    ) : TypedApiEndpoint() {
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

    data class Post<out BODY, out RESPONSE>(
        override val uri: String,
        val body: KSerializer<out BODY>,
        val response: KSerializer<out RESPONSE>,
        override val attributes: TypedAttributes = TypedAttributes.empty,
    ) : TypedApiEndpoint() {
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

    data class Put<out BODY, out RESPONSE>(
        override val uri: String,
        val body: KSerializer<out BODY>,
        val response: KSerializer<out RESPONSE>,
        override val attributes: TypedAttributes = TypedAttributes.empty,
    ) : TypedApiEndpoint() {
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
