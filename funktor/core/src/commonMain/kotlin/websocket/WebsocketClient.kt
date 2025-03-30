package de.peekandpoke.ktorfx.core.websocket

import de.peekandpoke.ultra.common.model.EmptyObject
import kotlinx.serialization.KSerializer

interface WebsocketClient {

    /**
     * Sends an empty message of the given [type]
     */
    fun send(type: String): WsClientMessage<EmptyObject>? =
        send(type = type, data = EmptyObject(), serializer = EmptyObject.serializer())

    /**
     * Sends an empty message as defined by [sends]
     */
    fun send(sends: WebsocketClientModule.Sends<EmptyObject>): WsClientMessage<EmptyObject>? =
        send(type = sends.type, data = EmptyObject(), serializer = EmptyObject.serializer())

    /**
     * Sends a message as defined by [sends] with the given [data].
     */
    fun <T : Any> send(sends: WebsocketClientModule.Sends<T>, data: T): WsClientMessage<T>? =
        send(type = sends.type, data = data, serializer = sends.serializer)

    /**
     * Sends a message of the given [type] with the given [data].
     */
    fun <T : Any> send(type: String, data: T, serializer: KSerializer<T>): WsClientMessage<T>?
}
