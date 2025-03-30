package de.peekandpoke.kraft.messages

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.ultra.common.TypedKey

/**
 * The key used to store the [MessagesHandler] on the components properties
 */
private val MessagesKey = TypedKey<MessagesHandler>("messages")

/**
 * Get the [MessagesHandler] for the [Component]
 */
val Component<*>.messages: MessagesHandler
    get() = attributes.getOrPut(MessagesKey) {
        MessagesHandler()
    }

/**
 * Listens for a [Message] of the given type [M].
 */
inline fun <reified M : Message<*>> Component<*>.onMessage(noinline handler: (M) -> Unit) {
    messages.stream { next ->
        next?.let {
            (next as? M)?.let {
                handler(it)
            }
        }
    }
}

/**
 * Send the given [message].
 */
fun <M : Message<*>> Component<*>.sendMessage(message: M) {

    // We do not dispatch the message on the component that sent it
    if (message.sender != this) {
        messages.send(message)
    } else {
        // but we will trigger a re-draw
        triggerRedraw()
    }

    // Continue with the parent if the message was not stopped
    if (!message.isStopped) {
        parent?.sendMessage(message)
    }
}
