package io.peekandpoke.kraft.messages

/**
 * A message that propagates up the component tree from a [sender].
 */
interface Message<S> {
    /** The component or object that originated this message. */
    val sender: S

    /** Whether this message's propagation has been stopped. */
    val isStopped: Boolean
}
