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
            uri = buildUri(pattern = bound.endpoint.uri, params = bound.params)
        )
        .body { it decodedBy bound.endpoint.response }
}

fun <RESPONSE> ApiClient.call(bound: TypedApiEndpoint.Head.Bound<RESPONSE>): Flow<RESPONSE> {
    return remote
        .head(
            uri = buildUri(pattern = bound.endpoint.uri, params = bound.params)
        )
        .body { it decodedBy bound.endpoint.response }
}

fun <BODY, RESPONSE> ApiClient.call(bound: TypedApiEndpoint.Post.Bound<BODY, RESPONSE>): Flow<RESPONSE> {
    return remote
        .post(
            uri = buildUri(pattern = bound.endpoint.uri, params = bound.params),
            contentType = "application/json",
            body = bound.body encodedBy bound.endpoint.body
        )
        .body { it decodedBy bound.endpoint.response }
}

fun <BODY, RESPONSE> ApiClient.call(bound: TypedApiEndpoint.Put.Bound<BODY, RESPONSE>): Flow<RESPONSE> {
    return remote
        .put(
            uri = buildUri(pattern = bound.uri, params = bound.params),
            contentType = "application/json",
            body = bound.body encodedBy bound.bodySerializer
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

    data class Get<RESPONSE>(
        override val uri: String,
        val response: KSerializer<RESPONSE>,
        override val attributes: TypedAttributes = TypedAttributes.empty,
    ) : TypedApiEndpoint() {
        class Bound<RESPONSE>(
            val endpoint: Get<RESPONSE>,
            val params: Map<String, String?>,
        )

        fun bind(params: Map<String, String?> = emptyMap()) =
            Bound(endpoint = this, params = params)

        operator fun invoke(params: Map<String, String?> = emptyMap()) =
            bind(params = params)

        operator fun invoke(vararg params: Pair<String, String?>) =
            bind(params = params.toMap())

        fun withAttributes(builder: TypedAttributes.Builder.() -> Unit): Get<RESPONSE> = copy(
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

    data class Post<BODY, RESPONSE>(
        override val uri: String,
        val body: KSerializer<BODY>,
        val response: KSerializer<RESPONSE>,
        override val attributes: TypedAttributes = TypedAttributes.empty,
    ) : TypedApiEndpoint() {
        class Bound<BODY, RESPONSE>(
            val endpoint: Post<BODY, RESPONSE>,
            val params: Map<String, String?>,
            val body: BODY,
        )

        fun bind(params: Map<String, String?> = emptyMap(), body: BODY) =
            Bound(endpoint = this, params = params, body = body)

        operator fun invoke(params: Map<String, String?> = emptyMap(), body: BODY) =
            bind(params = params, body = body)

        operator fun invoke(vararg params: Pair<String, String>, body: BODY) =
            bind(params = params.toMap(), body = body)

        fun withAttributes(builder: TypedAttributes.Builder.() -> Unit): Post<BODY, RESPONSE> = copy(
            attributes = attributes.plus(builder)
        )
    }

    data class Put<BODY, RESPONSE>(
        override val uri: String,
        val body: KSerializer<BODY>,
        val response: KSerializer<RESPONSE>,
        override val attributes: TypedAttributes = TypedAttributes.empty,
    ) : TypedApiEndpoint() {
        class Bound<BODY, RESPONSE>(
            val uri: String,
            val params: Map<String, String?>,
            val body: BODY,
            val bodySerializer: KSerializer<BODY>,
            val responseSerializer: KSerializer<RESPONSE>,
        )

        fun <SUB : BODY> bind(params: Map<String, String?> = emptyMap(), body: SUB, bodySerializer: KSerializer<SUB>) =
            Bound(
                uri = uri,
                params = params,
                body = body,
                bodySerializer = bodySerializer,
                responseSerializer = response
            )

        operator fun invoke(params: Map<String, String?> = emptyMap(), body: BODY) =
            bind(params = params, body = body, bodySerializer = this.body)

        operator fun invoke(vararg params: Pair<String, String>, body: BODY) =
            bind(params = params.toMap(), body = body, bodySerializer = this.body)

        operator fun <SUB : BODY> invoke(
            vararg params: Pair<String, String>,
            body: SUB,
            bodySerializer: KSerializer<SUB>
        ) =
            bind(params = params.toMap(), body = body, bodySerializer = bodySerializer)

        fun withAttributes(builder: TypedAttributes.Builder.() -> Unit): Put<BODY, RESPONSE> = copy(
            attributes = attributes.plus(builder)
        )
    }
}