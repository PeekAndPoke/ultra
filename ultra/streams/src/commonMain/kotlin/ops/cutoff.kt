package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamHandler
import io.peekandpoke.ultra.streams.Unsubscribe
import io.peekandpoke.ultra.streams.notifyHandlers

/**
 * Cuts off the stream when the [predicate] is true.
 *
 * The last value of the source stream is published.
 */
fun <T> Stream<T>.cutoffWhen(predicate: Stream<Boolean>): Stream<T> {
    return CutoffStream(source = this, predicate = predicate)
}

/**
 * Cuts off the stream when the [predicate] is false.
 *
 * The last value of the source stream is published.
 */
fun <T> Stream<T>.cutoffWhenNot(predicate: Stream<Boolean>): Stream<T> = cutoffWhen(predicate.map { !it })

/**
 * Cuts off the stream when the [predicate] is true.
 *
 * - While cut off, the source is fully unsubscribed.
 * - When the cut-off is lifted, the source is re-subscribed and its current value is published immediately.
 */
private class CutoffStream<T>(
    private val source: Stream<T>,
    private val predicate: Stream<Boolean>,
) : Stream<T> {

    private val subscriptions = mutableSetOf<StreamHandler<T>>()

    private var lastValue: T = source()
    private var isCutOff: Boolean = predicate()

    private var predicateUnsubscribe: Unsubscribe? = null
    private var sourceUnsubscribe: Unsubscribe? = null

    override fun invoke(): T = lastValue

    override fun subscribeToStream(sub: (T) -> Unit): Unsubscribe {
        subscriptions.add(sub)

        if (subscriptions.size == 1) {
            start()
        } else {
            sub(lastValue)
        }

        return {
            subscriptions.remove(sub)
            if (subscriptions.isEmpty()) {
                stop()
            }
        }
    }

    private fun start() {
        var isInitial = true

        predicateUnsubscribe = predicate.subscribeToStream { next ->
            val previous = isCutOff
            isCutOff = next

            if (isInitial) {
                isInitial = false
                if (!isCutOff) {
                    subscribeSource()
                }
                return@subscribeToStream
            }

            if (previous == isCutOff) {
                return@subscribeToStream
            }

            if (isCutOff) {
                unsubscribeSource()
            } else {
                subscribeSource()
            }
        }
    }

    private fun stop() {
        unsubscribeSource()
        predicateUnsubscribe?.invoke()
        predicateUnsubscribe = null
    }

    private fun subscribeSource() {
        if (sourceUnsubscribe != null) return

        sourceUnsubscribe = source.subscribeToStream { next ->
            lastValue = next
            publish(next)
        }
    }

    private fun unsubscribeSource() {
        sourceUnsubscribe?.invoke()
        sourceUnsubscribe = null
    }

    private fun publish(value: T) {
        notifyHandlers(subscriptions, value)
    }
}
