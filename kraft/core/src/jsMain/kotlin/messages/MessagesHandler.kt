package de.peekandpoke.kraft.messages

import de.peekandpoke.ultra.streams.Stream
import de.peekandpoke.ultra.streams.StreamSource

class MessagesHandler {
    private val _stream = StreamSource<Message<*>?>(null)

    val stream: Stream<Message<*>?> = _stream

    fun send(message: Message<*>) {
        _stream(message)
    }
}
