package io.peekandpoke.funktor.core.websocket

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/** A typed message sent from the websocket client to the server. */
@Serializable
data class WsClientMessage<T>(
    val uuid: String? = null,
    val type: String,
    val token: String? = null,
    val data: T,
) {
    companion object {
        fun <T> withUuid(type: String, token: String, data: T) = WsClientMessage(
            uuid = Uuid.random().toString(),
            type = type,
            token = token,
            data = data,
        )
    }

    fun <X> withData(newData: X): WsClientMessage<X> = WsClientMessage(
        uuid = uuid,
        type = type,
        token = token,
        data = newData
    )
}
