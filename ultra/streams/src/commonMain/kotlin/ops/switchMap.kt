package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamHandler
import io.peekandpoke.ultra.streams.Unsubscribe
import io.peekandpoke.ultra.streams.notifyHandlers

/**
 * Switches to the inner stream selected by [selector] from the latest value of this stream.
 *
 * When a value selects a different inner stream, the old one is unsubscribed before the new one is
 * subscribed and its current value is published. An upstream shared by the inner streams is torn
 * down and restarted on every switch - `permanent()` on that upstream prevents this.
 *
 * Inner streams are compared by identity: selecting the same instance again keeps the subscription
 * and publishes nothing, while a [selector] building a new stream per call resubscribes every time.
 */
fun <T, R> Stream<T>.switchMap(selector: (T) -> Stream<R>): Stream<R> {
    return SwitchMapStream(
        outer = this,
        selector = selector,
        convert = { it },
        // Never called: the selector always returns an inner stream
        absent = { error("The switchMap selector returned no inner stream") },
    )
}

/**
 * Switches to the inner stream selected by [selector] from the latest non-null value of this stream.
 *
 * While the value of this stream is null, the resulting stream publishes null instead of keeping the
 * last inner value. Combine with [fallbackTo] for a non-null result.
 *
 * See [switchMap] for how inner streams are compared and switched.
 */
fun <T : Any, R> Stream<T?>.switchMapNotNull(selector: (T) -> Stream<R>): Stream<R?> {
    return SwitchMapStream(
        outer = this,
        selector = { it?.let(selector) },
        convert = { it },
        absent = { null },
    )
}

/**
 * Switch-map operator impl.
 *
 * [INNER] and [RESULT] are distinct type parameters because [Stream] is invariant:
 * [switchMapNotNull] follows inner streams of [INNER] but publishes [RESULT] = `INNER?`.
 */
private class SwitchMapStream<OUTER, INNER, RESULT>(
    /** The outer stream that drives which inner stream is active */
    private val outer: Stream<OUTER>,
    /** Selects the inner stream for an outer value, null meaning there is none */
    private val selector: (OUTER) -> Stream<INNER>?,
    /** Converts inner values to result values */
    private val convert: (INNER) -> RESULT,
    /** Produces the result value while no inner stream is selected */
    private val absent: () -> RESULT,
) : Stream<RESULT> {

    private val subscriptions = mutableSetOf<StreamHandler<RESULT>>()

    /** Only valid while [hasValue] is set, which is exactly while we are subscribed */
    private var lastValue: RESULT? = null
    private var hasValue = false

    /** The currently subscribed inner stream, used for the identity check */
    private var currentInner: Stream<INNER>? = null

    private var outerUnsubscribe: Unsubscribe? = null
    private var innerUnsubscribe: Unsubscribe? = null

    /** Set while [start] is establishing the outer subscription */
    private var starting = false

    /** Counts switches, so a subscription left over from an outdated switch can be detected */
    private var switchCount = 0

    override fun invoke(): RESULT {
        // While we are not subscribed to the outer stream we might have missed values.
        // So we need to compute the value fresh.
        if (!hasValue) {
            return compute()
        }

        @Suppress("UNCHECKED_CAST")
        return lastValue as RESULT
    }

    override fun subscribeToStream(sub: (RESULT) -> Unit): Unsubscribe {
        // Start before adding the subscriber, so that no subscriber runs while we are wiring up
        if (!starting && outerUnsubscribe == null) {
            start()
        }

        subscriptions.add(sub)
        sub(invoke())

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

            outerUnsubscribe = outer.subscribeToStream { next ->
                if (established) {
                    switchTo(inner = selector(next), force = false)
                }
            }

            established = true

            // The initial value is switched to here and not in the handler above: running the
            // selector before the outer subscription is established would orphan it on a throw
            switchTo(inner = selector(outer()), force = true)
        } catch (t: Throwable) {
            stop()
            throw t
        } finally {
            starting = false
        }
    }

    private fun stop() {
        innerUnsubscribe?.invoke()
        innerUnsubscribe = null
        currentInner = null

        outerUnsubscribe?.invoke()
        outerUnsubscribe = null

        hasValue = false
    }

    private fun switchTo(inner: Stream<INNER>?, force: Boolean) {
        // The same inner stream selected again? Then keep the subscription and stay silent.
        if (!force && inner === currentInner) {
            return
        }

        // Release the old inner stream before subscribing to the new one
        innerUnsubscribe?.invoke()
        innerUnsubscribe = null
        currentInner = null

        val switch = ++switchCount

        if (inner == null) {
            publish(absent())
            return
        }

        val unsubscribe = inner.subscribeToStream { next ->
            publish(convert(next))
        }

        // A subscriber may have switched again or unsubscribed while the subscription above was
        // publishing the inner stream's current value. Then this subscription is already outdated.
        if (switch != switchCount || outerUnsubscribe == null) {
            unsubscribe()
        } else {
            currentInner = inner
            innerUnsubscribe = unsubscribe
        }
    }

    private fun compute(): RESULT {
        val inner = selector(outer()) ?: return absent()

        return convert(inner())
    }

    private fun publish(value: RESULT) {
        lastValue = value
        hasValue = true

        notifyHandlers(subscriptions, value)
    }
}
