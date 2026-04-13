package io.peekandpoke.funktor.core.websocket

import io.peekandpoke.ultra.model.EmptyObject
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

/** Base class for defining the message types a websocket client can send and receive. */
abstract class WebsocketClientModule {

    val receivedTypes = mutableMapOf<String, Receives<*>>()

    val sentTypes = mutableMapOf<String, Sends<*>>()

    /** Descriptor for a message type the client can send. */
    class Sends<T : Any>(val type: String, val serializer: KSerializer<T>, val cls: KClass<T>)

    /** Descriptor for a message type the client can receive. */
    class Receives<T : Any>(val type: String, val serializer: KSerializer<T>, val cls: KClass<T>)

    // Methods for handling incoming messages

    fun sends(type: String): Sends<EmptyObject> = sends(type, EmptyObject.serializer())

    inline fun <reified T : Any> sends(type: String, serializer: KSerializer<T>): Sends<T> =
        Sends(type, serializer, T::class).also {
            sentTypes[it.type] = it
        }

    fun receives(type: String) = receives(type, EmptyObject.serializer())

    inline fun <reified T : Any> receives(type: String, serializer: KSerializer<T>): Receives<T> =
        Receives(type, serializer, T::class).also {
            receivedTypes[it.type] = it
        }
}
