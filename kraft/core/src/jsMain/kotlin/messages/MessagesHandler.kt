package io.peekandpoke.kraft.messages

import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamHandler
import io.peekandpoke.ultra.streams.Unsubscribe

/**
 * Handles dispatching and subscribing to [Message] events within a component.
 *
 * Every message reaches every handler, also one sent by a handler while another message is dispatched (ULTRA-00307):
 * the bus has its own delivery, not a stream source's "latest value wins". The nested message is delivered at once,
 * so `sendMessage` sees `isStopped` right after `send` returns.
 */
class MessagesHandler {

    private val handlers = mutableSetOf<StreamHandler<Message<*>?>>()

    private var last: Message<*>? = null

    /** Stream of messages; subscribers receive each message as it is sent. */
    val stream: Stream<Message<*>?> = object : Stream<Message<*>?> {
        override fun invoke(): Message<*>? = last

        override fun subscribeToStream(sub: StreamHandler<Message<*>?>): Unsubscribe {
            // An entry of this subscription's own: the same handler given twice is two subscriptions
            val entry: StreamHandler<Message<*>?> = { sub(it) }

            handlers.add(entry)
            sub(last)

            return { handlers.remove(entry) }
        }
    }

    /** Dispatches the given [message] to all subscribers. */
    fun send(message: Message<*>) {
        last = message

        for (handler in handlers.toList()) {
            // Unsubscribed during this dispatch: its unsubscribe has returned
            if (handler !in handlers) continue

            try {
                handler(message)
            } catch (t: Throwable) {
                console.error("[Kraft] message handler threw", t)
            }
        }
    }
}
