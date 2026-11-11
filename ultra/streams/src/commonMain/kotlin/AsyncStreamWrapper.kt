package io.peekandpoke.ultra.streams

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Base class for stream wrappers that map the value type from [WRAPPED] to [RESULT]
 *
 * @param WRAPPED The value type of the wrapped stream
 * @param RESULT  The value type of the resulting stream
 */
abstract class AsyncStreamWrapper<WRAPPED, RESULT>(
    /** The wrapped stream */
    private val wrapped: Stream<WRAPPED>,
    /** Maps the value of the [wrapped] stream to the result ([WRAPPED] to [RESULT]) */
    private val mapper: suspend (WRAPPED) -> RESULT?,
    /** coroutine launcher */
    private val scope: CoroutineScope,
) : StreamWrapperBase<WRAPPED, RESULT?>(
    wrapped = wrapped
) {
    private var latest: RESULT? = null

    /** Counts the incoming values: only the newest one's result is published (ULTRA-00308). */
    private var incoming = 0

    override fun invoke(): RESULT? = latest

    override fun handleIncoming(value: WRAPPED) {
        val own = ++incoming

        scope.launch {
            val mapped = mapper(value)

            // A newer value came meanwhile: its result, not this older one, is the stream's
            if (own == incoming) {
                latest = mapped
                publish(mapped)
            }
        }
    }
}
