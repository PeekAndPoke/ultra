package de.peekandpoke.ktorfx.core.websocket

import com.benasher44.uuid.uuid4
import kotlinx.serialization.Serializable

@Serializable
data class WsClientMessage<T>(
    val uuid: String? = null,
    val type: String,
    val token: String? = null,
    val data: T,
) {
    companion object {
        fun <T> withUuid(type: String, token: String, data: T) = WsClientMessage(
            uuid = uuid4().toString(),
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
