package de.peekandpoke.ultra.streams

/**
 * Base class for stream wrappers that map the value type from [WRAPPED] to [RESULT]
 *
 * @param WRAPPED The value type of the wrapped stream
 * @param RESULT  The value type of the resulting stream
 */
open class StreamMapper<WRAPPED, RESULT>(
    /** The wrapped stream */
    private val wrapped: Stream<WRAPPED>,
    /** Maps the value of the [wrapped] stream to the result ([WRAPPED] to [RESULT]) */
    private val mapper: (WRAPPED) -> RESULT,
    /** Calculates the initial value */
    private val initial: () -> RESULT = { mapper(wrapped()) },
) : StreamWrapperBase<WRAPPED, RESULT>(
    wrapped = wrapped
) {
    private var latest: RESULT? = null

    override fun invoke(): RESULT {
        // If we are not subscribed to the wrapped we might miss values.
        // So we need to get the value directly.
        if (!isSubscribedToWrapped()) {
            return mapper(wrapped()).also { latest = it }
        }

        // Otherwise we can
        return latest ?: initial().also { latest = it }
    }

    override fun handleIncoming(value: WRAPPED) {
        mapper(value)?.let {
            latest = it
            publish(it)
        }
    }
}
