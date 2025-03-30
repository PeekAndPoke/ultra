package de.peekandpoke.ktorfx.core.websocket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class WsServerMessage {

    @Serializable
    @SerialName("ack")
    data class Ack(
        val uuid: String,
        val requestType: String,
        val message: String? = null,
    ) : WsServerMessage()

    @Serializable
    @SerialName("nack")
    data class Nack(
        val uuid: String,
        val requestType: String,
        val message: String? = null,
    ) : WsServerMessage()

    @Serializable
    @SerialName("log")
    data class Log(
        val level: String,
        val message: String,
    ) : WsServerMessage()

    @Serializable
    @SerialName("data")
    data class Data<T>(
        val type: String,
        val data: T,
    ) : WsServerMessage()
}
