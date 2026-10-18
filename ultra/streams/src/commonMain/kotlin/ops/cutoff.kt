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
 * - The initial cut-off state is read from the [predicate]'s current value, not from its first emission.
 */
private class CutoffStream<T>(
    private val source: Stream<T>,
    private val predicate: Stream<Boolean>,
) : Stream<T> {

    private val subscriptions = mutableSetOf<StreamHandler<T>>()

    private var lastValue: T = source()

    /** Placeholder until [start] reads the real state from the predicate */
    private var isCutOff: Boolean = false

    private var predicateUnsubscribe: Unsubscribe? = null
    private var sourceUnsubscribe: Unsubscribe? = null

    /** Set while [start] is establishing the predicate subscription */
    private var starting = false

    /** Counts source subscriptions, so one left over from an outdated state can be detected */
    private var sourceCount = 0

    override fun invoke(): T = lastValue

    override fun subscribeToStream(sub: (T) -> Unit): Unsubscribe {
        // Start before adding the subscriber, so that no subscriber runs while we are wiring up
        if (!starting && predicateUnsubscribe == null) {
            start()
        }

        subscriptions.add(sub)
        sub(lastValue)

        return {
            subscriptions.remove(sub)
            if (subscriptions.isEmpty()) {
                stop()
            }
        }
    }

    private fun start() {
        starting = true

        try {
            var established = false

            predicateUnsubscribe = predicate.subscribeToStream { next ->
                if (established) {
                    val previous = isCutOff
                    isCutOff = next

                    if (previous != isCutOff) {
                        if (isCutOff) {
                            unsubscribeSource()
                        } else {
                            subscribeSource()
                        }
                    }
                }
            }

            established = true

            // The initial predicate value is applied here and not in the handler above: the
            // subscription must be established before the source can publish to a subscriber
            isCutOff = predicate()

            if (!isCutOff) {
                subscribeSource()
            }
        } catch (t: Throwable) {
            stop()
            throw t
        } finally {
            starting = false
        }
    }

    private fun stop() {
        unsubscribeSource()
        predicateUnsubscribe?.invoke()
        predicateUnsubscribe = null
    }

    private fun subscribeSource() {
        if (sourceUnsubscribe != null) return

        val attempt = ++sourceCount

        val unsubscribe = source.subscribeToStream { next ->
            // The cut-off may have taken effect while this handler was already part of a
            // notification snapshot. Values of an outdated subscription must not be published.
            if (attempt == sourceCount) {
                lastValue = next
                publish(next)
            }
        }

        // A subscriber may have cut off or torn down the stream while the subscription above
        // was publishing the source's current value. Then it is already outdated.
        if (attempt != sourceCount || predicateUnsubscribe == null) {
            unsubscribe()
        } else {
            sourceUnsubscribe = unsubscribe
        }
    }

    private fun unsubscribeSource() {
        // Also invalidates a subscription that is still being established
        sourceCount++

        sourceUnsubscribe?.invoke()
        sourceUnsubscribe = null
    }

    private fun publish(value: T) {
        notifyHandlers(subscriptions, value)
    }
}
