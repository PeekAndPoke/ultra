package de.peekandpoke.kraft.messages

/**
 * Message interface
 */
interface Message<S> {
    val sender: S
    val isStopped: Boolean
}
