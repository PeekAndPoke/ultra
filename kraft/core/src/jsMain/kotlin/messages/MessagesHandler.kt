package io.peekandpoke.kraft.messages

import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource

/**
 * Handles dispatching and subscribing to [Message] events within a component.
 */
class MessagesHandler {
    private val _stream = StreamSource<Message<*>?>(null)

    /** Stream of messages; subscribers receive each message as it is sent. */
    val stream: Stream<Message<*>?> = _stream

    /** Dispatches the given [message] to all subscribers. */
    fun send(message: Message<*>) {
        _stream(message)
    }
}
