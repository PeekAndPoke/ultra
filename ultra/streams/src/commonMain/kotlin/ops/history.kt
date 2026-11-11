package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSourceImpl
import io.peekandpoke.ultra.streams.Unsubscribe
import io.peekandpoke.ultra.streams.steady

/**
 * Records the latest incoming values up to the given [capacity].
 *
 * Each value copies the list (up to [capacity] entries), so a producer emitting many times a second pays that each
 * time: sample it first, `source.sampled(every = 250.milliseconds).history(480)`, which also gives a time axis.
 */
fun <T> Stream<T>.history(capacity: Int): Stream<List<T>> = history(steady(capacity))

/**
 * Records the latest incoming values up to [capacity], which may change (ULTRA-00271): a smaller capacity drops the
 * oldest values at once, a larger one keeps what there is and fills up from there. A capacity of 0 or less records
 * nothing.
 *
 * Values are recorded only while someone listens, the source's current value first; [capacity] is listened to only
 * then. Each value copies the list: sample a fast producer first.
 */
fun <T> Stream<T>.history(capacity: Stream<Int>): Stream<List<T>> =
    StreamHistoryOperator(source = this, capacity = capacity, accept = { true })

/**
 * Records the latest incoming values that are not null up to the given [capacity].
 *
 * Each value copies the list, as [history] does: sample a fast producer first.
 */
fun <T> Stream<T?>.historyOfNonNull(capacity: Int): Stream<List<T>> = historyOfNonNull(steady(capacity))

/** Records the latest incoming values that are not null up to [capacity], which may change; see [history]. */
fun <T> Stream<T?>.historyOfNonNull(capacity: Stream<Int>): Stream<List<T>> =
    StreamHistoryOperator(source = this, capacity = capacity, accept = { it != null })

/** The history operator; [accept] picks the values to record, which are then of type [T]. */
private class StreamHistoryOperator<IN, T>(
    private val source: Stream<IN>,
    private val capacity: Stream<Int>,
    private val accept: (IN) -> Boolean,
) : StreamSourceImpl<List<T>>(emptyList()) {

    private var listening = false

    /** Set while [onSub] subscribes: the first values are recorded, not published (the subscriber gets the list). */
    private var starting = false

    private var unsubscribeSource: Unsubscribe? = null

    private var unsubscribeCapacity: Unsubscribe? = null

    /** The capacity in force: [capacity]'s latest while someone listens. */
    private var limit = 0

    /** Nobody listening: what was recorded, cut to the capacity as it is now. */
    override fun invoke(): List<T> = if (listening) current else current.takeLast(capacity().coerceAtLeast(0))

    override fun onSub() {
        if (listening) return

        listening = true
        starting = true

        try {
            unsubscribeCapacity = capacity.subscribeToStream { resize(it) }
            unsubscribeSource = source.subscribeToStream { record(it) }
        } catch (e: Throwable) {
            // Half subscribed: undone, so the next subscriber starts afresh
            stopListening()
            throw e
        } finally {
            starting = false
        }
    }

    override fun onUnSub() {
        if (subscriptions.isEmpty()) {
            stopListening()
        }
    }

    private fun stopListening() {
        listening = false
        unsubscribeSource?.invoke()
        unsubscribeSource = null
        unsubscribeCapacity?.invoke()
        unsubscribeCapacity = null
    }

    private fun resize(next: Int) {
        limit = next.coerceAtLeast(0)

        if (current.size > limit) update(current.takeLast(limit))
    }

    private fun record(value: IN) {
        if (!accept(value)) return

        @Suppress("UNCHECKED_CAST")
        update((current + (value as T)).takeLast(limit))
    }

    private fun update(next: List<T>) {
        if (starting) current = next else invoke(next)
    }
}
