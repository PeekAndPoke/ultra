package de.peekandpoke.kraft.streams

/**
 * Base class for stream wrappers that keep the value type
 *
 * @param [wrapped] The wrapped stream
 */
abstract class StreamWrapper<T>(private val wrapped: Stream<T>) : StreamWrapperBase<T, T>(
    wrapped = wrapped
) {
    override fun invoke(): T = wrapped()

    override fun handleIncoming(value: T): Unit = publish(value)
}
