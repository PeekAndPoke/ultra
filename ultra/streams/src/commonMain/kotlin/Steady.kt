package io.peekandpoke.ultra.streams

/**
 * A [Stream] that always holds the same [value]. It never changes and never notifies.
 */
class Steady<T>(private val value: T) : Stream<T> {

    override operator fun invoke(): T = value

    override fun subscribeToStream(sub: StreamHandler<T>): Unsubscribe {
        sub(value)
        return {}
    }
}

/**
 * Creates a [Stream] that always holds the given [value].
 */
fun <T> steady(value: T): Stream<T> = Steady(value)
