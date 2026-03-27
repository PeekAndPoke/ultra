package io.peekandpoke.funktor.demo.common.showcase

import kotlinx.serialization.Serializable

@Serializable
data class EndpointInfo(
    val feature: String,
    val group: String,
    val method: String,
    val uri: String,
    val authDescription: String,
)

@Serializable
data class ServerTimeResponse(
    val timestamp: String,
    val epochMs: Long,
)

@Serializable
data class EchoResponse(
    val message: String,
    val echoedAt: String,
)

@Serializable
data class TransformRequest(
    val text: String,
    val operation: String = "uppercase",
)

@Serializable
data class TransformResponse(
    val original: String,
    val transformed: String,
    val operation: String,
)

@Serializable
data class ItemResponse(
    val id: String,
    val name: String,
    val updatedAt: String,
)

@Serializable
data class UpdateItemRequest(
    val name: String,
)
